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
import com.suttori.entity.User;
import com.suttori.telegram.*;
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
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
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

@Slf4j
@Service
public class MangaService {

    @Value("${telegraphApiToken}")
    private String telegraphApiToken;
    @Value("${telegraphAuthorName}")
    private String telegraphAuthorName;
    @Value("${telegraphAuthorUrl}")
    private String telegraphAuthorUrl;

    private TelegramSender telegramSender;
    private Util util;
    private DesuMeApiFeignClient desuMeApiFeignClient;
    private CopyMessageMangaRepository copyMessageMangaRepository;
    private MangaStatusParameterRepository mangaStatusParameterRepository;
    private HistoryEntityRepository historyEntityRepository;
    private StatisticEntityRepository statisticEntityRepository;
    private NotificationEntityRepository notificationEntityRepository;
    private NotificationChapterMappingRepository notificationChapterMappingRepository;
    private TelegraphApiFeignClient telegraphApiFeignClient;
    private UserRepository userRepository;
    private MangaDexApiFeignClient mangaDexApiFeignClient;
    private UploadMangaDexFeignClient uploadMangaDexFeignClient;


    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    public MangaService(TelegramSender telegramSender, Util util, DesuMeApiFeignClient desuMeApiFeignClient,
                        CopyMessageMangaRepository copyMessageMangaRepository, MangaStatusParameterRepository mangaStatusParameterRepository,
                        HistoryEntityRepository historyEntityRepository, StatisticEntityRepository statisticEntityRepository,
                        NotificationEntityRepository notificationEntityRepository, NotificationChapterMappingRepository notificationChapterMappingRepository,
                        TelegraphApiFeignClient telegraphApiFeignClient, UserRepository userRepository, MangaDexApiFeignClient mangaDexApiFeignClient, UploadMangaDexFeignClient uploadMangaDexFeignClient) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.desuMeApiFeignClient = desuMeApiFeignClient;
        this.copyMessageMangaRepository = copyMessageMangaRepository;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
        this.historyEntityRepository = historyEntityRepository;
        this.statisticEntityRepository = statisticEntityRepository;
        this.notificationEntityRepository = notificationEntityRepository;
        this.notificationChapterMappingRepository = notificationChapterMappingRepository;
        this.telegraphApiFeignClient = telegraphApiFeignClient;
        this.userRepository = userRepository;
        this.mangaDexApiFeignClient = mangaDexApiFeignClient;
        this.uploadMangaDexFeignClient = uploadMangaDexFeignClient;
    }


    public void clickSearch(Message message) {
        telegramSender.send(SendMessage.builder()
                .text("Нажми на кнопки чтобы начать искать или открыть общий каталог\n\nИнструкция по поиску: @searchInstructions")
                .chatId(message.getFrom().getId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать каталог")).callbackData("clickChooseCatalog").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск")).switchInlineQueryCurrentChat("").build()),
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск по хештегам")).switchInlineQueryCurrentChat("hashtag\n").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Последние обновления")).switchInlineQueryCurrentChat("last updated").build())
                )))).build());
    }

    public void clickSearch(CallbackQuery callbackQuery) {
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Нажми на кнопки чтобы начать искать или открыть общий каталог\n\nИнструкция по поиску: @searchInstructions")
                .chatId(callbackQuery.getFrom().getId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать каталог")).callbackData("clickChooseCatalog").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск")).switchInlineQueryCurrentChat("").build()),
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск по хештегам")).switchInlineQueryCurrentChat("hashtag\n").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Последние обновления")).switchInlineQueryCurrentChat("last updated").build())
                )))).build());
    }

    public void clickChooseCatalog(CallbackQuery callbackQuery) {
        //String text = "Выбери каталог в котором нужно искать мангу, сейчас доступны такие каталоги:\n\nHoney-manga.com.ua\nMangaDex.org\nReadManga.live\nDesu.me";
        String text = "Выбери каталог в котором нужно искать мангу, сейчас доступны такие каталоги:\n\nMangaDex.org\nDesu.me";
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text(text)
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        //new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Honey-manga.com.ua")).callbackData("chooseCatalog\nhoney-manga.com.ua").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("MangaDex.org")).callbackData("chooseCatalog\nmangadex.org").build()),
                        //new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("ReadManga.live")).callbackData("chooseCatalog\nreadmanga.live").build(),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Desu.me")).callbackData("chooseCatalog\ndesu.me").build())
                ))))
                .entities(getEntitiesChooseCatalog(text))
                .disableWebPagePreview(true)
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public List<MessageEntity> getEntitiesChooseCatalog(String text) {
        List<MessageEntity> entities = new ArrayList<>();
//        entities.add(MessageEntity.builder()
//                .type("text_link")
//                .url("Honey-manga.com.ua")
//                .length("Honey-manga.com.ua".length())
//                .offset(text.indexOf("Honey-manga.com.ua")).build());
        entities.add(MessageEntity.builder()
                .type("text_link")
                .url("MangaDex.org")
                .length("MangaDex.org".length())
                .offset(text.indexOf("MangaDex.org")).build());
//        entities.add(MessageEntity.builder()
//                .type("text_link")
//                .url("ReadManga.live")
//                .length("ReadManga.live".length())
//                .offset(text.indexOf("ReadManga.live")).build());
        entities.add(MessageEntity.builder()
                .type("text_link")
                .url("Desu.me")
                .length("Desu.me".length())
                .offset(text.indexOf("Desu.me")).build());
        return entities;
    }

    public void catalogWasChosen(CallbackQuery callbackQuery) {
        String catalog = util.parseValue(callbackQuery.getData())[1];
        String text;
        if (catalog != null) {
            userRepository.setCurrentMangaCatalog(catalog, callbackQuery.getFrom().getId());
            text = "Выбран каталог сайта " + catalog;
        } else {
            text = "Произошла ошибка при выборе каталога, лучше обратиться в поддержку";
        }
        clickSearch(callbackQuery);
        telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                .showAlert(false)
                .text(text)
                .callbackQueryId(callbackQuery.getId())
                .build());
    }

//    public void getSearchResultManga(InlineQuery inlineQuery) {
//        User user = userRepository.findByUserId(inlineQuery.getFrom().getId());
//
//        if (user.getCurrentMangaCatalog().equals("desu.me")) {
//            desuMeService.getSearchResultFromDesuMe(inlineQuery);
//        } else if (user.getCurrentMangaCatalog().equals("readmanga.live")) {
//        } else if (user.getCurrentMangaCatalog().equals("mangadex.org")) {
//            mangaDexService.getSearchResultFromMangaDex(inlineQuery);
//        } else if (user.getCurrentMangaCatalog().equals("honey-manga.com.ua")) {
//        }
//    }

    public void sendMangaById(Long userId, Long mangaDataId) {
//        MangaDataDesu mangaDataDesu = getMangaData(mangaDataId);
//        telegramSender.sendPhoto(SendPhoto.builder()
//                .photo(new InputFile(mangaDataDesu.getImage().getOriginal()))
//                .chatId(userId)
//                .parseMode("HTML")
//                .replyMarkup(getMangaButtons(mangaDataId, userId))
//                .caption(getMangaText(mangaDataDesu)).build());
    }

    public MangaDataDesu getMangaData(Long mangaId) {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            Response response = desuMeApiFeignClient.getMangaById(mangaId);
//            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
//            return mangaResponse.getResponse();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return null;
    }

    public InlineKeyboardMarkup getMangaButtons(Long userId, String mangaId) {
        String whiteCheckMark = "";
        if (notificationEntityRepository.findByMangaIdAndUserId(mangaId, userId) != null) {
            whiteCheckMark = " :white_check_mark:";
        }
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В закладки")).callbackData("changeStatus\n" + mangaId).build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Уведомления" + whiteCheckMark)).callbackData("clickNotification\n" + mangaId).build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к чтению")).callbackData("getChapters\n" + mangaId + "\n" + 1).build())
        )));
    }

    public void clickNotification(CallbackQuery callbackQuery) {
//        try {
//            Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
//            Long userId = callbackQuery.getFrom().getId();
//            NotificationEntity notificationEntity = notificationEntityRepository.findByMangaIdAndUserId(String.valueOf(mangaId), userId);
//            if (notificationEntity != null) {
//                notificationEntityRepository.delete(notificationEntity);
//            } else {
//                ObjectMapper objectMapper = new ObjectMapper();
//                Response response = desuMeApiFeignClient.getMangaById(mangaId);
//                String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
//                MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
//                Long lastChapter = Long.valueOf(mangaResponse.getResponse().getChapters().getLast().getCh());
//                notificationEntityRepository.save(new NotificationEntity(String.valueOf(mangaId), callbackQuery.getFrom().getId()));
//                if (notificationChapterMappingRepository.findByMangaId(String.valueOf(mangaId)) == null) {
//                    notificationChapterMappingRepository.save(new NotificationChapterMapping(String.valueOf(mangaId), lastChapter));
//                }
//                telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
//                        .callbackQueryId(callbackQuery.getId())
//                        .text("Теперь ты будешь получать уведомление о выходе новых глав!")
//                        .showAlert(true).build());
//            }
//            telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
//                    .replyMarkup(getMangaButtons(String.valueOf(mangaId), userId))
//                    .chatId(userId)
//                    .messageId(callbackQuery.getMessage().getMessageId()).build());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

//    public String getMangaText(MangaDataDesu mangaDataDesu) {
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("<b>").append(mangaDataDesu.getRussian()).append("</b>").append("\n\n");
//        stringBuilder.append("<b>").append("Рейтинг: ").append("</b>").append(mangaDataDesu.getScore()).append("\n");
//        stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(new SimpleDateFormat("yyyy").format(new Date(mangaDataDesu.getAired_on() * 1000))).append("\n");
//        stringBuilder.append("<b>").append("Тип: ").append("</b>").append(mangaDataDesu.getKind()).append("\n");
//        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(util.getStatus(mangaDataDesu.getStatus())).append("\n");
//        stringBuilder.append("<b>").append("Глав: ").append("</b>").append(mangaDataDesu.getChapters().getLast().getCh()).append("\n");
//        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(util.getGenres(mangaDataDesu.getGenres())).append("</i>\n\n");
//        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(mangaDataDesu.getDescription());
//
//        if (stringBuilder.length() > 1024) {
//            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
//            stringBuilder.append("...");
//        }
//        return stringBuilder.toString();
//    }

    public void getChapters(CallbackQuery callbackQuery) {
        InlineKeyboardMarkup inlineKeyboardMarkup = getMangaChaptersButton(callbackQuery);
        if (inlineKeyboardMarkup == null) {
            return;
        }
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .chatId(callbackQuery.getFrom().getId())
                .replyMarkup(inlineKeyboardMarkup)
                .messageId(callbackQuery.getMessage().getMessageId()).build());
    }

    public InlineKeyboardMarkup getMangaChaptersButton(CallbackQuery callbackQuery) {
        try {
            Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
            Response response = desuMeApiFeignClient.getMangaById(String.valueOf(mangaId));
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = new ObjectMapper().readValue(jsonResponse, MangaResponse.class);
            MangaDataDesu mangaDataDesu = mangaResponse.getResponse();
            MangaChapters mangaChapters = mangaDataDesu.getChapters();

            List<MangaChapterItem> mangaChapterItems = mangaChapters.getList();
            Collections.reverse(mangaChapterItems);


            List<InlineKeyboardRow> keyboard = new ArrayList<>();

            int chapterPerPage = 10;
            int gap = 15;
            int currentPage = Integer.parseInt(util.parseValue(callbackQuery.getData())[2]);
            if (currentPage == 1) {
                gap = 14;
            }
            int lastPage;
            if (mangaChapters.getList().size() % chapterPerPage == 0) {
                lastPage = mangaChapters.getList().size() / chapterPerPage;
            } else {
                lastPage = mangaChapters.getList().size() / chapterPerPage + 1;
            }
            currentPage = getCurrentPage(callbackQuery, currentPage, lastPage, gap);
            if (currentPage == 0) {
                return null;
            }

            int startIndex = currentPage * chapterPerPage - chapterPerPage;
            int endIndex = Math.min(startIndex + chapterPerPage, mangaChapters.getList().size());

            InlineKeyboardRow row = new InlineKeyboardRow();
            keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В начало")).callbackData("onFirstPage\n" + mangaId + "\n" + currentPage).build(),
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(String.valueOf(currentPage))).callbackData("currentPage\n" + mangaId + "\n" + currentPage).build(),
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В конец")).callbackData("onLastPage\n" + mangaId + "\n" + currentPage).build()));

            for (int i = startIndex; i < endIndex; i++) {
                String buttonText = "Том " + mangaChapterItems.get(i).getVol() + ". " + "Глава " + mangaChapterItems.get(i).getCh();
                if (row.size() == 2) {
                    keyboard.add(row);
                    row = new InlineKeyboardRow();
                }
                row.add(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(buttonText)).callbackData("chapter\n" + mangaDataDesu.getId() + "\n" + mangaChapterItems.get(i).getId()).build());
                if (i == endIndex - 1) {
                    keyboard.add(row);
                }
            }

            keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("<<")).callbackData("prevGapPage\n" + mangaId + "\n" + (currentPage - gap)).build(),
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("prevPage\n" + mangaId + "\n" + (currentPage - 1)).build(),
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData("nextPage\n" + mangaId + "\n" + (currentPage + 1)).build(),
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(">>")).callbackData("nextGapPage\n" + mangaId + "\n" + (currentPage + gap)).build()));
            return new InlineKeyboardMarkup(keyboard);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    public MangaDataDesu getMangaDataChapters(Long mangaId, Long mangaChapterItemsId) {
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

    public void getChapterFromCallbackHandler(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
        Long mangaChapterItemsId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        MangaDataDesu mangaDataDesu = getMangaDataChapters(mangaId, mangaChapterItemsId);
        if (callbackQuery.getData().contains("nextChapter\n") || callbackQuery.getData().contains("prevChapter\n")) {
            deleteKeyboard(callbackQuery.getMessage().getMessageId(), userId);
        }
        writeHistory(mangaDataDesu, userId);
        writeStatistic(mangaDataDesu, userId);
        getChapterHandler(mangaDataDesu, userId);
    }

    public void getChapterHandler(MangaDataDesu mangaDataDesu, Long userId) {
        MangaChapter copyMessageManga = copyMessageMangaRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), mangaDataDesu.getPages().getCh_curr().getVol(), mangaDataDesu.getPages().getCh_curr().getCh());
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

    public void writeHistory(MangaDataDesu mangaDataDesu, Long userId) {
        HistoryEntity historyEntity = historyEntityRepository.findByMangaIdAndUserId(mangaDataDesu.getId(), userId);
        if (historyEntity == null) {
            historyEntityRepository.save(new HistoryEntity(mangaDataDesu.getId(), userId, mangaDataDesu.getName(), mangaDataDesu.getRussian(), new Timestamp(System.currentTimeMillis())));
        } else {
            historyEntity.setUpdateAt(new Timestamp(System.currentTimeMillis()));
            historyEntityRepository.save(historyEntity);
        }
    }

    public void writeStatistic(MangaDataDesu mangaDataDesu, Long userId) {
        statisticEntityRepository.save(new StatisticEntity(mangaDataDesu.getId(), userId, mangaDataDesu.getName(), mangaDataDesu.getRussian(), mangaDataDesu.getPages().getCh_curr().getVol(), mangaDataDesu.getPages().getCh_curr().getCh(), new Timestamp(System.currentTimeMillis())));
    }

    public void waitForUploadManhwa(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {
        Integer messageIdForDelete = sendWaitGIFAndAction(userId);
        MangaChapter copyMessageManga;
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000);
                copyMessageManga = copyMessageMangaRepository.findById(copyMessageMangaId).get();
                if (copyMessageManga.getStatus().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        copyMessageManga = copyMessageMangaRepository.findById(copyMessageMangaId).get();
        if (copyMessageManga.getStatus().equals("process")) {
            copyMessageMangaRepository.delete(copyMessageManga);
            sendPDFChapter(userId, mangaDataDesu);
        } else if (copyMessageManga.getStatus().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu);
            telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
        }
    }

    public void waitForUploadManga(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {
        MangaChapter copyMessageManga;
        for (int i = 0; i < 30; i++) {
            try {
                Thread.sleep(1000);
                copyMessageManga = copyMessageMangaRepository.findById(copyMessageMangaId).get();
                if (copyMessageManga.getStatus().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        copyMessageManga = copyMessageMangaRepository.findById(copyMessageMangaId).get();
        if (copyMessageManga.getStatus().equals("process")) {
            copyMessageMangaRepository.delete(copyMessageManga);
            sendTelegraphArticle(userId, mangaDataDesu);
        } else if (copyMessageManga.getStatus().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(copyMessageManga.getMessageId(), userId, mangaDataDesu);
        }
    }

    public void preloadMangaChapter(Long userId, MangaDataDesu mangaDataDesu) {
        try {
            MangaChapter copyMessageManga = copyMessageMangaRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), mangaDataDesu.getPages().getCh_next().getVol(), mangaDataDesu.getPages().getCh_next().getCh());
            if (copyMessageManga != null) {
                return;
            }

            List<Node> content = new ArrayList<>();
            for (MangaPage mangaPage : getMangaDataChapters(mangaDataDesu.getId(), Long.valueOf(mangaDataDesu.getPages().getCh_next().getId())).getPages().getList()) {
                if (mangaPage.getHeight() / 3 >= mangaPage.getWidth()) {
                    preloadManhwaChapter(mangaDataDesu, userId);
                    return;
                }
                Node image = createImage(mangaPage.getImg().replace("desu.me", "desu.win"));
                content.add(image);
            }
            copyMessageManga = copyMessageMangaRepository.save(new MangaChapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), mangaDataDesu.getPages().getCh_next().getVol(), mangaDataDesu.getPages().getCh_next().getCh(), "process"));

            CreatePage createPage = new CreatePage(telegraphApiToken, mangaDataDesu.getName() + " Vol " + mangaDataDesu.getPages().getCh_next().getVol() + ". Chapter " + mangaDataDesu.getPages().getCh_next().getCh(), content)
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
                copyMessageMangaRepository.setMessageId(messageId, copyMessageManga.getId());
                copyMessageMangaRepository.setStatus("finished", copyMessageManga.getId());
                copyMessageMangaRepository.setTelegraphUrl(page.getUrl(), copyMessageManga.getId());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void preloadManhwaChapter(MangaDataDesu mangaDataDesu, Long userId) {
        try {
            MangaChapter copyMessageManga = copyMessageMangaRepository.findFirstByMangaIdAndVolAndChapter(String.valueOf(mangaDataDesu.getId()), mangaDataDesu.getPages().getCh_next().getVol(), mangaDataDesu.getPages().getCh_next().getCh());
            if (copyMessageManga != null) {
                return;
            }

            copyMessageManga = copyMessageMangaRepository.save(new MangaChapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), mangaDataDesu.getPages().getCh_next().getVol(), mangaDataDesu.getPages().getCh_next().getCh(), "process"));
            File pdfFolder = util.createStorageFolder("TemPdfStorage");
            String pdfFileName = pdfFolder + File.separator + mangaDataDesu.getName().replace(" ", "_") + "_Vol_" + mangaDataDesu.getPages().getCh_next().getVol() + "_Chapter_" + mangaDataDesu.getPages().getCh_next().getCh() + "_From_" + userId + "_timeStamp_" + System.currentTimeMillis() + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (MangaPage page : getMangaDataChapters(mangaDataDesu.getId(), Long.valueOf(mangaDataDesu.getPages().getCh_next().getId())).getPages().getList()) {
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


            File pdfFile = compressImages(pdfFileName, getMangaDataChapters(mangaDataDesu.getId(), Long.valueOf(mangaDataDesu.getPages().getCh_next().getId())), userId, 0.9);

            Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                    .document(new InputFile(pdfFile))
                    .caption(mangaDataDesu.getRussian() + "\n" + "Том " + mangaDataDesu.getPages().getCh_next().getVol() + ". Глава " + mangaDataDesu.getPages().getCh_next().getCh())
                    .chatId(-1002092468371L).build()).getMessageId();
            pdfFile.delete();

            if (messageId != null) {
                copyMessageMangaRepository.setMessageId(messageId, copyMessageManga.getId());
                copyMessageMangaRepository.setStatus("finished", copyMessageManga.getId());
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

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

    public void sendCopyMessageMangaFromMangaStorage(Integer messageId, Long userId, MangaDataDesu mangaDataDesu) {
        CopyMessage copyMessage = new CopyMessage(String.valueOf(userId), "-1002092468371L", messageId);
        InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(mangaDataDesu);
        if (inlineKeyboardMarkup != null) {
            copyMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        try {
            telegramSender.resendCopyMessageFromStorage(copyMessage);
        } catch (ExecutionException | InterruptedException e) {
            copyMessageMangaRepository.deleteByMessageId(messageId);
            getChapterHandler(mangaDataDesu, userId);
            log.error("Copy message not found: " + mangaDataDesu.getRussian() + " messageId" + messageId);
        }
    }

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

            MangaChapter copyMessageManga = copyMessageMangaRepository.save(new MangaChapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), mangaDataDesu.getPages().getCh_curr().getVol(), mangaDataDesu.getPages().getCh_curr().getCh(), "process"));

            CreatePage createPage = new CreatePage(telegraphApiToken, mangaDataDesu.getName() + " Vol " + mangaDataDesu.getPages().getCh_curr().getVol() + ". Chapter " + mangaDataDesu.getPages().getCh_curr().getCh(), content)
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
                copyMessageMangaRepository.setMessageId(messageId, copyMessageManga.getId());
                copyMessageMangaRepository.setStatus("finished", copyMessageManga.getId());
                copyMessageMangaRepository.setTelegraphUrl(page.getUrl(), copyMessageManga.getId());


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

    public void sendPDFChapter(Long userId, MangaDataDesu mangaDataDesu) {
        try {
            MangaChapter copyMessageManga = copyMessageMangaRepository.save(new MangaChapter(String.valueOf(mangaDataDesu.getId()),
                    mangaDataDesu.getName(), mangaDataDesu.getPages().getCh_curr().getVol(), mangaDataDesu.getPages().getCh_curr().getCh(), "process"));
            File pdfFolder = util.createStorageFolder("TemPdfStorage");
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
                copyMessageMangaRepository.setMessageId(messageId, copyMessageManga.getId());
                copyMessageMangaRepository.setStatus("finished", copyMessageManga.getId());

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

    public File getJpg(java.io.File folder, URL imgUrl, String fileName) {
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

    public InlineKeyboardMarkup getPrevNextButtons(MangaDataDesu mangaDataDesu) {
        InlineKeyboardMarkup inlineKeyboardMarkup;
        if (mangaDataDesu.getPages().getCh_prev().getId() == -1) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(new InlineKeyboardRow(
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующая глава")).callbackData("nextChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_next().getId()).build())
            )));
        } else if (mangaDataDesu.getPages().getCh_next().getId() == -1) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(new InlineKeyboardRow(
                    InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущая глава")).callbackData("prevChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_prev().getId()).build())
            )));
        } else if (mangaDataDesu.getPages().getCh_prev().getId() == -1 && mangaDataDesu.getPages().getCh_next().getId() == -1) {
            inlineKeyboardMarkup = null;
        } else {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущая глава")).callbackData("prevChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_prev().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующая глава")).callbackData("nextChapter\n" + mangaDataDesu.getId() + "\n" + mangaDataDesu.getPages().getCh_next().getId()).build())
            )));
        }
        return inlineKeyboardMarkup;
    }

    public Node createImage(String imageUrl) {
        NodeElement image = new NodeElement();
        image.setTag("img");
        image.setAttrs(new HashMap<>());
        image.getAttrs().put("src", imageUrl);
        return image;
    }

    public Integer sendWaitGIFAndAction(Long userId) {
        Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                .chatId(userId)
                .caption("Твоя глава уже загружается, обычно это занимает не больше минуты, спасибо за ожидание\n\nВ боте предусмотрена автоматическая предзагрузка глав, поэтому пока ты будешь читать текующую главу, следующая уже будет загружена")
                .document(new InputFile("CgACAgQAAxkBAAICV2XKAyJ_d0xIoK5tTXiI14xVYCB5AAKJCwACye1AUZtzbClFKHTFNAQ")).build()).getMessageId();
        telegramSender.sendChatAction(userId, "upload_document");
        return messageId;
    }

    public Integer sendWaitForUploadManhwa(Long userId) {
        return telegramSender.sendDocument(SendDocument.builder()
                .chatId(userId)
                .caption("Прямо сейчас кто-то загружает главу или пытался это сделать и у него не получилось. Через минуту загрузка главы начнется повторно, если в этот раз глава не будет загружена, то напиши в поддержку, значит возникла какая-то ошибка")
                .document(new InputFile("CgACAgQAAxkBAAICV2XKAyJ_d0xIoK5tTXiI14xVYCB5AAKJCwACye1AUZtzbClFKHTFNAQ")).build()).getMessageId();
    }

    public void deleteKeyboard(Integer messageId, Long userId) {
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>()))
                .messageId(messageId)
                .chatId(userId).build());
    }

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
            sendMangaById(userId, random.nextLong(mangaData.getId() + 1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clickChangeMangaStatus(CallbackQuery callbackQuery) {
        Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
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

        InlineKeyboardMarkup inlineKeyboardMarkup;
        if (callbackQuery.getData().contains("ViaHistory")) {
            inlineKeyboardMarkup = getKeyboardForChangeStatusViaProfile(read, planned, finished, postponed, mangaId, "ViaHistory");
            inlineKeyboardMarkup.getKeyboard().add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("sendManga" + "ViaHistory" + "\n" + mangaId).build()));
        } else if (callbackQuery.getData().contains("ViaFavorites")) {
            inlineKeyboardMarkup = getKeyboardForChangeStatusViaProfile(read, planned, finished, postponed, mangaId, "ViaFavorites");
            inlineKeyboardMarkup.getKeyboard().add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("sendManga" + "ViaFavorites" + "\n" + mangaId + "\n" + "none").build()));
        } else {
            inlineKeyboardMarkup = getKeyboardForChangeStatus(read, planned, finished, postponed, mangaId);
        }

        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public void clickMangaStatus(CallbackQuery callbackQuery) {
        Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
        Long userId = callbackQuery.getFrom().getId();
        String parameter = util.parseValue(callbackQuery.getData())[2];

        MangaDataDesu mangaDataDesu = getMangaData(mangaId);

        String read = "Читаю";
        String planned = "В планах";
        String finished = "Прочитано";
        String postponed = "Отложено";

        MangaStatusParameter mangaStatusParameter = mangaStatusParameterRepository.findByMangaIdAndUserId(mangaId, userId);

        if (mangaStatusParameter == null) {
            mangaStatusParameter = new MangaStatusParameter();
            mangaStatusParameter.setMangaId(mangaId);
            mangaStatusParameter.setUserId(userId);
            mangaStatusParameter.setStatus(parameter);
            mangaStatusParameter.setRussian(mangaDataDesu.getRussian());
            mangaStatusParameter.setName(mangaDataDesu.getName());
            mangaStatusParameter.setAddedAt(new Timestamp(System.currentTimeMillis()));

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

        InlineKeyboardMarkup inlineKeyboardMarkup;
        if (callbackQuery.getData().contains("ViaHistory")) {
            inlineKeyboardMarkup = getKeyboardForChangeStatusViaProfile(read, planned, finished, postponed, mangaId, "ViaHistory");
            inlineKeyboardMarkup.getKeyboard().add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("sendManga" + "ViaHistory" + "\n" + mangaId).build()));
        } else if (callbackQuery.getData().contains("ViaFavorites")) {
            inlineKeyboardMarkup = getKeyboardForChangeStatusViaProfile(read, planned, finished, postponed, mangaId, "ViaFavorites");
            inlineKeyboardMarkup.getKeyboard().add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("sendManga" + "ViaFavorites" + "\n" + mangaId + "\n" + "none").build()));
        } else {
            inlineKeyboardMarkup = getKeyboardForChangeStatus(read, planned, finished, postponed, mangaId);
        }


        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, Long mangaId) {
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(read)).callbackData("changeMangaStatusRead\n" + mangaId + "\nread").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(planned)).callbackData("changeMangaStatusPlanned\n" + mangaId + "\nplanned").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(finished)).callbackData("changeMangaStatusFinished\n" + mangaId + "\nfinished").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(postponed)).callbackData("changeMangaStatusPostponed\n" + mangaId + "\npostponed").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickBackManga\n" + mangaId).build())
        )));
    }

    public InlineKeyboardMarkup getKeyboardForChangeStatusViaProfile(String read, String planned, String finished, String postponed, Long mangaId, String viaProfile) {
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(read)).callbackData("changeMangaStatusRead" + viaProfile + "\n" + mangaId + "\nread").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(planned)).callbackData("changeMangaStatusPlanned" + viaProfile + "\n" + mangaId + "\nplanned").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(finished)).callbackData("changeMangaStatusFinished" + viaProfile + "\n" + mangaId + "\nfinished").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(postponed)).callbackData("changeMangaStatusPostponed" + viaProfile + "\n" + mangaId + "\npostponed").build())
        )));
    }

    public void clickBackManga(CallbackQuery callbackQuery) {
        Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getMangaButtons(callbackQuery.getFrom().getId(), String.valueOf(mangaId)))
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public void doBackup() {
        ArrayList<MangaChapter> copyMessageMangas = copyMessageMangaRepository.findAllByBackupMessageIdIsNull();
        for (MangaChapter copyMessageManga : copyMessageMangas) {
            if (copyMessageManga.getMessageId() == null) {
                continue;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            MessageId messageId = telegramSender.sendCopyMessage(CopyMessage.builder()
                    .messageId(copyMessageManga.getMessageId())
                    .fromChatId(-1002092468371L)
                    .chatId(-1002119024676L).build());
            if (messageId != null) {
                copyMessageManga.setBackupMessageId(Math.toIntExact(messageId.getMessageId()));
                copyMessageMangaRepository.save(copyMessageManga);
            }
        }
    }
}
