package com.suttori.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.suttori.dao.NotificationEntityRepository;
import com.suttori.entity.HistoryEntity;
import com.suttori.entity.MangaChapter;
import com.suttori.entity.MangaDesu.MangaDataDesu;
import com.suttori.entity.MangaDex.Chapter.*;
import com.suttori.entity.MangaDex.Manga.*;
import com.suttori.entity.StatisticEntity;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.MangaDexApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.telegram.UploadMangaDexFeignClient;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegraph.api.objects.Node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
public class MangaDexService implements MangaServiceInterface<MangaDataMangaDex, String> {

    private MangaDexApiFeignClient mangaDexApiFeignClient;
    private UploadMangaDexFeignClient uploadMangaDexFeignClient;
    private TelegramSender telegramSender;
    private Util util;
    private MangaService mangaService;

    private NotificationEntityRepository notificationEntityRepository;

    @Autowired
    public MangaDexService(MangaDexApiFeignClient mangaDexApiFeignClient, UploadMangaDexFeignClient uploadMangaDexFeignClient, TelegramSender telegramSender, Util util, MangaService mangaService, NotificationEntityRepository notificationEntityRepository) {
        this.mangaDexApiFeignClient = mangaDexApiFeignClient;
        this.uploadMangaDexFeignClient = uploadMangaDexFeignClient;
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaService = mangaService;
        this.notificationEntityRepository = notificationEntityRepository;
    }

    @Override
    public void getSearchResult(InlineQuery inlineQuery) {
        try {
            int offset = 30;
            if (!inlineQuery.getOffset().isEmpty()) {
                offset = Integer.parseInt(inlineQuery.getOffset());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Response response = mangaDexApiFeignClient.searchMangaIncludesCoverArt(getSearchParams(inlineQuery, offset));
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaListResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaListResponse.class);

            if (mangaResponse.getResponse() == null) {
                return;
            }
            List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
            int i = 0;
            for (MangaDataMangaDex mangaData : mangaResponse.getData()) {
                MangaAttributes attributes = mangaData.getAttributes();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getYear(attributes.getYear()));
                stringBuilder.append(" | Тип: ").append(mangaData.getType()).append("\n");
                stringBuilder.append("Статус: ").append(attributes.getStatus()).append("\n");
                stringBuilder.append("Жанр: ").append(getGenres(attributes.getTags()));
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(getTitle(attributes.getTitle()))
                        .description(stringBuilder.toString())
                        .thumbnailUrl(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "256"))
                        .inputMessageContent(new InputTextMessageContent("mangaId\n" + mangaData.getId())).build());
            }
            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(inlineQueryResultList)
                    .nextOffset(String.valueOf(offset + 30))
                    .cacheTime(1)
                    .isPersonal(true)
                    .inlineQueryId(inlineQuery.getId()).build());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, List<String>> getSearchParams(InlineQuery inlineQuery, int offset) {
        Map<String, List<String>> searchParams = new HashMap<>();
        if (inlineQuery.getQuery().equals("last updated")) {
            searchParams.put("order[latestUploadedChapter]", List.of("desc"));
        } else {
            searchParams.put("order[rating]", List.of("desc"));
            searchParams.put("title", Collections.singletonList(inlineQuery.getQuery()));
        }
        searchParams.put("limit", List.of("30"));
        searchParams.put("offset", List.of(String.valueOf(offset - 30)));
        searchParams.put("contentRating[]", List.of("safe", "suggestive", "erotica", "pornographic"));
        searchParams.put("availableTranslatedLanguage[]", List.of("en"));
        searchParams.put("hasAvailableChapters", Collections.singletonList("true"));
        return searchParams;
    }

    @Override
    public void sendMangaById(Long userId, String string) {
        String mangaId = util.parseValue(string)[1];
        MangaDataMangaDex mangaData = getMangaData(mangaId);
        telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(getCoverMangaDex(mangaData.getRelationships(), mangaId, "512")))
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(mangaService.getMangaButtons(userId, mangaId))
                .caption(getMangaText(mangaData)).build());
    }

    @Override
    public MangaDataMangaDex getMangaData(String mangaId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = mangaDexApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMangaText(MangaDataMangaDex mangaData) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b>").append(getTitle(mangaData.getAttributes().getTitle())).append("</b>").append("\n\n");
        if (mangaData.getAttributes().getYear() != 0) {
            stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(mangaData.getAttributes().getYear()).append("\n");
        }
        stringBuilder.append("<b>").append("Тип: ").append("</b>").append(mangaData.getType()).append("\n");
        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(mangaData.getAttributes().getStatus()).append("\n");
        stringBuilder.append("<b>").append("Глав: ").append("</b>").append(mangaData.getAttributes().getLatestUploadedChapter()).append("\n");
        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(getGenres(mangaData.getAttributes().getTags())).append("</i>\n\n");
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(getDescription(mangaData.getAttributes().getDescription()));

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {

    }

    @Override
    public void getMangaChaptersButton(CallbackQuery callbackQuery) {
        String mangaId = util.parseValue(callbackQuery.getData())[1];
        int currentPage = Integer.parseInt(util.parseValue(callbackQuery.getData())[2]);
        int chapterPerPage = 10;

        List<Map.Entry<String, String>> volChList = getChapters(mangaId);

        List<InlineKeyboardRow> keyboard = new ArrayList<>();


        byte[] compressedBytes = compressString(volChList.get(0).getValue() + volChList.get(1).getValue() + volChList.get(2).getValue());
        // Преобразование сжатых данных в строку (Base64 для удобства)
        String compressedString = Base64.getEncoder().encodeToString(compressedBytes);
        System.out.println("Сжатая строка: " + compressedString);
        System.out.println("Исходная строка: " + volChList.get(0).getValue() + volChList.get(1).getValue() + volChList.get(2).getValue());

        // Распаковка строки
        String decompressedString = decompressString(Base64.getDecoder().decode(compressedString));
        System.out.println("Распаковка строка: " + decompressedString);

        int gap = 15;
        if (currentPage == 1) {
            gap = 14;
        }
        int lastPage;
        if (volChList.size() % chapterPerPage == 0) {
            lastPage = volChList.size() / chapterPerPage;
        } else {
            lastPage = volChList.size() / chapterPerPage + 1;
        }

        currentPage = getCurrentPage(callbackQuery, currentPage, lastPage, gap);
        if (currentPage == 0) {
            return;
        }

        int startIndex = currentPage * chapterPerPage - chapterPerPage;
        int endIndex = Math.min(startIndex + chapterPerPage, volChList.size());

        InlineKeyboardRow row = new InlineKeyboardRow();
        keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В начало")).callbackData("onFirstPage\n" + mangaId + "\n" + currentPage).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(String.valueOf(currentPage))).callbackData("currentPage\n" + mangaId + "\n" + currentPage).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В конец")).callbackData("onLastPage\n" + mangaId + "\n" + currentPage).build()));

        for (int i = startIndex; i < endIndex; i++) {
            String buttonText = volChList.get(i).getKey();
            if (row.size() == 2) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
            row.add(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(buttonText)).callbackData("chapter\n" + volChList.get(i).getValue()).build());
            if (i == endIndex - 1) {
                keyboard.add(row);
            }
        }

        keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("<<")).callbackData("prevGapPage\n" + mangaId + "\n" + (currentPage - gap)).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("prevPage\n" + mangaId + "\n" + (currentPage - 1)).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData("nextPage\n" + mangaId + "\n" + (currentPage + 1)).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(">>")).callbackData("nextGapPage\n" + mangaId + "\n" + (currentPage + gap)).build()));

        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .chatId(callbackQuery.getFrom().getId())
                .replyMarkup(new InlineKeyboardMarkup(keyboard))
                .messageId(callbackQuery.getMessage().getMessageId()).build());

    }


    public byte[] compressString(String input) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(input.getBytes("UTF-8"));
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decompressString(byte[] compressedData) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<Map.Entry<String, String>> getChapters(String mangaId) {
        try {
            Map<String, List<String>> params = new HashMap<>();
            params.put("translatedLanguage[]", List.of("en"));
            Response response = mangaDexApiFeignClient.getChapterListAggregate(mangaId, params);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ChapterListAggregate chapterListAggregate = new ObjectMapper().readValue(jsonResponse, ChapterListAggregate.class);

            Map<String, String> volChMap = new LinkedHashMap<>();
            for (Volume volume : chapterListAggregate.getVolumes().values()) {
                for (Chapter chapter : volume.getChapters().values()) {
                    volChMap.put("Том " + volume.getVolume() + ". Глава " + chapter.getChapter(), chapter.getId());
                }
            }
            List<Map.Entry<String, String>> volChList = new ArrayList<>(volChMap.entrySet());
            Collections.reverse(volChList);
            return volChList;
        } catch (IOException e) {
            log.error("getChapters ", e);
            throw new RuntimeException();
        }
    }

    @Override
    public int getCurrentPage(CallbackQuery callbackQuery, int currentPage, int lastPage, int gap) {
        String query = util.parseValue(callbackQuery.getData())[0];
        if ((query.contains("onFirstPage") && currentPage == 1) || (currentPage == 0 && query.contains("prevPage")) || (query.contains("prevGapPage") && currentPage == 1 - gap)) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .text("Вы уже находитесть на первой странице")
                    .showAlert(false)
                    .callbackQueryId(callbackQuery.getId()).build());
            return 0;
        } else if ((query.contains("prevGapPage") && currentPage <= 0) || query.contains("onFirstPage")) {
            return 1;
        } else if ((query.contains("onLastPage") && currentPage == lastPage) || (currentPage == lastPage + 1 && query.contains("nextPage")) || (query.contains("nextGapPage") && currentPage == lastPage + gap)) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .text("Вы уже находитесть на последней странице")
                    .showAlert(false)
                    .callbackQueryId(callbackQuery.getId()).build());
            return 0;
        } else if ((query.contains("nextGapPage") && currentPage >= lastPage) || query.contains("onLastPage")) {
            return lastPage;
        }
        return currentPage;
    }

    public List<String> getMangaDataChapters(String chapterId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Response response = mangaDexApiFeignClient.getChapterPageIds(chapterId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);

            ChapterUrlResponse chapterUrlResponse = objectMapper.readValue(jsonResponse, ChapterUrlResponse.class);
            String hash = chapterUrlResponse.getChapter().getHash();
            List<String> urlList = new ArrayList<>();
            for (String pageUrl : chapterUrlResponse.getChapter().getData()) {
                urlList.add("https://uploads.mangadex.org/data/" + hash + "/" + pageUrl);
            }
            return urlList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getChapterFromCallbackHandler(CallbackQuery callbackQuery) {
//        Long userId = callbackQuery.getFrom().getId();
//        String mangaId = util.parseValue(callbackQuery.getData())[1];
//        String chapterId = util.parseValue(callbackQuery.getData())[1];
//        List<String> urlList = getMangaDataChapters(chapterId);
//
//        MangaDataDesu mangaDataDesu = getMangaDataChapters(mangaId, mangaChapterItemsId);
//        if (callbackQuery.getData().contains("nextChapter\n") || callbackQuery.getData().contains("prevChapter\n")) {
//            deleteKeyboard(callbackQuery.getMessage().getMessageId(), userId);
//        }
//        writeHistory(mangaDataDesu, userId);
//        writeStatistic(mangaDataDesu, userId);
//        getChapterHandler(mangaDataDesu, userId);
    }

    @Override
    public void getMangaDataChapters(Long mangaId, Long mangaChapterItemsId) {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//            Response response = mangaDexApiFeignClient.getChapter(mangaId, mangaChapterItemsId);
//            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            com.suttori.entity.MangaDesu.MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, com.suttori.entity.MangaDesu.MangaResponse.class);
//            return mangaResponse.getResponse();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void getChapterHandler(MangaDataDesu mangaDataDesu, Long userId) {
//        MangaChapter copyMessageManga = copyMessageMangaRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), mangaDataDesu.getPages().getCh_curr().getVol(), mangaDataDesu.getPages().getCh_curr().getCh());
//        if (mangaDataDesu.getKind().equals("manga") || mangaDataDesu.getKind().equals("one_shot") || mangaDataDesu.getKind().equals("comics")) {
//            if (copyMessageManga != null && copyMessageManga.getStatus().equals("process")) {
//                waitForUploadManga(userId, copyMessageManga.getId(), mangaDataDesu);
//                executorService.submit(() ->
//                        preloadMangaChapter(userId, mangaDataDesu)
//                );
//                return;
//            }
//            if (copyMessageManga != null && copyMessageManga.getStatus().equals("finished")) {
//                executorService.submit(() ->
//                        sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu)
//                );
//            } else {
//                executorService.submit(() ->
//                        sendTelegraphArticle(userId, mangaDataDesu)
//                );
//            }
//            executorService.submit(() ->
//                    preloadMangaChapter(userId, mangaDataDesu)
//            );
//        } else {
//            if (copyMessageManga != null && copyMessageManga.getStatus().equals("process")) {
//                waitForUploadManhwa(userId, copyMessageManga.getId(), mangaDataDesu);
//                executorService.submit(() ->
//                        preloadManhwaChapter(mangaDataDesu, userId)
//                );
//                return;
//            }
//            if (copyMessageManga != null && copyMessageManga.getStatus().equals("finished")) {
//                executorService.submit(() ->
//                        sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu)
//                );
//            } else {
//                executorService.submit(() ->
//                        sendPDFChapter(userId, mangaDataDesu)
//                );
//            }
//            executorService.submit(() ->
//                    preloadManhwaChapter(mangaDataDesu, userId)
//            );
//        }
    }

    @Override
    public void writeHistory(MangaDataDesu mangaDataDesu, Long userId) {
//        HistoryEntity historyEntity = historyEntityRepository.findByMangaIdAndUserId(mangaDataDesu.getId(), userId);
//        if (historyEntity == null) {
//            historyEntityRepository.save(new HistoryEntity(mangaDataDesu.getId(), userId, mangaDataDesu.getName(), mangaDataDesu.getRussian(), new Timestamp(System.currentTimeMillis())));
//        } else {
//            historyEntity.setUpdateAt(new Timestamp(System.currentTimeMillis()));
//            historyEntityRepository.save(historyEntity);
//        }
    }

    @Override
    public void writeStatistic(MangaDataDesu mangaDataDesu, Long userId) {
        //statisticEntityRepository.save(new StatisticEntity(mangaDataDesu.getId(), userId, mangaDataDesu.getName(), mangaDataDesu.getRussian(), mangaDataDesu.getPages().getCh_curr().getVol(), mangaDataDesu.getPages().getCh_curr().getCh(), new Timestamp(System.currentTimeMillis())));

    }

    @Override
    public void waitForUploadManhwa(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void waitForUploadManga(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void preloadMangaChapter(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void preloadManhwaChapter(MangaDataDesu mangaDataDesu, Long userId) {

    }

    @Override
    public File compressImages(String pdfFileName, MangaDataDesu mangaDataDesu, Long userId, double compressParam) {
        return null;
    }

    @Override
    public void executeBuilder(FFmpeg ffmpeg, FFprobe ffprobe, PdfDocument pdfDoc, Document doc, String fileName, File file, FFmpegBuilder builder, File folder) {

    }

    @Override
    public void sendCopyMessageMangaFromMangaStorage(Integer messageId, Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void sendTelegraphArticle(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void sendPDFChapter(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public File getJpg(File folder, URL imgUrl, String fileName) {
        return null;
    }

    @Override
    public InlineKeyboardMarkup getPrevNextButtons(MangaDataDesu mangaDataDesu) {
        return null;
    }

    @Override
    public Node createImage(String imageUrl) {
        return null;
    }

    @Override
    public Integer sendWaitGIFAndAction(Long userId) {
        return null;
    }

    @Override
    public Integer sendWaitForUploadManhwa(Long userId) {
        return null;
    }

    @Override
    public void deleteKeyboard(Integer messageId, Long userId) {

    }

    @Override
    public void getRandomManga(Long userId) {

    }

    @Override
    public void clickChangeMangaStatus(CallbackQuery callbackQuery) {

    }

    @Override
    public void clickMangaStatus(CallbackQuery callbackQuery) {

    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, Long mangaId) {
        return null;
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatusViaProfile(String read, String planned, String finished, String postponed, Long mangaId, String viaProfile) {
        return null;
    }

    @Override
    public void clickBackManga(CallbackQuery callbackQuery) {

    }


    public String getYear(int year) {
        if (year != 0) {
            return "Год: " + year;
        } else {
            return "";
        }
    }

    private String getTitle(Map<String, String> titleMap) {
        return titleMap.getOrDefault("en", titleMap.getOrDefault("ja", titleMap.values().iterator().next()));
    }

    private String getDescription(Map<String, String> titleMap) {
        return titleMap.getOrDefault("en", titleMap.getOrDefault("ja", titleMap.values().iterator().next()));
    }

    public String getCoverMangaDex(List<MangaRelationship> relationships, String mangaId, String pix) {
        MangaRelationship coverArtRelationship = relationships.stream()
                .filter(relationship -> "cover_art".equals(relationship.getType()))
                .findFirst().get();
        return "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverArtRelationship.getAttributes().getFileName() + "." + pix + ".jpg";

    }

    public String getGenres(List<MangaTag> tags) {
        StringBuilder stringBuilder = new StringBuilder();
        for (MangaTag mangaTag : tags) {
            if (mangaTag.getAttributes().getGroup().equals("genre")) {
                stringBuilder.append(mangaTag.getAttributes().getName().getOrDefault("en", mangaTag.getAttributes().getName().values().iterator().next())).append(" ");
            }
        }
        return stringBuilder.toString();
    }
}
