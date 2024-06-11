package com.suttori.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.suttori.dao.*;
import com.suttori.entity.*;
import com.suttori.entity.MangaDesu.MangaDataDesu;
import com.suttori.entity.MangaDesu.NotificationChapterMapping;
import com.suttori.entity.MangaDesu.NotificationEntity;
import com.suttori.entity.MangaDex.Chapter.*;
import com.suttori.entity.MangaDex.Manga.*;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.MangaDexApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.telegram.TelegraphApiFeignClient;
import com.suttori.telegram.UploadMangaDexFeignClient;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegraph.api.methods.CreatePage;
import org.telegram.telegraph.api.objects.Node;
import org.telegram.telegraph.api.objects.NodeElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MangaDexService implements MangaServiceInterface<MangaDataMangaDex, String> {

    @Value("${telegraphApiToken}")
    private String telegraphApiToken;
    @Value("${telegraphAuthorName}")
    private String telegraphAuthorName;
    @Value("${telegraphAuthorUrl}")
    private String telegraphAuthorUrl;

    private MangaDexApiFeignClient mangaDexApiFeignClient;
    private TelegraphApiFeignClient telegraphApiFeignClient;

    private TelegramSender telegramSender;
    private Util util;

    private NotificationEntityRepository notificationEntityRepository;
    private UserRepository userRepository;
    private MangaRepository mangaRepository;
    private MangaChapterRepository mangaChapterRepository;
    private NotificationChapterMappingRepository notificationChapterMappingRepository;
    private StatisticEntityRepository statisticEntityRepository;
    private HistoryEntityRepository historyEntityRepository;
    private MangaStatusParameterRepository mangaStatusParameterRepository;
    private ReadStatusRepository readStatusRepository;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    public MangaDexService(MangaDexApiFeignClient mangaDexApiFeignClient, TelegramSender telegramSender, Util util, NotificationEntityRepository notificationEntityRepository, UserRepository userRepository, MangaRepository mangaRepository, MangaChapterRepository mangaChapterRepository, NotificationChapterMappingRepository notificationChapterMappingRepository, StatisticEntityRepository statisticEntityRepository, HistoryEntityRepository historyEntityRepository, MangaStatusParameterRepository mangaStatusParameterRepository, ReadStatusRepository readStatusRepository, TelegraphApiFeignClient telegraphApiFeignClient) {
        this.mangaDexApiFeignClient = mangaDexApiFeignClient;
        this.telegramSender = telegramSender;
        this.util = util;
        this.notificationEntityRepository = notificationEntityRepository;
        this.userRepository = userRepository;
        this.mangaRepository = mangaRepository;
        this.mangaChapterRepository = mangaChapterRepository;
        this.notificationChapterMappingRepository = notificationChapterMappingRepository;
        this.statisticEntityRepository = statisticEntityRepository;
        this.historyEntityRepository = historyEntityRepository;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
        this.readStatusRepository = readStatusRepository;
        this.telegraphApiFeignClient = telegraphApiFeignClient;
    }

    @Override
    public void getSearchResult(InlineQuery inlineQuery) {
        try {
            String languageCodeForCatalog = userRepository.findByUserId(inlineQuery.getFrom().getId()).getLanguageCodeForCatalog();
            int offset = !inlineQuery.getOffset().isEmpty() ? Integer.parseInt(inlineQuery.getOffset()) : 30;

            ObjectMapper objectMapper = new ObjectMapper();
            Response response = mangaDexApiFeignClient.searchMangaIncludesCoverArt(getSearchParams(inlineQuery, offset, languageCodeForCatalog));
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
                stringBuilder.append(" | Формат: ").append(getFormat(attributes.getTags(), languageCodeForCatalog)).append("\n");
                stringBuilder.append("Статус: ").append(attributes.getStatus()).append("\n");
                stringBuilder.append("Жанр: ").append(getGenres(attributes.getTags(), languageCodeForCatalog));

                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(getTitle(attributes.getTitle(), languageCodeForCatalog))
                        .description(stringBuilder.toString())
                        .thumbnailUrl(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "256"))
                        .inputMessageContent(new InputTextMessageContent("mangadex.org\nmangaId\n" + mangaData.getId())).build());
            }

            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(inlineQueryResultList)
                    .nextOffset(String.valueOf(offset + 30))
                    .cacheTime(1)
                    .isPersonal(true)
                    .inlineQueryId(inlineQuery.getId()).build());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, List<String>> getSearchParams(InlineQuery inlineQuery, int offset, String languageCodeForCatalog) {
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
        searchParams.put("availableTranslatedLanguage[]", List.of(languageCodeForCatalog));
        searchParams.put("hasAvailableChapters", Collections.singletonList("true"));
        return searchParams;
    }

    @Override
    public void sendMangaById(Long userId, String mangaId) {
        String languageCode = userRepository.findByUserId(userId).getLanguageCodeForCatalog();
        Manga manga = mangaRepository.findByMangaIdAndCatalogNameAndLanguageCode(mangaId, "mangadex.org", languageCode);

        if (manga == null) {
            MangaDataMangaDex mangaData = getMangaData(mangaId);
            manga = saveManga(mangaData, languageCode);
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "512")))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(userId, mangaId, manga.getId(), manga.getLanguageCode()))
                    .caption(getMangaText(manga)).build()));
            mangaRepository.setCoverFileId(coverFileId, mangaId, "mangadex.org");
        } else {
            telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverFileId()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(userId, mangaId, manga.getId(), manga.getLanguageCode()))
                    .caption(getMangaText(manga)).build());
        }
    }

    @Override
    public InlineKeyboardMarkup getMangaButtons(Long userId, String mangaId) {
        return null;
    }

    public Manga saveManga(MangaDataMangaDex mangaData, String languageCode) {
        return mangaRepository.save(new Manga(null, null, mangaData.getId(), "mangadex.org",
                getTitle(mangaData.getAttributes().getTitle(), languageCode), mangaData.getType(), mangaData.getAttributes().getStatus(),
                getGenres(mangaData.getAttributes().getTags(), languageCode), getDescription(mangaData.getAttributes().getDescription(), languageCode),
                mangaData.getAttributes().getYear(), null, 0, getFormat(mangaData.getAttributes().getTags(), languageCode), languageCode));
    }

    public InlineKeyboardMarkup getMangaButtons(Long userId, String mangaId, Long mangaDatabaseId, String languageCode) {
        String whiteCheckMark = notificationEntityRepository.findByMangaIdAndUserId(mangaId, userId) != null ? " :white_check_mark:" : "";
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В закладки")).callbackData("mangadex.org" + "\nchangeStatus\n" + mangaDatabaseId + "\n" + languageCode).build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Уведомления" + whiteCheckMark)).callbackData("mangadex.org" + "\nnotification\n" + mangaDatabaseId + "\n" + languageCode).build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к чтению")).switchInlineQueryCurrentChat("mangadex.org" + "\nmangaId:\n" + mangaId + "\n" + languageCode).build())
        )));
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

    public String getMangaText(Manga manga) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b>").append(manga.getName()).append("</b>").append("\n\n");
        if (manga.getReleaseDate() != 0) {
            stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(manga.getReleaseDate()).append("\n");
        }
        stringBuilder.append("<b>").append("Формат: ").append("</b>").append(manga.getFormat()).append("\n");
        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(manga.getStatus()).append("\n");
        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(manga.getGenres()).append("</i>\n\n");
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(manga.getDescription());

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    @Override
    public String getMangaText(MangaDataMangaDex mangaData) {
        return null;
    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {
        Long mangaDatabaseId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        String languageCode = util.parseValue(callbackQuery.getData())[3];
        String mangaId = mangaRepository.findById(mangaDatabaseId).get().getMangaId();

        Long userId = callbackQuery.getFrom().getId();
        NotificationEntity notificationEntity = notificationEntityRepository.findByMangaIdAndUserId(mangaId, userId);
        if (notificationEntity != null) {
            notificationEntityRepository.delete(notificationEntity);
        } else {
            List<Map.Entry<String, String>> chapters = getChaptersFromSource(mangaId, languageCode);
            if (chapters == null) {
                util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", userId);
                return;
            }
            notificationEntityRepository.save(new NotificationEntity(mangaId, mangaDatabaseId, callbackQuery.getFrom().getId(), "mangadex.org"));
            NotificationChapterMapping chapterMapping = notificationChapterMappingRepository.findByMangaId(mangaId);
            if (chapterMapping == null) {
                notificationChapterMappingRepository.save(new NotificationChapterMapping(mangaId, (chapters.size() + 1) + "", "mangadex.org"));
            } else {
                chapterMapping.setChapter((chapters.size() + 1) + "");
                notificationChapterMappingRepository.save(chapterMapping);
            }
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Теперь ты будешь получать уведомление о выходе новых глав!")
                    .showAlert(true).build());
        }
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getMangaButtons(userId, mangaId, mangaDatabaseId, languageCode))
                .chatId(userId)
                .messageId(callbackQuery.getMessage().getMessageId()).build());
    }

    @Override
    public List<?> getChaptersFromSource(String mangaId) {
        return null;
    }

    @Override
    public void getMangaChaptersButton(InlineQuery inlineQuery) {
        User user = userRepository.findByUserId(inlineQuery.getFrom().getId());
        String mangaId = util.parseValue(inlineQuery.getQuery())[2];
        String languageCode = util.parseValue(inlineQuery.getQuery())[3];
        Manga manga = mangaRepository.findByMangaIdAndCatalogNameAndLanguageCode(mangaId, "mangadex.org", languageCode);
        List<Map.Entry<String, String>> volChList = getChaptersFromSource(mangaId, languageCode);
        if (volChList == null) {
            util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", user.getUserId());
            return;
        }
        List<Chapter> sortedChapters = saveChapters(volChList, manga);

        int offset = 0;
        if (!inlineQuery.getOffset().isEmpty()) {
            offset = Integer.parseInt(inlineQuery.getOffset());
        }
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();

        int limit = offset + 49;
        if (limit > sortedChapters.size()) {
            limit = sortedChapters.size();
        }

        if (user.getSortParam() == null || user.getSortParam().equals("sortDESC")) {
            Collections.reverse(sortedChapters);
        }

        if (offset == 0) {
            if (user.getSortParam() == null || user.getSortParam().equals("sortASC")) {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "0")
                        .title(EmojiParser.parseToUnicode("Отсортировать с конца"))
                        .description("Нажми чтобы отсортировать главы от последней к первой")
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/free-icon-sort-1102420.png")
                        .inputMessageContent(new InputTextMessageContent("sortDESC")).build());
            } else {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "0")
                        .title(EmojiParser.parseToUnicode("Отсортировать с начала"))
                        .description("Нажми чтобы отсортировать главы от первой к последней")
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/free-icon-sort-1102420.png")
                        .inputMessageContent(new InputTextMessageContent("sortASC")).build());
            }
        }

        int i = 1;
        String downloadStatus;
        String readStatus;
        for (int j = offset; j < limit; j++) {
            if (sortedChapters.get(j).getStatus() != null && sortedChapters.get(j).getStatus().equals("finished")) {
                downloadStatus = "✔️ Загружена";
            } else {
                downloadStatus = "Не загружена";
            }

            if (readStatusRepository.findByMangaIdAndChapterIdAndUserIdAndCatalogName(sortedChapters.get(j).getMangaId(), sortedChapters.get(j).getChapterId(), user.getUserId(), "mangadex.org") != null) {
                readStatus = "✔️ Прочитана";
            } else {
                readStatus = "Не прочитана";
            }

            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title("Том " + sortedChapters.get(j).getVol() + ". Глава " + sortedChapters.get(j).getChapter())
                    .description(downloadStatus + "\n" + readStatus)
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                    .inputMessageContent(new InputTextMessageContent(util.getSourceName(inlineQuery.getQuery()) + "\nchapterId\n" + volChList.get(j).getValue())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .nextOffset(String.valueOf(limit))
                .cacheTime(1)
                .isPersonal(true)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public List<Chapter> saveChapters(List<Map.Entry<String, String>> volChList, Manga manga) {
        List<Chapter> chapterList = mangaChapterRepository.findAllByMangaIdAndCatalogNameAndLanguageCode(manga.getMangaId(), "mangadex.org", manga.getLanguageCode());
        Map<String, Chapter> chapterMap = chapterList.stream()
                .collect(Collectors.toMap(Chapter::getChapterId, chapter -> chapter));
        Collections.reverse(volChList);

        if (chapterList.isEmpty()) {
            for (Map.Entry<String, String> stringEntry : volChList) {
                Chapter chapter = getNewChapter(manga, stringEntry);
                chapter.setType(manga.getType());
                chapterList.add(chapter);
            }

            for (int i = 0; i < volChList.size(); i++) {
                try {
                    chapterList.get(i).setPrevChapter(chapterList.get(i - 1));
                } catch (IndexOutOfBoundsException e) {
                    chapterList.get(i).setPrevChapter(null);
                }

                try {
                    chapterList.get(i).setNextChapter(chapterList.get(i + 1));
                } catch (IndexOutOfBoundsException e) {
                    chapterList.get(i).setNextChapter(null);
                }
            }
            mangaChapterRepository.saveAll(chapterList);
        } else {
            for (int i = 0; i < volChList.size(); i++) {
                if (!chapterMap.containsKey(volChList.get(i).getValue())) {
                    Chapter currentChapter = mangaChapterRepository.findByChapterId(volChList.get(i).getValue());
                    if (currentChapter == null) {
                        currentChapter = getNewChapter(manga, volChList.get(i));
                        mangaChapterRepository.save(currentChapter);
                    }

                    if (i != 0 && (currentChapter.getPrevChapter() == null || !currentChapter.getPrevChapter().getChapterId().equals(volChList.get(i - 1).getValue()))) {
                        mangaChapterRepository.setPrevChapter(volChList.get(i - 1).getValue(), currentChapter.getId());
                        try {
                            mangaChapterRepository.setNextChapterByChapterId(currentChapter.getChapterId(), volChList.get(i - 1).getValue());
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Next Chapter not found");
                        }
                    }
                    if (i != volChList.size() - 1 && (currentChapter.getNextChapter() == null || !currentChapter.getNextChapter().getChapterId().equals(volChList.get(i + 1).getValue()))) {
                        Chapter nextChapter = mangaChapterRepository.findByChapterId(volChList.get(i + 1).getValue());
                        if (nextChapter == null) {
                            nextChapter = getNewChapter(manga, volChList.get(i + 1));
                        }
                        nextChapter.setChapterId(volChList.get(i + 1).getValue());
                        try {
                            nextChapter.setNextChapter(mangaChapterRepository.findByChapterId(volChList.get(i + 2).getValue()));
                        } catch (IndexOutOfBoundsException e) {
                            nextChapter.setNextChapter(null);
                        }
                        nextChapter.setPrevChapter(currentChapter);
                        mangaChapterRepository.save(nextChapter);
                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());

                        try {
                            mangaChapterRepository.setPrevChapterByChapterId(nextChapter.getChapterId(), volChList.get(i + 2).getValue());
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Prev Chapter not found");
                        }
                    }
                }
            }
        }
        return mangaChapterRepository.getChaptersInOrder(manga.getMangaId(), "mangadex.org", manga.getLanguageCode());
    }

    public Chapter getNewChapter(Manga manga, Map.Entry<String, String> stringEntry) {
        return new Chapter("mangadex.org", manga.getMangaId(), stringEntry.getValue(),
                manga.getName(), stringEntry.getKey().split("\n")[0], stringEntry.getKey().split("\n")[1],
                new Timestamp(System.currentTimeMillis()), manga.getFormat(), manga.getId(), manga.getLanguageCode());
    }

    public List<Map.Entry<String, String>> getChaptersFromSource(String mangaId, String languageCode) {
        try {
            Map<String, List<String>> params = new HashMap<>();
            params.put("translatedLanguage[]", List.of(languageCode));
            Response response = mangaDexApiFeignClient.getChapterListAggregate(mangaId, params);
            if (response.status() != 200) {
                return null;
            }

            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ChapterListAggregate chapterListAggregate = new ObjectMapper().readValue(jsonResponse, ChapterListAggregate.class);

            Map<String, String> volChMap = new LinkedHashMap<>();
            for (Volume volume : chapterListAggregate.getVolumes().values()) {
                for (ChapterMangaDex chapterMangaDex : volume.getChapters().values()) {
                    volChMap.put(volume.getVolume() + "\n" + chapterMangaDex.getChapter(), chapterMangaDex.getId());
                }
            }

            return new ArrayList<>(volChMap.entrySet());
        } catch (IOException e) {
            log.error("getChapters ", e);
            return null;
        }
    }

    @Override
    public void getChapterFromCallbackHandler(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        Chapter chapter = mangaChapterRepository.findById(Long.valueOf(util.parseValue(callbackQuery.getData())[2])).orElse(null);

        if (chapter == null) {
            util.sendErrorMessage("Глава не найдена, перезапусти бот и попробуй снова. Если не получится, то обратись в поддержку.", userId);
            return;
        }

        if (callbackQuery.getData().contains("nextChapter\n") || callbackQuery.getData().contains("prevChapter\n")) {
            deleteKeyboard(callbackQuery.getMessage().getMessageId(), userId);
        }
        writeHistory(chapter, userId);
        writeStatistic(chapter, userId);
        getChapterHandler(chapter, userId);
    }

    @Override
    public void getChapterFromMessageHandler(Message message) {
        Long userId = message.getFrom().getId();
        String chapterId = util.parseValue(message.getText())[2];
        Chapter chapter = mangaChapterRepository.findByChapterId(chapterId);
        writeHistory(chapter, userId);
        writeStatistic(chapter, userId);
        getChapterHandler(chapter, userId);
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
    public MangaDataMangaDex getMangaDataChapters(String mangaId, String mangaChapterItemsId) {
        return null;
    }

    public void getChapterHandler(Chapter chapter, Long userId) {
        if (chapter.getFormat() != null && !chapter.getFormat().contains("Long Strip")) {
            if (chapter.getStatus() != null && chapter.getStatus().equals("process")) {
                waitForUploadManga(userId, chapter);
                executorService.submit(() ->
                        preloadMangaChapter(userId, chapter)
                );
                return;
            }
            if (chapter.getStatus() != null && chapter.getStatus().equals("finished")) {
                executorService.submit(() ->
                        sendCopyMessageMangaFromMangaStorage(userId, chapter)
                );
            } else {
                executorService.submit(() ->
                        sendTelegraphArticle(userId, chapter)
                );
            }
            executorService.submit(() ->
                    preloadMangaChapter(userId, chapter)
            );
        } else {
            if (chapter.getStatus() != null && chapter.getStatus().equals("process")) {
                waitForUploadManhwa(userId, chapter);
                executorService.submit(() ->
                        preloadManhwaChapter(userId, chapter)
                );
                return;
            }
            if (chapter.getStatus() != null && chapter.getStatus().equals("finished")) {
                executorService.submit(() ->
                        sendCopyMessageMangaFromMangaStorage(userId, chapter)
                );
            } else {
                executorService.submit(() ->
                        sendPDFChapter(userId, chapter)
                );
            }
            executorService.submit(() ->
                    preloadManhwaChapter(userId, chapter)
            );
        }
    }

    @Override
    public void getChapterHandler(MangaDataMangaDex mangaDataMangaDex, Long userId) {

    }

    @Override
    public void writeHistory(MangaDataMangaDex mangaData, Long userId) {

    }

    @Override
    public void writeStatistic(MangaDataMangaDex mangaData, Long userId) {

    }

    public void writeHistory(Chapter chapter, Long userId) {
        HistoryEntity historyEntity = historyEntityRepository.findByMangaIdAndUserId(chapter.getMangaId(), userId);
        if (historyEntity == null) {
            historyEntityRepository.save(new HistoryEntity(chapter.getMangaId(), userId, chapter.getName(), null, new Timestamp(System.currentTimeMillis()), "mangadex.org"));
        } else {
            historyEntity.setUpdateAt(new Timestamp(System.currentTimeMillis()));
            historyEntityRepository.save(historyEntity);
        }
    }


    public void writeStatistic(Chapter chapter, Long userId) {
        statisticEntityRepository.save(new StatisticEntity(chapter.getMangaId(), userId, chapter.getName(), null, chapter.getVol(), chapter.getChapter(), new Timestamp(System.currentTimeMillis()), "mangadex.org"));
    }

    @Override
    public void waitForUploadManhwa(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {

    }

    public void waitForUploadManhwa(Long userId, Chapter chapter) {
        Integer messageIdForDelete = sendWaitGIFAndAction(userId);
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000);
                if (chapter.getStatus().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        chapter = mangaChapterRepository.findById(chapter.getId()).get();
        if (chapter.getStatus().equals("process")) {
            mangaChapterRepository.setStatus(null, chapter.getId());
            sendPDFChapter(userId, chapter);
        } else if (chapter.getStatus().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(userId, chapter);
            telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
        }
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


    public void waitForUploadManga(Long userId, Chapter chapter) {
        for (int i = 0; i < 30; i++) {
            try {
                Thread.sleep(1000);
                chapter = mangaChapterRepository.findById(chapter.getId()).get();
                if (chapter.getStatus().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        chapter = mangaChapterRepository.findById(chapter.getId()).get();
        if (chapter.getStatus().equals("process")) {
            mangaChapterRepository.setStatus(null, chapter.getId());
            sendTelegraphArticle(userId, chapter);
        } else if (chapter.getStatus().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(userId, chapter);
        }
    }


    public void preloadMangaChapter(Long userId, Chapter chapter) {
        try {
            Chapter nextChapter = chapter.getNextChapter();
            if (nextChapter == null || (nextChapter.getStatus() != null && nextChapter.getStatus().equals("finished")) || (nextChapter.getStatus() != null && nextChapter.getStatus().equals("process"))) {
                return;
            }

            List<String> urlList = getMangaDataChapters(chapter.getNextChapter().getChapterId());
            if (urlList.isEmpty()) {
                return;
            }
            List<Node> content = new ArrayList<>();
            for (String url : urlList) {
//                if (mangaPage.getHeight() / 3 >= mangaPage.getWidth()) {
//                    sendPDFChapter(userId, mangaDataDesu);
//                    return;
//                }
                content.add(createImage(url));
            }

            nextChapter.setStatus("process");
            mangaChapterRepository.setStatus("process", nextChapter.getId());


            CreatePage createPage = new CreatePage(telegraphApiToken, nextChapter.getName() + " Vol " + nextChapter.getVol() + ". Chapter " + nextChapter.getChapter(), content)
                    .setAuthorName(telegraphAuthorName)
                    .setAuthorUrl(telegraphAuthorUrl)
                    .setReturnContent(true);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Response response = telegraphApiFeignClient.createPage(createPage);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            PageResponse result = objectMapper.readValue(jsonResponse, PageResponse.class);
            Page page = result.getResult();

            List<MessageEntity> messageEntityList = new ArrayList<>();
            messageEntityList.add(MessageEntity.builder()
                    .type("bold")
                    .length(nextChapter.getName().length())
                    .offset(0).build());

            messageEntityList.add(MessageEntity.builder()
                    .type("text_link")
                    .url(page.getUrl())
                    .length(nextChapter.getName().length())
                    .offset(0).build());

            Integer messageId = telegramSender.send(SendMessage.builder()
                    .text(nextChapter.getName() + "\n" + "Том " + nextChapter.getVol() + ". Глава " + nextChapter.getChapter())
                    .chatId(-1002092468371L)
                    .entities(messageEntityList).build()).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, nextChapter.getId());
                mangaChapterRepository.setStatus("finished", nextChapter.getId());
                mangaChapterRepository.setTelegraphUrl(page.getUrl(), nextChapter.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void preloadManhwaChapter(Long userId, Chapter chapter) {
        try {

            Chapter nextChapter = chapter.getNextChapter();
            if (nextChapter == null || (nextChapter.getStatus() != null && nextChapter.getStatus().equals("finished")) || (nextChapter.getStatus() != null && nextChapter.getStatus().equals("process"))) {
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            List<String> urlList = getMangaDataChapters(nextChapter.getChapterId());

            if (urlList.isEmpty()) {
                return;
            }

            File pdfFolder = util.createStorageFolder("TempPdfStorage");
            String pdfFileName = pdfFolder + File.separator + nextChapter.getName().replace(" ", "_") + "_Vol_" + nextChapter.getVol() + "_Chapter_" + nextChapter.getChapter() + "_From_" + userId + "_" + dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (int i = 0; i < urlList.size(); i++) {
                if (urlList.get(i).endsWith(".webp") || urlList.get(i).endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File webpFile = getJpg(folder, new URL(urlList.get(i)), nextChapter.getName().replace(" ", "_") + "_Vol_" + nextChapter.getVol() + "_Chapter_" + nextChapter.getChapter() + "_Page_" + i + "_From_" + userId);
                    ImageData imgData = ImageDataFactory.create(webpFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    webpFile.delete();
                } else {
                    ImageData imgData = util.downloadImage(urlList.get(i));
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();


            File pdfFile = compressImages(pdfFileName, nextChapter, userId, 0.9, urlList);

            Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                    .document(new InputFile(pdfFile))
                    .caption(nextChapter.getName() + "\n" + "Том " + nextChapter.getVol() + ". Глава " + nextChapter.getChapter())
                    .chatId(-1002092468371L).build()).getMessageId();
            pdfFile.delete();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, nextChapter.getId());
                mangaChapterRepository.setStatus("finished", nextChapter.getId());
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public File compressImages(String pdfFileName, MangaDataDesu mangaDataDesu, Long userId, double compressParam) {
        return null;
    }

    public File compressImages(String pdfFileName, Chapter chapter, Long userId, double compressParam, List<String> urlList) {
        try {
            File pdfFile = new File(pdfFileName);
            if (pdfFile.length() >= 52000000) {
                FFmpeg ffmpeg = new FFmpeg();
                FFprobe ffprobe = new FFprobe();
                pdfFile.delete();
                pdfFile = new File(pdfFileName);
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(pdfFileName));
                Document doc = new Document(pdfDoc);
                for (int i = 0; i < urlList.size(); i++) {
                    String fileName = chapter.getName().replace(" ", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + i + "_From_" + userId;
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFile(folder, new URL(urlList.get(i)), fileName);
                    if (urlList.get(i).endsWith(".webp") || urlList.get(i).endsWith(".WEBP")) {
                        FFmpegStream photoStream = ffprobe.probe(file.getPath()).getStreams().get(0);
                        FFmpegBuilder builder = new FFmpegBuilder()
                                .setInput(file.getPath())
                                .overrideOutputFiles(true)
                                .addOutput(folder + File.separator + fileName + "output.jpeg")
                                .setVideoCodec("mjpeg")
                                .setVideoResolution(photoStream.width, photoStream.height)
                                .setVideoFilter("scale=iw*" + compressParam + ":ih*" + compressParam)
                                .done();
                        executeBuilder(ffmpeg, ffprobe, pdfDoc, doc, fileName, file, builder, folder);
                    } else {
                        FFmpegBuilder builder = new FFmpegBuilder()
                                .setInput(file.getPath())
                                .overrideOutputFiles(true)
                                .addOutput(folder + File.separator + fileName + "output.jpeg")
                                .setFormat("mjpeg")
                                .setVideoFilter("scale=iw*" + compressParam + ":ih*" + compressParam)
                                .done();
                        executeBuilder(ffmpeg, ffprobe, pdfDoc, doc, fileName, file, builder, folder);
                    }
                }
                doc.close();
            } else {
                return pdfFile;
            }
            if (pdfFile.length() >= 52000000 && compressParam >= 0.3) {
                compressImages(pdfFileName, chapter, userId, compressParam - 0.1, urlList);
            }
            return pdfFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeBuilder(FFmpeg ffmpeg, FFprobe ffprobe, PdfDocument pdfDoc, Document doc, String fileName, File file, FFmpegBuilder builder, File folder) {
        try {
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
            ImageData imgData = ImageDataFactory.create(folder + File.separator + fileName + "output.jpeg");
            Image image = new Image(imgData);
            PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
            pdfDoc.addNewPage(pageSize);
            image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
            doc.add(image);
            file.delete();
            new File(folder + File.separator + fileName + "output.jpeg").delete();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendCopyMessageMangaFromMangaStorage(Integer messageId, Long userId, MangaDataDesu mangaDataDesu) {

    }


    public void sendCopyMessageMangaFromMangaStorage(Long userId, Chapter chapter) {
        CopyMessage copyMessage = new CopyMessage(String.valueOf(userId), "-1002092468371L", chapter.getMessageId());
        InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(chapter, userId);
        if (inlineKeyboardMarkup != null) {
            copyMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        try {
            telegramSender.resendCopyMessageFromStorage(copyMessage);
        } catch (ExecutionException | InterruptedException e) {
            mangaChapterRepository.setStatus(null, chapter.getId());
            log.error("Copy message not send: " + chapter.getName() + " chapterId " + chapter.getChapterId() + " vol " + chapter.getVol() + " ch " + chapter.getChapter());
            e.printStackTrace();
            sendTelegraphArticle(userId, chapter);
        }
    }

    @Override
    public void sendTelegraphArticle(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void sendPDFChapter(Long userId, MangaDataDesu mangaDataDesu) {

    }


    public void sendTelegraphArticle(Long userId, Chapter chapter) {
        List<String> urlList = getMangaDataChapters(chapter.getChapterId());
        if (urlList.isEmpty()) {
            util.sendErrorMessage("Данная глава доступна только на странице создателя, к сожалению это можно проверить только при ее загрузке, ссылки на все главы можно найти тут: \nhttps://mangadex.org/title/" + chapter.getMangaId() + "/", userId);
            return;
        }
        try {
            List<Node> content = new ArrayList<>();
            for (String url : urlList) {
//                if (mangaPage.getHeight() / 3 >= mangaPage.getWidth()) {
//                    sendPDFChapter(userId, mangaDataDesu);
//                    return;
//                }
                content.add(createImage(url));
            }

            chapter.setStatus("process");
            mangaChapterRepository.setStatus("process", chapter.getId());


            CreatePage createPage = new CreatePage(telegraphApiToken, chapter.getName() + " Vol " + chapter.getVol() + ". Chapter " + chapter.getChapter(), content)
                    .setAuthorName(telegraphAuthorName)
                    .setAuthorUrl(telegraphAuthorUrl)
                    .setReturnContent(true);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Response response = telegraphApiFeignClient.createPage(createPage);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            PageResponse result = objectMapper.readValue(jsonResponse, PageResponse.class);
            Page page = result.getResult();

            List<MessageEntity> messageEntityList = new ArrayList<>();
            messageEntityList.add(MessageEntity.builder()
                    .type("bold")
                    .length(chapter.getName().length())
                    .offset(0).build());

            messageEntityList.add(MessageEntity.builder()
                    .type("text_link")
                    .url(page.getUrl())
                    .length(chapter.getName().length())
                    .offset(0).build());

            SendMessage sendMessage = new SendMessage("-1002092468371L", chapter.getName() + "\n" + "Том " + chapter.getVol() + ". Глава " + chapter.getChapter());
            sendMessage.setEntities(messageEntityList);

            InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(chapter, userId);


            Integer messageId = telegramSender.send(sendMessage).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, chapter.getId());
                mangaChapterRepository.setStatus("finished", chapter.getId());
                mangaChapterRepository.setTelegraphUrl(page.getUrl(), chapter.getId());


                if (inlineKeyboardMarkup != null) {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .replyMarkup(inlineKeyboardMarkup)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                } else {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPDFChapter(Long userId, Chapter chapter) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            List<String> urlList = getMangaDataChapters(chapter.getChapterId());

            if (urlList.isEmpty()) {
                util.sendErrorMessage("Данная глава доступна только на странице создателя, к сожалению это можно проверить только при ее загрузке, ссылку на главы можно найти тут: \nhttps://mangadex.org/title/" + chapter.getMangaId() + "/", userId);
                return;
            }

            File pdfFolder = util.createStorageFolder("TempPdfStorage");
            Integer messageIdForDelete = sendWaitGIFAndAction(userId);
            String pdfFileName = pdfFolder + File.separator + chapter.getName().replace(" ", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_From_" + userId + "_" + dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (int i = 0; i < urlList.size(); i++) {
                if (urlList.get(i).endsWith(".webp") || urlList.get(i).endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File webpFile = getJpg(folder, new URL(urlList.get(i)), chapter.getName().replace(" ", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + i + "_From_" + userId);
                    ImageData imgData = ImageDataFactory.create(webpFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    webpFile.delete();
                } else {
                    ImageData imgData = util.downloadImage(urlList.get(i));
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();
            File pdfFile = compressImages(pdfFileName, chapter, userId, 0.9, urlList);
            SendDocument sendDocument = new SendDocument("-1002092468371L", new InputFile(pdfFile));
            sendDocument.setCaption(chapter.getName() + "\n" + "Том " + chapter.getVol() + ". Глава " + chapter.getChapter());
            InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(chapter, userId);

            Integer messageId = telegramSender.sendDocument(sendDocument).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, chapter.getId());
                mangaChapterRepository.setStatus("finished", chapter.getId());

                if (inlineKeyboardMarkup != null) {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .replyMarkup(inlineKeyboardMarkup)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                } else {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                }
                pdfFile.delete();
            }

            telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public File getJpg(File folder, URL imgUrl, String fileName) {
        try {
            FFmpeg ffmpeg = new FFmpeg();
            FFprobe ffprobe = new FFprobe();
            File file = util.downloadFile(folder, imgUrl, "temp_img" + fileName + ".webp");
            FFmpegStream photoStream = ffprobe.probe(file.getPath()).getStreams().get(0);
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(file.getPath())
                    .overrideOutputFiles(true)
                    .addOutput(new java.io.File(folder + File.separator + "output_img" + fileName + ".jpeg").getPath())
                    .setVideoCodec("mjpeg")
                    .setVideoResolution(photoStream.width, photoStream.height)
                    .done();
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();
            file.delete();
            return new File(folder + File.separator + "output_img" + fileName + ".jpeg");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InlineKeyboardMarkup getPrevNextButtons(MangaDataDesu mangaDataDesu) {
        return null;
    }


    public InlineKeyboardMarkup getPrevNextButtons(Chapter chapter, Long userId) {
        InlineKeyboardMarkup inlineKeyboardMarkup;

        String readStatus;
        if (readStatusRepository.findByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, "mangadex.org") != null) {
            readStatus = "✅";
        } else {
            readStatus = "\uD83D\uDCD5";
        }


        if (chapter.getPrevChapter() == null && chapter.getNextChapter() == null) {
            inlineKeyboardMarkup = null;
        } else if (chapter.getPrevChapter() == null) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat("mangadex.org" + "\nmangaId:\n" + chapter.getMangaId() + "\n" + chapter.getLanguageCode()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData("mangadex.org" + "\nreadStatus\n" + chapter.getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующая глава")).callbackData("mangadex.org" + "\nnextChapter\n" + chapter.getNextChapter().getId()).build())
            )));
        } else if (chapter.getNextChapter() == null) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat("mangadex.org" + "\nmangaId:\n" + chapter.getMangaId() + "\n" + chapter.getLanguageCode()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущая глава")).callbackData("mangadex.org" + "\nprevChapter\n" + chapter.getPrevChapter().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData("mangadex.org" + "\nreadStatus\n" + chapter.getId()).build())
            )));
        } else {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat("mangadex.org" + "\nmangaId:\n" + chapter.getMangaId() + "\n" + chapter.getLanguageCode()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("mangadex.org" + "\nprevChapter\n" + chapter.getPrevChapter().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData("mangadex.org" + "\nreadStatus\n" + chapter.getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData("mangadex.org" + "\nnextChapter\n" + chapter.getNextChapter().getId()).build())
            )));
        }
        return inlineKeyboardMarkup;
    }

    @Override
    public void clickReadStatus(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        Chapter chapter = mangaChapterRepository.findById(Long.valueOf(util.parseValue(callbackQuery.getData())[2])).orElseThrow();
        ReadStatus readStatus = readStatusRepository.findByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, "mangadex.org");

        if (readStatus == null) {
            readStatusRepository.save(new ReadStatus(chapter.getMangaId(), chapter.getChapterId(), userId, new Timestamp(System.currentTimeMillis()), "mangadex.org"));
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Главу отмечено как \"Прочитано\"")
                    .showAlert(false).build());
        } else {
            readStatusRepository.delete(readStatus);
        }

        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getPrevNextButtons(chapter, userId))
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId).build());
    }

    @Override
    public Node createImage(String imageUrl) {
        NodeElement image = new NodeElement();
        image.setTag("img");
        image.setAttrs(new HashMap<>());
        image.getAttrs().put("src", imageUrl);
        return image;
    }

    @Override
    public Integer sendWaitGIFAndAction(Long userId) {
        return null;
    }

    @Override
    public void deleteKeyboard(Integer messageId, Long userId) {
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>()))
                .messageId(messageId)
                .chatId(userId).build());
    }

    @Override
    public void getRandomManga(Long userId) {

        util.sendInfoMessage("К сожалению, для этого каталога случайная манга недоступна, я как можно скорее постараюсь заменить этот пункт на что-то другое, спасибо за понимание.", userId);


//        try {
//            Map<String, List<String>> searchParams = new HashMap<>();
//            searchParams.put("contentRating[]", List.of("safe", "suggestive", "erotica", "pornographic"));
//            //searchParams.put("availableTranslatedLanguage[]", List.of("en"));
//            //searchParams.put("hasAvailableChapters", Collections.singletonList("true"));
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            Response response = mangaDexApiFeignClient.getRandomManga(searchParams);
//            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
//
//            String mangaId = mangaResponse.getData().getId();
//            sendMangaById(userId, mangaId);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void clickChangeMangaStatus(CallbackQuery callbackQuery) {
        String mangaDatabaseId = util.parseValue(callbackQuery.getData())[2];
        Long userId = callbackQuery.getFrom().getId();
        MangaStatusParameter mangaStatusParameter = mangaStatusParameterRepository.findByMangaDatabaseIdAndUserId(Long.valueOf(mangaDatabaseId), userId);

        String read = "Читаю";
        String planned = "В планах";
        String finished = "Прочитано";
        String postponed = "Отложено";

        if (mangaStatusParameter != null && !mangaStatusParameter.getStatus().isEmpty()) {
            if (mangaStatusParameter.getStatus().equals("read")) {
                read = read + " :white_check_mark:";
            } else if (mangaStatusParameter.getStatus().equals("planned")) {
                planned = planned + " :white_check_mark:";
            } else if (mangaStatusParameter.getStatus().equals("finished")) {
                finished = finished + " :white_check_mark:";
            } else if (mangaStatusParameter.getStatus().equals("postponed")) {
                postponed = postponed + " :white_check_mark:";
            }
        }

        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .replyMarkup(getKeyboardForChangeStatus(read, planned, finished, postponed, mangaDatabaseId)).build());
    }

    @Override
    public void clickMangaStatus(CallbackQuery callbackQuery) {
        String mangaDatabaseId = util.parseValue(callbackQuery.getData())[2];
        Long userId = callbackQuery.getFrom().getId();
        String parameter = util.parseValue(callbackQuery.getData())[3];

        Manga manga = mangaRepository.findById(Long.valueOf(mangaDatabaseId)).orElseThrow();

        String read = "Читаю";
        String planned = "В планах";
        String finished = "Прочитано";
        String postponed = "Отложено";

        MangaStatusParameter mangaStatusParameter = mangaStatusParameterRepository.findByMangaIdAndUserId(manga.getMangaId(), userId);

        if (mangaStatusParameter == null) {
            mangaStatusParameter = new MangaStatusParameter(manga.getMangaId(), Long.valueOf(mangaDatabaseId), userId, parameter, manga.getName(), null, new Timestamp(System.currentTimeMillis()), "mangadex.org");

            switch (parameter) {
                case "read" -> read = read + " :white_check_mark:";
                case "planned" -> planned = planned + " :white_check_mark:";
                case "finished" -> finished = finished + " :white_check_mark:";
                case "postponed" -> postponed = postponed + " :white_check_mark:";
            }
        } else {
            switch (parameter) {
                case "read" -> {
                    if (mangaStatusParameter.getStatus().equals("read")) {
                        mangaStatusParameter.setStatus("none");
                    } else {
                        mangaStatusParameter.setStatus("read");
                        read = read + " :white_check_mark:";
                    }
                }
                case "planned" -> {
                    if (mangaStatusParameter.getStatus().equals("planned")) {
                        mangaStatusParameter.setStatus("none");
                    } else {
                        mangaStatusParameter.setStatus("planned");
                        planned = planned + " :white_check_mark:";
                    }
                }
                case "finished" -> {
                    if (mangaStatusParameter.getStatus().equals("finished")) {
                        mangaStatusParameter.setStatus("none");
                    } else {
                        mangaStatusParameter.setStatus("finished");
                        finished = finished + " :white_check_mark:";
                    }
                }
                case "postponed" -> {
                    if (mangaStatusParameter.getStatus().equals("postponed")) {
                        mangaStatusParameter.setStatus("none");
                    } else {
                        mangaStatusParameter.setStatus("postponed");
                        postponed = postponed + " :white_check_mark:";
                    }
                }
            }
        }

        mangaStatusParameterRepository.save(mangaStatusParameter);
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .replyMarkup(getKeyboardForChangeStatus(read, planned, finished, postponed, mangaDatabaseId)).build());
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, String mangaDatabaseId) {
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(read)).callbackData("mangadex.org" + "\nchangeMangaStatusRead\n" + mangaDatabaseId + "\nread").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(planned)).callbackData("mangadex.org" + "\nchangeMangaStatusPlanned\n" + mangaDatabaseId + "\nplanned").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(finished)).callbackData("mangadex.org" + "\nchangeMangaStatusFinished\n" + mangaDatabaseId + "\nfinished").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(postponed)).callbackData("mangadex.org" + "\nchangeMangaStatusPostponed\n" + mangaDatabaseId + "\npostponed").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("mangadex.org" + "\nclickBackManga\n" + mangaDatabaseId).build())
        )));
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatusViaProfile(String read, String planned, String finished, String postponed, String mangaId, String viaProfile) {
        return null;
    }

    @Override
    public void clickBackManga(CallbackQuery callbackQuery) {
        Long mangaDatabaseId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        Manga manga = mangaRepository.findById(mangaDatabaseId).orElseThrow();
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getMangaButtons(callbackQuery.getFrom().getId(), manga.getMangaId(), mangaDatabaseId, manga.getLanguageCode()))
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public String getYear(int year) {
        if (year != 0) {
            return "Год: " + year;
        } else {
            return "";
        }
    }

    private String getTitle(Map<String, String> titleMap, String languageCodeForCatalog) {
        if (titleMap.isEmpty()) {
            return " ";
        }
        return titleMap.getOrDefault(languageCodeForCatalog, titleMap.getOrDefault("en", titleMap.getOrDefault("ja", titleMap.values().iterator().next())));
    }

    private String getDescription(Map<String, String> titleMap, String languageCodeForCatalog) {
        if (titleMap.isEmpty()) {
            return " ";
        }
        return titleMap.getOrDefault(languageCodeForCatalog, titleMap.getOrDefault("en", titleMap.getOrDefault("ja", titleMap.values().iterator().next())));
    }

    public String getCoverMangaDex(List<MangaRelationship> relationships, String mangaId, String pix) {
        MangaRelationship coverArtRelationship = relationships.stream()
                .filter(relationship -> "cover_art".equals(relationship.getType()))
                .findFirst().get();
        return "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverArtRelationship.getAttributes().getFileName() + "." + pix + ".jpg";

    }

    public String getGenres(List<MangaTag> tags, String languageCode) {
        StringBuilder stringBuilder = new StringBuilder();
        for (MangaTag mangaTag : tags) {
            if (mangaTag.getAttributes().getGroup().equals("genre")) {
                stringBuilder.append(mangaTag.getAttributes().getName().getOrDefault(languageCode, mangaTag.getAttributes().getName().getOrDefault("en", mangaTag.getAttributes().getName().values().iterator().next()))).append(" ");
            }
        }
        return stringBuilder.toString();
    }

    public String getFormat(List<MangaTag> tags, String languageCode) {
        if (tags.isEmpty()) {
            return " ";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (MangaTag mangaTag : tags) {
            if (mangaTag.getAttributes().getGroup().equals("format")) {
                stringBuilder.append(mangaTag.getAttributes().getName().getOrDefault(languageCode, mangaTag.getAttributes().getName().getOrDefault("en", mangaTag.getAttributes().getName().values().iterator().next()))).append(" ");
            }
        }
        return stringBuilder.toString();
    }
}
