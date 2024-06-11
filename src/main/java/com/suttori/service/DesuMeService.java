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
import com.suttori.entity.MangaDesu.*;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.DesuMeApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.telegram.TelegraphApiFeignClient;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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

@Slf4j
@Service
public class DesuMeService implements MangaServiceInterface<MangaDataDesu, Long> {

    @Value("${telegraphApiToken}")
    private String telegraphApiToken;
    @Value("${telegraphAuthorName}")
    private String telegraphAuthorName;
    @Value("${telegraphAuthorUrl}")
    private String telegraphAuthorUrl;

    private DesuMeApiFeignClient desuMeApiFeignClient;
    private TelegramSender telegramSender;
    private Util util;

    private NotificationEntityRepository notificationEntityRepository;
    private UserRepository userRepository;
    private MangaChapterRepository mangaChapterRepository;
    private HistoryEntityRepository historyEntityRepository;
    private StatisticEntityRepository statisticEntityRepository;
    private MangaStatusParameterRepository mangaStatusParameterRepository;
    private NotificationChapterMappingRepository notificationChapterMappingRepository;

    private TelegraphApiFeignClient telegraphApiFeignClient;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    public DesuMeService(DesuMeApiFeignClient desuMeApiFeignClient, TelegramSender telegramSender, Util util, NotificationEntityRepository notificationEntityRepository, UserRepository userRepository, MangaChapterRepository mangaChapterRepository, HistoryEntityRepository historyEntityRepository, StatisticEntityRepository statisticEntityRepository, MangaStatusParameterRepository mangaStatusParameterRepository, NotificationChapterMappingRepository notificationChapterMappingRepository, TelegraphApiFeignClient telegraphApiFeignClient) {
        this.desuMeApiFeignClient = desuMeApiFeignClient;
        this.telegramSender = telegramSender;
        this.util = util;
        this.notificationEntityRepository = notificationEntityRepository;
        this.userRepository = userRepository;
        this.mangaChapterRepository = mangaChapterRepository;
        this.historyEntityRepository = historyEntityRepository;
        this.statisticEntityRepository = statisticEntityRepository;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
        this.notificationChapterMappingRepository = notificationChapterMappingRepository;
        this.telegraphApiFeignClient = telegraphApiFeignClient;
    }

    private static final Map<String, String> TAG_TRANSLATIONS = new HashMap<>();

    static {
        TAG_TRANSLATIONS.put("безумие", "Dementia");
        TAG_TRANSLATIONS.put("боевые_искусства", "Martial_Arts");
        TAG_TRANSLATIONS.put("в_цвете", "Color");
        TAG_TRANSLATIONS.put("вампиры", "Vampire");
        TAG_TRANSLATIONS.put("веб", "Web");
        TAG_TRANSLATIONS.put("гарем", "Harem");
        TAG_TRANSLATIONS.put("героическое_фэнтези", "Heroic_Fantasy");
        TAG_TRANSLATIONS.put("демоны", "Demons");
        TAG_TRANSLATIONS.put("детектив", "Mystery");
        TAG_TRANSLATIONS.put("дзёсей", "Josei");
        TAG_TRANSLATIONS.put("драма", "Drama");
        TAG_TRANSLATIONS.put("ёнкома", "Yonkoma");
        TAG_TRANSLATIONS.put("игры", "Game");
        TAG_TRANSLATIONS.put("исекай", "Isekai");
        TAG_TRANSLATIONS.put("исторический", "Historical");
        TAG_TRANSLATIONS.put("космос", "Space");
        TAG_TRANSLATIONS.put("литRPG", "LitRPG");
        TAG_TRANSLATIONS.put("магия", "Magic");
        TAG_TRANSLATIONS.put("меха", "Mecha");
        TAG_TRANSLATIONS.put("мистика", "Mystic");
        TAG_TRANSLATIONS.put("музыка", "Music");
        TAG_TRANSLATIONS.put("научная фантастика", "Sci-Fi");
        TAG_TRANSLATIONS.put("пародия", "Parody");
        TAG_TRANSLATIONS.put("повседневность", "Slice_of_Life");
        TAG_TRANSLATIONS.put("постапокалиптика", "Post_Apocalyptic");
        TAG_TRANSLATIONS.put("приключения", "Adventure");
        TAG_TRANSLATIONS.put("психологическое", "Psychological");
        TAG_TRANSLATIONS.put("романтика", "Romance");
        TAG_TRANSLATIONS.put("сверхъестественное", "Supernatural");
        TAG_TRANSLATIONS.put("сёдзе", "Shoujo");
        TAG_TRANSLATIONS.put("сёдзе_ай", "Shoujo_Ai");
        TAG_TRANSLATIONS.put("сейнен", "Seinen");
        TAG_TRANSLATIONS.put("сёнен", "Shounen");
        TAG_TRANSLATIONS.put("сёнен_ай", "Shounen_Ai");
        TAG_TRANSLATIONS.put("смена_пола", "Gender_Bender");
        TAG_TRANSLATIONS.put("спорт", "Sports");
        TAG_TRANSLATIONS.put("супер_сила", "Super_Power");
        TAG_TRANSLATIONS.put("трагедия", "Tragedy");
        TAG_TRANSLATIONS.put("триллер", "Thriller");
        TAG_TRANSLATIONS.put("ужасы", "Horror");
        TAG_TRANSLATIONS.put("фантастика", "Fiction");
        TAG_TRANSLATIONS.put("фэнтези", "Fantasy");
        TAG_TRANSLATIONS.put("хентай", "Hentai");
        TAG_TRANSLATIONS.put("школа", "School");
        TAG_TRANSLATIONS.put("экшен", "Action");
        TAG_TRANSLATIONS.put("этти", "Ecchi");
        TAG_TRANSLATIONS.put("юри", "Yuri");
        TAG_TRANSLATIONS.put("яой", "Yaoi");
        TAG_TRANSLATIONS.put("манга", "manga");
        TAG_TRANSLATIONS.put("манхва", "manhwa");
        TAG_TRANSLATIONS.put("манхьхуа", "manhua");
        TAG_TRANSLATIONS.put("ваншот", "one_shot");
        TAG_TRANSLATIONS.put("комикс", "comics");
    }

    @Override
    public void getSearchResult(InlineQuery inlineQuery) {
        try {
            int offset = 30;
            if (!inlineQuery.getOffset().isEmpty()) {
                offset = Integer.parseInt(inlineQuery.getOffset());
            }

            String page = String.valueOf(offset / 30);
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = desuMeApiFeignClient.searchManga(getSearchParams(inlineQuery, page));
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaSearchResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaSearchResponse.class);

            if (mangaResponse.getResponse() == null) {
                return;
            }
            List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
            int i = 0;
            for (MangaDataAsSearchResult mangaData : mangaResponse.getResponse()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Рейтинг: ").append(mangaData.getScore());
                stringBuilder.append(" | Год: ").append(new SimpleDateFormat("yyyy").format(new Date(mangaData.getAired_on() * 1000))).append(" | Тип: ").append(mangaData.getKind()).append("\n");
                stringBuilder.append("Статус: ").append(util.getStatus(mangaData.getStatus())).append("\n");
                stringBuilder.append("Жанр: ").append(mangaData.getGenres());
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(mangaData.getRussian())
                        .description(stringBuilder.toString())
                        .thumbnailUrl(mangaData.getImage().getOriginal())
                        .inputMessageContent(new InputTextMessageContent("desu.me\nmangaId\n" + mangaData.getId())).build());
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


    public Map<String, String> getSearchParams(InlineQuery inlineQuery, String page) {
        Map<String, String> params = new HashMap<>();
        String query = inlineQuery.getQuery();

        if (query.contains("popular")) {
            params.put("order", "popular");
            params.put("page", page);
            params.put("limit", "30");
        } else if (query.contains("last updated")) {
            params.put("order", "updated");
            params.put("page", page);
            params.put("limit", "30");
        } else if (query.contains("hashtag")) {
            params.put("page", page);
            params.put("limit", "30");
            String[] lines = query.split("\n");
            String[] tags = lines[1].split(" ");
            StringBuilder genresBuilder = new StringBuilder();
            StringBuilder kindBuilder = new StringBuilder();
            StringBuilder statusBuilder = new StringBuilder();

            for (String tag : tags) {
                String cleanTag = tag.replace("#", "");
                String translatedTag = TAG_TRANSLATIONS.get(cleanTag.toLowerCase());

                if (translatedTag == null) {
                    continue;
                }

                if (translatedTag.equals("manga") || translatedTag.equals("manhwa") || translatedTag.equals("comics") || translatedTag.equals("manhua") || translatedTag.equals("one_shot")) {
                    appendWithComma(kindBuilder, translatedTag);
                } else if (translatedTag.equals("ongoing") || translatedTag.equals("released") || translatedTag.equals("continued") || translatedTag.equals("completed")) {
                    appendWithComma(statusBuilder, translatedTag);
                } else {
                    appendWithComma(genresBuilder, translatedTag.replace("_", " "));
                }
            }

            if (!genresBuilder.isEmpty()) {
                params.put("genres", genresBuilder.toString());
            }
            if (!kindBuilder.isEmpty()) {
                params.put("kinds", kindBuilder.toString());
            }
            if (!statusBuilder.isEmpty()) {
                params.put("status", statusBuilder.toString());
            }
        } else {
            params.put("search", query);
            params.put("limit", "30");
            params.put("page", page);
        }
        return params;
    }

    private void appendWithComma(StringBuilder builder, String value) {
        if (!builder.isEmpty()) {
            builder.append(",");
        }
        builder.append(value);
    }

    @Override
    public void sendMangaById(Long userId, String mangaId) {
        MangaDataDesu mangaDataDesu = getMangaData(mangaId);
        telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(mangaDataDesu.getImage().getOriginal()))
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(getMangaButtons(userId, mangaId))
                .caption(getMangaText(mangaDataDesu)).build());
    }

    @Override
    public InlineKeyboardMarkup getMangaButtons(Long userId, String mangaId) {
        String whiteCheckMark = "";
        if (notificationEntityRepository.findByMangaIdAndUserId(mangaId, userId) != null) {
            whiteCheckMark = " :white_check_mark:";
        }
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В закладки")).callbackData("desu.me" + "\nchangeStatus\n" + mangaId).build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Уведомления" + whiteCheckMark)).callbackData("desu.me" + "\nnotification\n" + mangaId).build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к чтению")).switchInlineQueryCurrentChat("desu.me" + "\nmangaId:\n" + mangaId).build())
        )));
    }

    @Override
    public MangaDataDesu getMangaData(String mangaId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = desuMeApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {
        try {
            String mangaId = util.parseValue(callbackQuery.getData())[2];
            Long userId = callbackQuery.getFrom().getId();
            NotificationEntity notificationEntity = notificationEntityRepository.findByMangaIdAndUserId(mangaId, userId);
            if (notificationEntity != null) {
                notificationEntityRepository.delete(notificationEntity);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                Response response = desuMeApiFeignClient.getMangaById(mangaId);
                String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
                String lastChapter = mangaResponse.getResponse().getChapters().getLast().getCh();
                notificationEntityRepository.save(new NotificationEntity(mangaId, null, callbackQuery.getFrom().getId(), "desu.me"));
                if (notificationChapterMappingRepository.findByMangaId(mangaId) == null) {
                    notificationChapterMappingRepository.save(new NotificationChapterMapping(String.valueOf(mangaId), lastChapter, "desu.me"));
                }
                telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("Теперь ты будешь получать уведомление о выходе новых глав!")
                        .showAlert(true).build());
            }
            telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                    .replyMarkup(getMangaButtons(userId, mangaId))
                    .chatId(userId)
                    .messageId(callbackQuery.getMessage().getMessageId()).build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMangaText(MangaDataDesu mangaDataDesu) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b>").append(mangaDataDesu.getRussian()).append("</b>").append("\n\n");
        stringBuilder.append("<b>").append("Рейтинг: ").append("</b>").append(mangaDataDesu.getScore()).append("\n");
        stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(new SimpleDateFormat("yyyy").format(new Date(mangaDataDesu.getAired_on() * 1000))).append("\n");
        stringBuilder.append("<b>").append("Тип: ").append("</b>").append(mangaDataDesu.getKind()).append("\n");
        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(util.getStatus(mangaDataDesu.getStatus())).append("\n");
        stringBuilder.append("<b>").append("Глав: ").append("</b>").append(mangaDataDesu.getChapters().getLast().getCh()).append("\n");
        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(util.getGenres(mangaDataDesu.getGenres())).append("</i>\n\n");
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(mangaDataDesu.getDescription());

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    @Override
    public List<MangaChapterItem> getChaptersFromSource(String mangaId) {
        try {
            Response response = desuMeApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = new ObjectMapper().readValue(jsonResponse, MangaResponse.class);
            MangaDataDesu mangaDataDesu = mangaResponse.getResponse();
            return mangaDataDesu.getChapters().getList();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void getMangaChaptersButton(InlineQuery inlineQuery) {
        User user = userRepository.findByUserId(inlineQuery.getFrom().getId());
        String mangaId = util.parseValue(inlineQuery.getQuery())[2];
        List<MangaChapterItem> mangaChapterItems = getChaptersFromSource(mangaId);

        int offset = 0;
        if (!inlineQuery.getOffset().isEmpty()) {
            offset = Integer.parseInt(inlineQuery.getOffset());
        }
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();

        int limit = offset + 49;
        if (limit > mangaChapterItems.size()) {
            limit = mangaChapterItems.size();
        }

        if (user.getSortParam() == null || user.getSortParam().equals("sortASC")) {
            Collections.reverse(mangaChapterItems);
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
        for (int j = offset; j < limit; j++) {
            String string = "Том: " + mangaChapterItems.get(j).getVol() +
                    " Глава: " + mangaChapterItems.get(j).getCh();
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(string)
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                    .description(mangaChapterItems.get(j).getTitle())
                    .inputMessageContent(new InputTextMessageContent(util.getSourceName(inlineQuery.getQuery()) + "\nmangaId\n" + mangaId + "\nchapterId\n" + mangaChapterItems.get(j).getId())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .nextOffset(String.valueOf(limit))
                .cacheTime(1)
                .isPersonal(true)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    @Override
    public MangaDataDesu getMangaDataChapters(String mangaId, String mangaChapterItemsId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Response response = desuMeApiFeignClient.getChapter(mangaId, mangaChapterItemsId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getChapterFromCallbackHandler(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        String mangaId = util.parseValue(callbackQuery.getData())[2];
        String mangaChapterItemsId = util.parseValue(callbackQuery.getData())[3];
        MangaDataDesu mangaDataDesu = getMangaDataChapters(mangaId, mangaChapterItemsId);
        if (callbackQuery.getData().contains("nextChapter\n") || callbackQuery.getData().contains("prevChapter\n")) {
            deleteKeyboard(callbackQuery.getMessage().getMessageId(), userId);
        }
        writeHistory(mangaDataDesu, userId);
        writeStatistic(mangaDataDesu, userId);
        getChapterHandler(mangaDataDesu, userId);
    }

    @Override
    public void getChapterFromMessageHandler(Message message) {
        Long userId = message.getFrom().getId();
        String mangaId = util.parseValue(message.getText())[2];
        String mangaChapterItemsId = util.parseValue(message.getText())[4];
        MangaDataDesu mangaDataDesu = getMangaDataChapters(mangaId, mangaChapterItemsId);
        writeHistory(mangaDataDesu, userId);
        writeStatistic(mangaDataDesu, userId);
        getChapterHandler(mangaDataDesu, userId);
    }

    @Override
    public void getChapterHandler(MangaDataDesu mangaDataDesu, Long userId) {
        Chapter copyMessageManga = mangaChapterRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), String.valueOf(mangaDataDesu.getPages().getCh_curr().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_curr().getCh()));
        if (mangaDataDesu.getKind().equals("manga") || mangaDataDesu.getKind().equals("one_shot") || mangaDataDesu.getKind().equals("comics")) {
            if (copyMessageManga != null && copyMessageManga.getStatus().equals("process")) {
                waitForUploadManga(userId, copyMessageManga.getId(), mangaDataDesu);
                executorService.submit(() ->
                        preloadMangaChapter(userId, mangaDataDesu)
                );
                return;
            }
            if (copyMessageManga != null && copyMessageManga.getStatus().equals("finished")) {
                executorService.submit(() ->
                        sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu)
                );
            } else {
                executorService.submit(() ->
                        sendTelegraphArticle(userId, mangaDataDesu)
                );
            }
            executorService.submit(() ->
                    preloadMangaChapter(userId, mangaDataDesu)
            );
        } else {
            if (copyMessageManga != null && copyMessageManga.getStatus().equals("process")) {
                waitForUploadManhwa(userId, copyMessageManga.getId(), mangaDataDesu);
                executorService.submit(() ->
                        preloadManhwaChapter(mangaDataDesu, userId)
                );
                return;
            }
            if (copyMessageManga != null && copyMessageManga.getStatus().equals("finished")) {
                executorService.submit(() ->
                        sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu)
                );
            } else {
                executorService.submit(() ->
                        sendPDFChapter(userId, mangaDataDesu)
                );
            }
            executorService.submit(() ->
                    preloadManhwaChapter(mangaDataDesu, userId)
            );
        }
    }

    @Override
    public void writeHistory(MangaDataDesu mangaDataDesu, Long userId) {
        HistoryEntity historyEntity = historyEntityRepository.findByMangaIdAndUserId(String.valueOf(mangaDataDesu.getId()), userId);
        if (historyEntity == null) {
            historyEntityRepository.save(new HistoryEntity(String.valueOf(mangaDataDesu.getId()), userId, mangaDataDesu.getName(), mangaDataDesu.getRussian(), new Timestamp(System.currentTimeMillis()), "desu.me"));
        } else {
            historyEntity.setUpdateAt(new Timestamp(System.currentTimeMillis()));
            historyEntityRepository.save(historyEntity);
        }
    }

    @Override
    public void writeStatistic(MangaDataDesu mangaDataDesu, Long userId) {
        statisticEntityRepository.save(new StatisticEntity(String.valueOf(mangaDataDesu.getId()), userId, mangaDataDesu.getName(), mangaDataDesu.getRussian(), String.valueOf(mangaDataDesu.getPages().getCh_curr().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_curr().getCh()), new Timestamp(System.currentTimeMillis()), "desu.me"));
    }

    @Override
    public void waitForUploadManhwa(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {
        Integer messageIdForDelete = sendWaitGIFAndAction(userId);
        Chapter copyMessageManga;
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000);
                copyMessageManga = mangaChapterRepository.findById(copyMessageMangaId).get();
                if (copyMessageManga.getStatus().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        copyMessageManga = mangaChapterRepository.findById(copyMessageMangaId).get();
        if (copyMessageManga.getStatus().equals("process")) {
            mangaChapterRepository.delete(copyMessageManga);
            sendPDFChapter(userId, mangaDataDesu);
        } else if (copyMessageManga.getStatus().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu);
            telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
        }
    }

    @Override
    public void waitForUploadManga(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {
        Chapter copyMessageManga;
        for (int i = 0; i < 30; i++) {
            try {
                Thread.sleep(1000);
                copyMessageManga = mangaChapterRepository.findById(copyMessageMangaId).get();
                if (copyMessageManga.getStatus().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        copyMessageManga = mangaChapterRepository.findById(copyMessageMangaId).get();
        if (copyMessageManga.getStatus().equals("process")) {
            mangaChapterRepository.delete(copyMessageManga);
            sendTelegraphArticle(userId, mangaDataDesu);
        } else if (copyMessageManga.getStatus().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu);
        }
    }

    @Override
    public void preloadMangaChapter(Long userId, MangaDataDesu mangaDataDesu) {
        try {
            Chapter copyMessageManga = mangaChapterRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), String.valueOf(mangaDataDesu.getPages().getCh_next().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_next().getCh()));
            if (copyMessageManga != null) {
                return;
            }

            List<Node> content = new ArrayList<>();
            for (MangaPage mangaPage : getMangaDataChapters(String.valueOf(mangaDataDesu.getId()), String.valueOf(mangaDataDesu.getPages().getCh_next().getId())).getPages().getList()) {
                if (mangaPage.getHeight() / 3 >= mangaPage.getWidth()) {
                    preloadManhwaChapter(mangaDataDesu, userId);
                    return;
                }
                Node image = createImage(mangaPage.getImg().replace("desu.me", "desu.win"));
                content.add(image);
            }
            copyMessageManga = mangaChapterRepository.save(new Chapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), String.valueOf(mangaDataDesu.getPages().getCh_next().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_next().getCh()), "process"));

            CreatePage createPage = new CreatePage(telegraphApiToken, mangaDataDesu.getName() + " Vol " + mangaDataDesu.getPages().getCh_next().getVol() + ". ChapterMangaDex " + mangaDataDesu.getPages().getCh_next().getCh(), content)
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
                    .length(mangaDataDesu.getRussian().length())
                    .offset(0).build());

            messageEntityList.add(MessageEntity.builder()
                    .type("text_link")
                    .url(page.getUrl())
                    .length(mangaDataDesu.getRussian().length())
                    .offset(0).build());

            Integer messageId = telegramSender.send(SendMessage.builder()
                    .text(mangaDataDesu.getRussian() + "\n" + "Том " + mangaDataDesu.getPages().getCh_next().getVol() + ". Глава " + mangaDataDesu.getPages().getCh_next().getCh())
                    .chatId(-1002092468371L)
                    .entities(messageEntityList).build()).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, copyMessageManga.getId());
                mangaChapterRepository.setStatus("finished", copyMessageManga.getId());
                mangaChapterRepository.setTelegraphUrl(page.getUrl(), copyMessageManga.getId());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preloadManhwaChapter(MangaDataDesu mangaDataDesu, Long userId) {
        try {
            Chapter copyMessageManga = mangaChapterRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), String.valueOf(mangaDataDesu.getPages().getCh_next().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_next().getCh()));
            if (copyMessageManga != null) {
                return;
            }

            copyMessageManga = mangaChapterRepository.save(new Chapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), String.valueOf(mangaDataDesu.getPages().getCh_next().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_next().getCh()), "process"));
            File pdfFolder = util.createStorageFolder("TemPdfStorage");
            String pdfFileName = pdfFolder + File.separator + mangaDataDesu.getName().replace(" ", "_") + "_Vol_" + mangaDataDesu.getPages().getCh_next().getVol() + "_Chapter_" + mangaDataDesu.getPages().getCh_next().getCh() + "_From_" + userId + "_timeStamp_" + System.currentTimeMillis() + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (MangaPage page : getMangaDataChapters(String.valueOf(mangaDataDesu.getId()), String.valueOf(mangaDataDesu.getPages().getCh_next().getId())).getPages().getList()) {
                if (page.getImg().endsWith(".webp") || page.getImg().endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File webpFile = getJpg(folder, new URL(page.getImg().replace("desu.me", "desu.win")), "Manga_" + mangaDataDesu.getId() + "_Vol_" + mangaDataDesu.getPages().getCh_next().getVol() + "_Chapter_" + mangaDataDesu.getPages().getCh_next().getCh() + "_Page_" + page.getPage() + "_From_" + userId);
                    ImageData imgData = ImageDataFactory.create(webpFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    webpFile.delete();
                } else {
                    ImageData imgData = util.downloadImageWithReferer(page.getImg().replace("desu.me", "desu.win"));
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();


            File pdfFile = compressImages(pdfFileName, getMangaDataChapters(String.valueOf(mangaDataDesu.getId()), String.valueOf(mangaDataDesu.getPages().getCh_next().getId())), userId, 0.9);

            Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                    .document(new InputFile(pdfFile))
                    .caption(mangaDataDesu.getRussian() + "\n" + "Том " + mangaDataDesu.getPages().getCh_next().getVol() + ". Глава " + mangaDataDesu.getPages().getCh_next().getCh())
                    .chatId(-1002092468371L).build()).getMessageId();
            pdfFile.delete();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, copyMessageManga.getId());
                mangaChapterRepository.setStatus("finished", copyMessageManga.getId());
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File compressImages(String pdfFileName, MangaDataDesu mangaDataDesu, Long userId, double compressParam) {
        try {
            File pdfFile = new File(pdfFileName);
            if (pdfFile.length() >= 52000000) {
                FFmpeg ffmpeg = new FFmpeg();
                FFprobe ffprobe = new FFprobe();
                pdfFile.delete();
                pdfFile = new File(pdfFileName);
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(pdfFileName));
                Document doc = new Document(pdfDoc);
                for (MangaPage page : mangaDataDesu.getPages().getList()) {
                    String fileName = "Manga_" + mangaDataDesu.getId() + "_Vol_" + mangaDataDesu.getPages().getCh_curr().getVol() + "_Chapter_" + mangaDataDesu.getPages().getCh_curr().getCh() + "_Page_" + page.getPage() + "_From_" + userId;
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFile(folder, new URL(page.getImg()), fileName);
                    if (page.getImg().endsWith(".webp") || page.getImg().endsWith(".WEBP")) {
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
                compressImages(pdfFileName, mangaDataDesu, userId, compressParam - 0.1);
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
        CopyMessage copyMessage = new CopyMessage(String.valueOf(userId), "-1002092468371L", messageId);
        InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(mangaDataDesu);
        if (inlineKeyboardMarkup != null) {
            copyMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        try {
            telegramSender.resendCopyMessageFromStorage(copyMessage);
        } catch (ExecutionException | InterruptedException e) {
            mangaChapterRepository.deleteByMessageId(messageId);
            getChapterHandler(mangaDataDesu, userId);
            log.error("Copy message not found: " + mangaDataDesu.getRussian() + " messageId" + messageId);
        }
    }

    @Override
    public void sendTelegraphArticle(Long userId, MangaDataDesu mangaDataDesu) {
        try {
            List<Node> content = new ArrayList<>();
            for (MangaPage mangaPage : mangaDataDesu.getPages().getList()) {
                if (mangaPage.getHeight() / 3 >= mangaPage.getWidth()) {
                    sendPDFChapter(userId, mangaDataDesu);
                    return;
                }
                Node image = createImage(mangaPage.getImg().replace("desu.me", "desu.win"));
                content.add(image);
            }

            Chapter copyMessageManga = mangaChapterRepository.save(new Chapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), String.valueOf(mangaDataDesu.getPages().getCh_curr().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_curr().getCh()), "process"));

            CreatePage createPage = new CreatePage(telegraphApiToken, mangaDataDesu.getName() + " Vol " + mangaDataDesu.getPages().getCh_curr().getVol() + ". ChapterMangaDex " + mangaDataDesu.getPages().getCh_curr().getCh(), content)
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
                    .length(mangaDataDesu.getRussian().length())
                    .offset(0).build());

            messageEntityList.add(MessageEntity.builder()
                    .type("text_link")
                    .url(page.getUrl())
                    .length(mangaDataDesu.getRussian().length())
                    .offset(0).build());

            SendMessage sendMessage = new SendMessage("-1002092468371L", mangaDataDesu.getRussian() + "\n" + "Том " + mangaDataDesu.getPages().getCh_curr().getVol() + ". Глава " + mangaDataDesu.getPages().getCh_curr().getCh());
            sendMessage.setEntities(messageEntityList);

            InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(mangaDataDesu);


            Integer messageId = telegramSender.send(sendMessage).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, copyMessageManga.getId());
                mangaChapterRepository.setStatus("finished", copyMessageManga.getId());
                mangaChapterRepository.setTelegraphUrl(page.getUrl(), copyMessageManga.getId());


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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendPDFChapter(Long userId, MangaDataDesu mangaDataDesu) {
        try {
            Chapter copyMessageManga = mangaChapterRepository.save(new Chapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), String.valueOf(mangaDataDesu.getPages().getCh_curr().getVol()), String.valueOf(mangaDataDesu.getPages().getCh_curr().getCh()), "process"));
            File pdfFolder = util.createStorageFolder("TempPdfStorage");
            Integer messageIdForDelete = sendWaitGIFAndAction(userId);
            String pdfFileName = pdfFolder + File.separator + mangaDataDesu.getName().replace(" ", "_") + "_Vol_" + mangaDataDesu.getPages().getCh_curr().getVol() + "_Chapter_" + mangaDataDesu.getPages().getCh_curr().getCh() + "_From_" + userId + "_timeStamp_" + System.currentTimeMillis() + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (MangaPage page : mangaDataDesu.getPages().getList()) {
                if (page.getImg().endsWith(".webp") || page.getImg().endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File webpFile = getJpg(folder, new URL(page.getImg().replace("desu.me", "desu.win")), "Manga_" + mangaDataDesu.getId() + "_Vol_" + mangaDataDesu.getPages().getCh_curr().getVol() + "_Chapter_" + mangaDataDesu.getPages().getCh_curr().getCh() + "_Page_" + page.getPage() + "_From_" + userId);
                    ImageData imgData = ImageDataFactory.create(webpFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    webpFile.delete();
                } else {
                    ImageData imgData = util.downloadImageWithReferer(page.getImg().replace("desu.me", "desu.win"));
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();
            File pdfFile = compressImages(pdfFileName, mangaDataDesu, userId, 0.9);

            SendDocument sendDocument = new SendDocument("-1002092468371L", new InputFile(pdfFile));
            sendDocument.setCaption(mangaDataDesu.getRussian() + "\n" + "Том " + mangaDataDesu.getPages().getCh_curr().getVol() + ". Глава " + mangaDataDesu.getPages().getCh_curr().getCh());

            InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(mangaDataDesu);


            Integer messageId = telegramSender.sendDocument(sendDocument).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setMessageId(messageId, copyMessageManga.getId());
                mangaChapterRepository.setStatus("finished", copyMessageManga.getId());

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
        InlineKeyboardMarkup inlineKeyboardMarkup;
        if (mangaDataDesu.getPages().getCh_prev().getId() == -1) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat("desu.me" + "\nmangaId:\n" + mangaDataDesu.getId()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("✅")).callbackData(" & ").build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующая глава")).callbackData("desu.me" + "\nnextChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_next().getId()).build())
            )));
        } else if (mangaDataDesu.getPages().getCh_next().getId() == -1) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat("desu.me" + "\nmangaId:\n" + mangaDataDesu.getId()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущая глава")).callbackData("desu.me" + "\nprevChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_prev().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("✅")).callbackData(" & ").build())
            )));
        } else if (mangaDataDesu.getPages().getCh_prev().getId() == -1 && mangaDataDesu.getPages().getCh_next().getId() == -1) {
            inlineKeyboardMarkup = null;
        } else {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat("desu.me" + "\nmangaId:\n" + mangaDataDesu.getId()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("desu.me" + "\nprevChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_prev().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("✅")).callbackData(" & ").build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData("desu.me" + "\nnextChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_next().getId()).build())
            )));
        }
        return inlineKeyboardMarkup;
    }


    @Override
    public void clickReadStatus(CallbackQuery callbackQuery) {
//        Long userId = callbackQuery.getFrom().getId();
//        Chapter chapter = mangaChapterRepository.findById(Long.valueOf(util.parseValue(callbackQuery.getData())[2])).orElseThrow();
//        ReadStatus readStatus = readStatusRepository.findByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, "mangadex.org");
//
//        if (readStatus == null) {
//            readStatusRepository.save(new ReadStatus(chapter.getMangaId(), chapter.getChapterId(), userId, new Timestamp(System.currentTimeMillis()), "mangadex.org"));
//            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
//                    .callbackQueryId(callbackQuery.getId())
//                    .text("Главу отмечено как \"Прочитано\"")
//                    .showAlert(true).build());
//        } else {
//            readStatusRepository.delete(readStatus);
//        }
//
//        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
//                .replyMarkup(getPrevNextButtons(chapter, userId))
//                .messageId(callbackQuery.getMessage().getMessageId())
//                .chatId(userId).build());
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
        Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                .chatId(userId)
                .caption("Твоя глава уже загружается, обычно это занимает не больше минуты, спасибо за ожидание\n\nВ боте предусмотрена автоматическая предзагрузка глав, поэтому пока ты будешь читать текующую главу, следующая уже будет загружена")
                .document(new InputFile("CgACAgQAAxkBAAICV2XKAyJ_d0xIoK5tTXiI14xVYCB5AAKJCwACye1AUZtzbClFKHTFNAQ")).build()).getMessageId();
        telegramSender.sendChatAction(userId, "upload_document");
        return messageId;
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
        try {
            Map<String, String> params = new HashMap<>();
            params.put("order", "updated");
            params.put("limit", "1");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Response response = desuMeApiFeignClient.searchManga(params);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaSearchResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaSearchResponse.class);
            MangaDataAsSearchResult mangaData = mangaResponse.getResponse().get(0);
            Random random = new Random();
            sendMangaById(userId, String.valueOf(random.nextLong(mangaData.getId() + 1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clickChangeMangaStatus(CallbackQuery callbackQuery) {
        String mangaId = util.parseValue(callbackQuery.getData())[2];
        Long userId = callbackQuery.getFrom().getId();
        MangaStatusParameter mangaStatusParameter = mangaStatusParameterRepository.findByMangaIdAndUserId(mangaId, userId);

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
                .replyMarkup(getKeyboardForChangeStatus(read, planned, finished, postponed, mangaId)).build());
    }

    @Override
    public void clickMangaStatus(CallbackQuery callbackQuery) {
        String mangaId = util.parseValue(callbackQuery.getData())[2];
        Long userId = callbackQuery.getFrom().getId();
        String parameter = util.parseValue(callbackQuery.getData())[3];

        MangaDataDesu mangaDataDesu = getMangaData(mangaId);

        String read = "Читаю";
        String planned = "В планах";
        String finished = "Прочитано";
        String postponed = "Отложено";

        MangaStatusParameter mangaStatusParameter = mangaStatusParameterRepository.findByMangaIdAndUserId(mangaId, userId);

        if (mangaStatusParameter == null) {
            mangaStatusParameter = new MangaStatusParameter(mangaId, null, userId, parameter, mangaDataDesu.getRussian(), mangaDataDesu.getName(), new Timestamp(System.currentTimeMillis()), "desu.me");

            if (parameter.equals("read")) {
                read = read + " :white_check_mark:";
            } else if (parameter.equals("planned")) {
                planned = planned + " :white_check_mark:";
            } else if (parameter.equals("finished")) {
                finished = finished + " :white_check_mark:";
            } else if (parameter.equals("postponed")) {
                postponed = postponed + " :white_check_mark:";
            }
        } else {
            if (parameter.equals("read")) {
                if (mangaStatusParameter.getStatus().equals("read")) {
                    mangaStatusParameter.setStatus("none");
                } else {
                    mangaStatusParameter.setStatus("read");
                    read = read + " :white_check_mark:";
                }
            } else if (parameter.equals("planned")) {
                if (mangaStatusParameter.getStatus().equals("planned")) {
                    mangaStatusParameter.setStatus("none");
                } else {
                    mangaStatusParameter.setStatus("planned");
                    planned = planned + " :white_check_mark:";
                }
            } else if (parameter.equals("finished")) {
                if (mangaStatusParameter.getStatus().equals("finished")) {
                    mangaStatusParameter.setStatus("none");
                } else {
                    mangaStatusParameter.setStatus("finished");
                    finished = finished + " :white_check_mark:";
                }
            } else if (parameter.equals("postponed")) {
                if (mangaStatusParameter.getStatus().equals("postponed")) {
                    mangaStatusParameter.setStatus("none");
                } else {
                    mangaStatusParameter.setStatus("postponed");
                    postponed = postponed + " :white_check_mark:";
                }
            }
        }

        mangaStatusParameterRepository.save(mangaStatusParameter);

        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .replyMarkup(getKeyboardForChangeStatus(read, planned, finished, postponed, mangaId)).build());
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, String mangaId) {
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(read)).callbackData("desu.me" + "\nchangeMangaStatusRead\n" + mangaId + "\nread").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(planned)).callbackData("desu.me" + "\nchangeMangaStatusPlanned\n" + mangaId + "\nplanned").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(finished)).callbackData("desu.me" + "\nchangeMangaStatusFinished\n" + mangaId + "\nfinished").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(postponed)).callbackData("desu.me" + "\nchangeMangaStatusPostponed\n" + mangaId + "\npostponed").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("desu.me" + "\nclickBackManga\n" + mangaId).build())
        )));
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatusViaProfile(String read, String planned, String finished, String postponed, String mangaId, String viaProfile) {
        return null;
    }

    @Override
    public void clickBackManga(CallbackQuery callbackQuery) {
        Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getMangaButtons(callbackQuery.getFrom().getId(), String.valueOf(mangaId)))
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());
    }
}
