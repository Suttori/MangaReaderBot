package com.suttori.service;

import com.suttori.config.ServiceConfig;
import com.suttori.dao.*;
import com.suttori.dto.ChapterDto;
import com.suttori.entity.*;
import com.suttori.entity.User;
import com.suttori.exception.CatalogNotFoundException;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.*;
import com.suttori.util.MangaUtil;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.*;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class MangaService {


    private TelegramSender telegramSender;
    private Util util;
    private MangaUtil mangaUtil;

    private MangaChapterRepository mangaChapterRepository;
    private MangaStatusParameterRepository mangaStatusParameterRepository;
    private HistoryEntityRepository historyEntityRepository;
    private StatisticEntityRepository statisticEntityRepository;
    private UserRepository userRepository;
    private ServiceConfig serviceConfig;
    private ReadStatusRepository readStatusRepository;
    private MangaRepository mangaRepository;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);


    @Autowired
    public MangaService(TelegramSender telegramSender, Util util,
                        MangaUtil mangaUtil, MangaChapterRepository mangaChapterRepository, MangaStatusParameterRepository mangaStatusParameterRepository,
                        HistoryEntityRepository historyEntityRepository, StatisticEntityRepository statisticEntityRepository,
                        UserRepository userRepository, ServiceConfig serviceConfig, ReadStatusRepository readStatusRepository, MangaRepository mangaRepository) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaUtil = mangaUtil;
        this.mangaChapterRepository = mangaChapterRepository;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
        this.historyEntityRepository = historyEntityRepository;
        this.statisticEntityRepository = statisticEntityRepository;
        this.userRepository = userRepository;
        this.serviceConfig = serviceConfig;
        this.readStatusRepository = readStatusRepository;
        this.mangaRepository = mangaRepository;
    }

    private static final Map<String, Map<String, String>> LANGUAGE_CODE = new LinkedHashMap<>();

    static {
        LANGUAGE_CODE.put("English", createMap("en", "https://eu-north-1.console.aws.amazon.com/s3/object/gorillastorage?region=eu-north-1&bucketType=general&prefix=MangaReaderBot/Flags/gb.jpg"));
        LANGUAGE_CODE.put("Ukrainian", createMap("uk", "https://eu.jpg"));
        LANGUAGE_CODE.put("Russian", createMap("ru", "https://eu.jpg"));
        LANGUAGE_CODE.put("Arabic", createMap("ar", "https://eu-north-1.console.aws.amazon.com/s3/object/gorillastorage?region=eu-north-1&bucketType=general&prefix=MangaReaderBot/Flags/sa.jpg"));
        LANGUAGE_CODE.put("Azerbaijani", createMap("az", "https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/az.jpg"));
        LANGUAGE_CODE.put("Bengali", createMap("bn", "https://eu-north-1.console.aws.amazon.com/s3/object/gorillastorage?region=eu-north-1&bucketType=general&prefix=MangaReaderBot/Flags/bd.jpg"));
        LANGUAGE_CODE.put("Bulgarian", createMap("bg", "https://eu-north-1.console.aws.amazon.com/s3/object/gorillastorage?region=eu-north-1&bucketType=general&prefix=MangaReaderBot/Flags/bg.jpg"));
        LANGUAGE_CODE.put("Burmese", createMap("my", "https://eu-north-1.console.aws.amazon.com/s3/object/gorillastorage?region=eu-north-1&bucketType=general&prefix=MangaReaderBot/Flags/mm.jpg"));
        LANGUAGE_CODE.put("Catalan, Valencian", createMap("ca", "https://eu.jpg"));
        LANGUAGE_CODE.put("Chinese (Simplified)", createMap("zh", "https://eu.jpg"));
        LANGUAGE_CODE.put("Croatian", createMap("hr", "https://eu.jpg"));
        LANGUAGE_CODE.put("Czech", createMap("cs", "https://eu.jpg"));
        LANGUAGE_CODE.put("Danish", createMap("da", "https://eu.jpg"));
        LANGUAGE_CODE.put("Dutch", createMap("nl", "https://eu.jpg"));
        LANGUAGE_CODE.put("Esperanto", createMap("eo", "https://eu.jpg"));
        LANGUAGE_CODE.put("Estonian", createMap("et", "https://eu.jpg"));
        LANGUAGE_CODE.put("Filipino", createMap("fi", "https://eu.jpg"));
        LANGUAGE_CODE.put("Finnish", createMap("hr", "https://eu.jpg"));
        LANGUAGE_CODE.put("French", createMap("fr", "https://eu.jpg"));
        LANGUAGE_CODE.put("Georgian", createMap("ka", "https://eu.jpg"));
        LANGUAGE_CODE.put("German", createMap("de", "https://eu.jpg"));
        LANGUAGE_CODE.put("Greek", createMap("el", "https://eu.jpg"));
        LANGUAGE_CODE.put("Hebrew", createMap("he", "https://eu.jpg"));
        LANGUAGE_CODE.put("Hindi", createMap("hi", "https://eu.jpg"));
        LANGUAGE_CODE.put("Hungarian", createMap("hu", "https://eu.jpg"));
        LANGUAGE_CODE.put("Indonesian", createMap("id", "https://eu.jpg"));
        LANGUAGE_CODE.put("Italian", createMap("it", "https://eu.jpg"));
        LANGUAGE_CODE.put("Japanese", createMap("ja", "https://eu.jpg"));
        LANGUAGE_CODE.put("Kazakh", createMap("kk", "https://eu.jpg"));
        LANGUAGE_CODE.put("Korean", createMap("ko", "https://eu.jpg"));
        LANGUAGE_CODE.put("Latin", createMap("la", "https://eu.jpg"));
        LANGUAGE_CODE.put("Lithuanian", createMap("lt", "https://eu.jpg"));
        LANGUAGE_CODE.put("Malay", createMap("ms", "https://eu.jpg"));
        LANGUAGE_CODE.put("Mongolian", createMap("mn", "https://eu.jpg"));
        LANGUAGE_CODE.put("Nepali", createMap("ne", "https://eu.jpg"));
        LANGUAGE_CODE.put("Norwegian", createMap("no", "https://eu.jpg"));
        LANGUAGE_CODE.put("Persian", createMap("fa", "https://eu.jpg"));
        LANGUAGE_CODE.put("Polish", createMap("pl", "https://eu.jpg"));
        LANGUAGE_CODE.put("Portuguese", createMap("pt", "https://eu.jpg"));
        LANGUAGE_CODE.put("Portuguese (Br)", createMap("pt-br", "https://eu.jpg"));
        LANGUAGE_CODE.put("Romanian", createMap("ro", "https://eu.jpg"));
        LANGUAGE_CODE.put("Serbian", createMap("sr", "https://eu.jpg"));
        LANGUAGE_CODE.put("Slovak", createMap("sk", "https://eu.jpg"));
        LANGUAGE_CODE.put("Slovenian", createMap("sl", "https://eu.jpg"));
        LANGUAGE_CODE.put("Spanish", createMap("es", "https://eu.jpg"));
        LANGUAGE_CODE.put("Spanish (LATAM)", createMap("es-la", "https://eu.jpg"));
        LANGUAGE_CODE.put("Swedish", createMap("sv", "https://eu.jpg"));
        LANGUAGE_CODE.put("Thai", createMap("th", "https://eu.jpg"));
        LANGUAGE_CODE.put("Turkish", createMap("tr", "https://eu.jpg"));
        LANGUAGE_CODE.put("Vietnamese", createMap("vi", "https://eu.jpg"));
    }

    private static Map<String, String> createMap(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public void clickSearch(Message message) {
        telegramSender.send(SendMessage.builder()
                .text("Нажми на кнопки чтобы начать искать или открыть общий каталог\n\nИнструкция по поиску: @searchInstructions")
                .chatId(message.getFrom().getId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать каталог")).callbackData("clickChooseCatalog").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Сортировка и фильтры")).callbackData("clickSetSortFilterParams").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск")).switchInlineQueryCurrentChat("").build())
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск по хештегам")).switchInlineQueryCurrentChat("hashtag\n").build()),
//                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Последние обновления")).switchInlineQueryCurrentChat("last updated").build())
                )))).build());
    }

    public void clickSearch(CallbackQuery callbackQuery) {
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Нажми на кнопки чтобы начать искать или открыть общий каталог\n\nИнструкция по поиску: @searchInstructions")
                .chatId(callbackQuery.getFrom().getId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать каталог")).callbackData("clickChooseCatalog").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Сортировка и фильтры")).callbackData("clickSetSortFilterParams").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск")).switchInlineQueryCurrentChat("").build())
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск по хештегам")).switchInlineQueryCurrentChat("hashtag\n").build()),
                        //new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Последние обновления")).switchInlineQueryCurrentChat("last updated").build())
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
            util.sendErrorMessage("Произошла ошибка при выборе каталога, лучше обратиться в поддержку", callbackQuery.getFrom().getId());
            return;
        }
        clickSearch(callbackQuery);
        telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                .showAlert(false)
                .text(text)
                .callbackQueryId(callbackQuery.getId())
                .build());
    }

    public void chooseMangaDexCatalog(CallbackQuery callbackQuery) {
        String catalog = util.parseValue(callbackQuery.getData())[1];
        if (catalog != null) {
            telegramSender.sendEditMessageText(EditMessageText.builder()
                    .text("Каталог сайта MangaDex.org поддержавает много языков, нажми на кнопку ниже чтобы выбрать на каком языке искать мангу")
                    .chatId(callbackQuery.getFrom().getId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                            new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать язык")).switchInlineQueryCurrentChat("SetLanguageCode").build())
                    )))).build());
        } else {
            telegramSender.send(SendMessage.builder()
                    .text("Произошла ошибка при выборе каталога, лучше обратиться в поддержку")
                    .chatId(callbackQuery.getFrom().getId()).build());
        }
    }

    public void chooseLanguageCodeMangaDex(InlineQuery inlineQuery) {
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Map<String, String>> outerEntry : LANGUAGE_CODE.entrySet()) {
            String outerKey = outerEntry.getKey();
            Map<String, String> innerMap = outerEntry.getValue();
            for (Map.Entry<String, String> innerEntry : innerMap.entrySet()) {
                String innerKey = innerEntry.getKey();
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(outerKey)
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                        .inputMessageContent(new InputTextMessageContent("chooseLanguageCodeMangaDex\n" + innerKey)).build());
            }
        }

        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public void setLanguageCodeMangaDexAndCatalog(Message message) {
        String languageCode = util.parseValue(message.getText())[1];
        for (Map.Entry<String, Map<String, String>> outerEntry : LANGUAGE_CODE.entrySet()) {
            Map<String, String> innerMap = outerEntry.getValue();
            for (Map.Entry<String, String> innerEntry : innerMap.entrySet()) {
                if (innerEntry.getKey().equals(languageCode)) {
                    userRepository.setCurrentMangaCatalog("mangadex.org", message.getFrom().getId());
                    userRepository.setCurrentLanguageCodeForCatalog(languageCode, message.getFrom().getId());
                    util.deleteMessageByMessageId(message.getFrom().getId(), message.getMessageId());
                    util.sendInfoMessage("Язык был успешно выбран, можешь вернутся к поиску", message.getFrom().getId());
                    clickSearch(message);
                    return;
                }
            }
        }
        util.sendErrorMessage("Возникла ошибка при выборе языка, попробуй еще раз и, если проблема повторится, то обратись в поддержку", message.getFrom().getId());
    }

    public void getChapterFromMessageHandler(Message message) {
        String catalogName;
        try {
            catalogName = util.getSourceName(message.getText());
        } catch (CatalogNotFoundException e) {
            log.error("Ошибка при получении каталога", e);
            util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }

        Long userId = message.getFrom().getId();
        ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(Long.valueOf(util.parseValue(message.getText())[2]));
        Chapter chapter = mangaUtil.getChapterByDto(chapterDto);
        if (chapter == null) {
            util.sendErrorMessage("Глава не найдена, перезапусти бот и попробуй снова. Если не получится, то обратись в поддержку.", userId);
            return;
        }
        writeHistory(chapter, userId, catalogName);
        writeStatistic(chapter, userId, catalogName);
        getChapterHandler(chapter, userId);
    }

    public void getChapterFromCallbackHandler(CallbackQuery callbackQuery) {
        String catalogName;
        try {
            catalogName = util.getSourceName(callbackQuery.getData());
        } catch (CatalogNotFoundException e) {
            log.error("Ошибка при получении каталога", e);
            util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", callbackQuery.getFrom().getId());
            return;
        }
        Long userId = callbackQuery.getFrom().getId();
        ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(Long.valueOf(util.parseValue(callbackQuery.getData())[2]));
        Chapter chapter = mangaUtil.getChapterByDto(chapterDto);

        if (chapter == null) {
            util.sendErrorMessage("Глава не найдена, перезапусти бот и попробуй снова. Если не получится, то обратись в поддержку.", userId);
            return;
        }

        if (callbackQuery.getData().contains("nextChapter\n") || callbackQuery.getData().contains("prevChapter\n")) {
            deleteKeyboard(callbackQuery.getMessage().getMessageId(), userId);
        }
        writeHistory(chapter, userId, catalogName);
        writeStatistic(chapter, userId, catalogName);
        getChapterHandler(chapter, userId);
    }

    public void handleChaptersPack(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        User user = userRepository.findByUserId(userId);

        if (user.getIsPremiumBotUser() == null || !user.getIsPremiumBotUser()) {
            util.sendErrorMessage("Что-то не так с твоей подпиской. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", callbackQuery.getFrom().getId());
            return;
        }
        List<Chapter> chapters = new ArrayList<>();
        String catalogName;
        try {
            catalogName = util.getSourceName(callbackQuery.getData());
        } catch (CatalogNotFoundException e) {
            log.error("Ошибка при получении каталога", e);
            util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", callbackQuery.getFrom().getId());
            return;
        }

        ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(Long.valueOf(util.parseValue(callbackQuery.getData())[2]));
        Chapter chapter = mangaUtil.getChapterByDto(chapterDto);

        if (chapter == null) {
            util.sendErrorMessage("Глава не найдена, перезапусти бот и попробуй снова. Если не получится, то обратись в поддержку.", userId);
            return;
        }

        if (callbackQuery.getData().contains("nextChaptersPack\n") || callbackQuery.getData().contains("prevChaptersPack\n")) {
            deleteKeyboard(callbackQuery.getMessage().getMessageId(), userId);
        }

        writeHistory(chapter, userId, catalogName);
        chapters.add(chapter);
        Chapter currentChapter = chapter;
        for (int i = 1; i < Integer.parseInt(user.getNumberOfChaptersSent()); i++) {
            if (callbackQuery.getData().contains("nextChaptersPack\n")) {
                if (currentChapter.getNextChapter() != null) {
                    currentChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoById(currentChapter.getNextChapter().getId()));
                    writeStatistic(currentChapter, userId, catalogName);
                    chapters.add(currentChapter);
                } else {
                    sendChaptersPack(chapters, user);
                    return;
                }
            } else {
                if (currentChapter.getPrevChapter() != null) {
                    currentChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoById(currentChapter.getPrevChapter().getId()));
                    writeStatistic(currentChapter, userId, catalogName);
                    chapters.add(currentChapter);
                } else {
                    sendChaptersPack(chapters, user);
                    return;
                }
            }
        }
        sendChaptersPack(chapters, user);
    }

    public Integer sendDownloadStatus(boolean flagEditMessageText, Integer tempDownloadMessageId, Chapter chapter, Long userId) {
        if (flagEditMessageText) {
            return telegramSender.send(SendMessage.builder()
                    .chatId(userId)
                    .text("Загружаю том " + chapter.getVol() + " главу " + chapter.getChapter() + "...").build()).getMessageId();
        } else {
            return telegramSender.sendEditMessageTextAsync(EditMessageText.builder()
                    .text("Загружаю том " + chapter.getVol() + " главу " + chapter.getChapter() + "...")
                    .messageId(tempDownloadMessageId)
                    .chatId(userId).build()).getMessageId();
        }
    }

    public void sendChaptersPack(List<Chapter> chapters, User user) {
        StringBuilder textTelegraphArticleChapters = new StringBuilder();
        List<MessageEntity> messageEntities = new ArrayList<>();
        List<Integer> messageIds = new ArrayList<>();
        Chapter lastChapter = chapters.get(chapters.size() - 1);
        Integer tempDownloadMessageId = null;
        boolean flagEditMessageText = true;

        for (Chapter chapter : chapters) {
            if ((user.getMangaFormatParameter() == null && (mangaUtil.isNotLongStripMangaDex(chapter) || mangaUtil.isMangaDesuMe(chapter))) || (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("telegraph"))) {
                if (chapter.getTelegraphStatusDownload() != null && chapter.getTelegraphStatusDownload().equals("finished")) {
                    String chapterSting = "\n" + chapter.getName() + " Том " + chapter.getVol() + ". Глава " + chapter.getChapter();
                    messageEntities.add(MessageEntity.builder()
                            .type("text_link")
                            .length(chapterSting.length())
                            .offset(textTelegraphArticleChapters.length())
                            .url(chapter.getTelegraphUrl()).build());
                    textTelegraphArticleChapters.append(chapterSting);
                } else {
                    tempDownloadMessageId = sendDownloadStatus(flagEditMessageText, tempDownloadMessageId, chapter, user.getUserId());
                    flagEditMessageText = false;

                    Integer chapterMessageId = serviceConfig.mangaServices().get(chapter.getCatalogName()).createTelegraphArticleChapter(user.getUserId(), chapter);
                    if (chapterMessageId == null) {
                        log.error("chapterMessageId null");
                        continue;
                    }
                    chapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoById(chapter.getId()));

                    if (chapter.getTelegraphMessageId() != null && chapter.getTelegraphMessageId().equals(chapterMessageId)) {
                        String chapterSting = "\n" + chapter.getName() + " Том " + chapter.getVol() + ". Глава " + chapter.getChapter();
                        messageEntities.add(MessageEntity.builder()
                                .type("text_link")
                                .length(chapterSting.length())
                                .offset(textTelegraphArticleChapters.length())
                                .url(chapter.getTelegraphUrl()).build());
                        textTelegraphArticleChapters.append(chapterSting);
                    } else if (chapter.getPdfMessageId() != null && chapter.getPdfMessageId().equals(chapterMessageId)) {
                        telegramSender.deleteMessageById(String.valueOf(user.getUserId()), tempDownloadMessageId);
                        flagEditMessageText = true;
                        if (!textTelegraphArticleChapters.isEmpty()) {
                            telegramSender.send(SendMessage.builder()
                                    .text(String.valueOf(textTelegraphArticleChapters))
                                    .entities(messageEntities)
                                    .chatId(user.getUserId()).build());
                            textTelegraphArticleChapters = new StringBuilder();
                            messageEntities.clear();
                        }
                        telegramSender.sendCopyMessage(CopyMessage.builder()
                                .messageId(chapter.getPdfMessageId())
                                .fromChatId("-1002092468371L")
                                .chatId(user.getUserId()).build());
                    }
                }
            } else {
                if (chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("finished")) {
                    messageIds.add(chapter.getPdfMessageId());
                } else {
                    if (!messageIds.isEmpty()) {
                        messageIds.sort(Comparator.naturalOrder());
                        telegramSender.sendCopyMessages(CopyMessages.builder()
                                .chatId(user.getUserId())
                                .fromChatId("-1002092468371L")
                                .messageIds(messageIds).build());
                        messageIds.clear();
                    }
                    Integer messageId = telegramSender.send(SendMessage.builder()
                            .chatId(user.getUserId())
                            .text("Загружаю том " + chapter.getVol() + " главу " + chapter.getChapter() + "...").build()).getMessageId();

                    serviceConfig.mangaServices().get(chapter.getCatalogName()).createPdfChapter(user.getUserId(), chapter);
                    chapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoById(chapter.getId()));
                    if (chapter.getPdfMessageId() == null) {
                        waitForUploadManhwa(user.getUserId(), chapter);
                        telegramSender.deleteMessageById(String.valueOf(user.getUserId()), messageId);
                        continue;
                    }
                    telegramSender.deleteMessageById(String.valueOf(user.getUserId()), messageId);
                    telegramSender.sendCopyMessage(CopyMessage.builder()
                            .messageId(chapter.getPdfMessageId())
                            .fromChatId("-1002092468371L")
                            .chatId(user.getUserId()).build());
                }
            }
        }

        if (!textTelegraphArticleChapters.isEmpty()) {
            telegramSender.send(SendMessage.builder()
                    .text(String.valueOf(textTelegraphArticleChapters))
                    .entities(messageEntities)
                    .replyMarkup(serviceConfig.mangaServices().get(lastChapter.getCatalogName()).getPrevNextButtons(lastChapter, user.getUserId()))
                    .chatId(user.getUserId()).build());
            telegramSender.deleteMessageById(String.valueOf(user.getUserId()), tempDownloadMessageId);
        } else {
            if (!messageIds.isEmpty()) {
                messageIds.sort(Comparator.naturalOrder());
                telegramSender.sendCopyMessages(CopyMessages.builder()
                        .chatId(user.getUserId())
                        .fromChatId("-1002092468371L")
                        .messageIds(messageIds).build());
            }
            telegramSender.send(SendMessage.builder()
                    .text("Навигация")
                    .replyMarkup(serviceConfig.mangaServices().get(lastChapter.getCatalogName()).getPrevNextButtons(lastChapter, user.getUserId()))
                    .chatId(user.getUserId()).build());

        }


    }

    public void writeHistory(Chapter chapter, Long userId, String catalogName) {
        HistoryEntity historyEntity = historyEntityRepository.findByMangaIdAndUserId(chapter.getMangaId(), userId);
        if (historyEntity == null) {
            historyEntityRepository.save(new HistoryEntity(chapter.getMangaId(), chapter.getMangaDataBaseId(), userId, chapter.getName(), chapter.getName(), new Timestamp(System.currentTimeMillis()), catalogName));
        } else {
            historyEntity.setUpdateAt(new Timestamp(System.currentTimeMillis()));
            if (historyEntity.getMangaDatabaseId() == null) {
                historyEntity.setMangaDatabaseId(chapter.getMangaDataBaseId());
            }
            historyEntityRepository.save(historyEntity);
        }
    }

    public void writeStatistic(Chapter chapter, Long userId, String catalogName) {
        statisticEntityRepository.save(new StatisticEntity(chapter.getMangaId(), userId, chapter.getName(), chapter.getName(), chapter.getVol(), chapter.getChapter(), new Timestamp(System.currentTimeMillis()), catalogName));
    }

    public void getChapterHandler(Chapter chapter, Long userId) {
        User user = userRepository.findByUserId(userId);
        MangaServiceInterface service = serviceConfig.mangaServices().get(chapter.getCatalogName());

//        if (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("cbz")) {
//            Integer messageIdChapterInStorage = service.createCbzChapter(userId, chapter);
//            sendChapterToUser(messageIdChapterInStorage, chapter, userId);
//        } else
        if ((user.getMangaFormatParameter() == null && (mangaUtil.isNotLongStripMangaDex(chapter) || mangaUtil.isMangaDesuMe(chapter))) || (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("telegraph"))) {
            if (chapter.getTelegraphStatusDownload() != null && chapter.getTelegraphStatusDownload().equals("process")) {
                waitForUploadManga(userId, chapter);
                executorService.submit(() ->
                        service.preloadMangaChapter(userId, chapter)
                );
                return;
            }
            if (chapter.getTelegraphStatusDownload() != null && chapter.getTelegraphStatusDownload().equals("finished")) {
                executorService.submit(() ->
                        sendCopyMessageMangaFromMangaStorage(userId, chapter)
                );
            } else {
                executorService.submit(() -> {
                            Integer messageIdChapterInStorage = service.createTelegraphArticleChapter(userId, chapter);
                            sendChapterToUser(messageIdChapterInStorage, chapter, userId);
                        }
                );
            }
            executorService.submit(() ->
                    serviceConfig.mangaServices().get(chapter.getCatalogName()).preloadMangaChapter(userId, chapter)
            );
        } else {
            if (chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("process")) {
                Integer messageIdForDelete = mangaUtil.sendWaitGIFAndAction(userId);
                waitForUploadManhwa(userId, chapter);
                telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
                executorService.submit(() ->
                        service.preloadManhwaChapter(userId, chapter)
                );
                return;
            }
            if (chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("finished")) {
                executorService.submit(() ->
                        sendCopyMessageManhwaFromMangaStorage(userId, chapter)
                );
            } else {
                executorService.submit(() -> {
                            Integer messageIdForDelete = mangaUtil.sendWaitGIFAndAction(userId);
                            Integer messageIdChapterInStorage = service.createPdfChapter(userId, chapter);
                            sendChapterToUser(messageIdChapterInStorage, chapter, userId);
                            telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
                        }

                );
            }
            executorService.submit(() ->
                    service.preloadManhwaChapter(userId, chapter)
            );
        }
    }


    public void waitForUploadManhwa(Long userId, Chapter chapter) {
        for (int i = 0; i < 60; i++) {
            try {
                ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(chapter.getId());
                chapter = mangaUtil.getChapterByDto(chapterDto);
                Thread.sleep(1000);
                if (chapter.getPdfStatusDownload().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(chapter.getId());
        chapter = mangaUtil.getChapterByDto(chapterDto);

        if (chapter.getPdfStatusDownload().equals("process")) {
            mangaChapterRepository.setPdfStatusDownload(null, chapter.getId());
            Integer messageId = serviceConfig.mangaServices().get(chapter.getCatalogName()).createPdfChapter(userId, chapter);
            sendChapterToUser(messageId, chapter, userId);
        } else if (chapter.getPdfStatusDownload().equals("finished")) {
            sendChapterToUser(chapter.getPdfMessageId(), chapter, userId);
        }
    }

    public void waitForUploadManga(Long userId, Chapter chapter) {
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1000);
                ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(chapter.getId());
                chapter = mangaUtil.getChapterByDto(chapterDto);

                if (chapter.getTelegraphStatusDownload().equals("finished")) {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(chapter.getId());
        chapter = mangaUtil.getChapterByDto(chapterDto);

        //chapter = mangaChapterRepository.findById(chapter.getId()).get();
        if (chapter.getTelegraphStatusDownload().equals("process")) {
            mangaChapterRepository.setTelegraphStatusDownload(null, chapter.getId());
            serviceConfig.mangaServices().get(chapter.getCatalogName()).createTelegraphArticleChapter(userId, chapter);
        } else if (chapter.getTelegraphStatusDownload().equals("finished")) {
            sendCopyMessageMangaFromMangaStorage(userId, chapter);
        }
    }


    private void sendCopyMessageMangaFromMangaStorage(Long userId, Chapter chapter) {
        CopyMessage copyMessage = new CopyMessage(String.valueOf(userId), "-1002092468371L", chapter.getTelegraphMessageId());
        InlineKeyboardMarkup inlineKeyboardMarkup = serviceConfig.mangaServices().get(chapter.getCatalogName()).getPrevNextButtons(chapter, userId);
        if (inlineKeyboardMarkup != null) {
            copyMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        try {
            telegramSender.resendCopyMessageFromStorage(copyMessage);
        } catch (ExecutionException | InterruptedException e) {
            mangaChapterRepository.setTelegraphStatusDownload(null, chapter.getId());
            log.error("Copy message not send: " + chapter.getName() + " chapterId " + chapter.getChapterId() + " vol " + chapter.getVol() + " ch " + chapter.getChapter());
            e.printStackTrace();
            serviceConfig.mangaServices().get(chapter.getCatalogName()).createTelegraphArticleChapter(userId, chapter);
        }
    }

    private void sendCopyMessageManhwaFromMangaStorage(Long userId, Chapter chapter) {
        CopyMessage copyMessage = new CopyMessage(String.valueOf(userId), "-1002092468371L", chapter.getPdfMessageId());
        InlineKeyboardMarkup inlineKeyboardMarkup = serviceConfig.mangaServices().get(chapter.getCatalogName()).getPrevNextButtons(chapter, userId);
        if (inlineKeyboardMarkup != null) {
            copyMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        try {
            telegramSender.resendCopyMessageFromStorage(copyMessage);
        } catch (ExecutionException | InterruptedException e) {
            mangaChapterRepository.setPdfStatusDownload(null, chapter.getId());
            log.error("Copy message not send: " + chapter.getName() + " chapterId " + chapter.getChapterId() + " vol " + chapter.getVol() + " ch " + chapter.getChapter());
            e.printStackTrace();
            serviceConfig.mangaServices().get(chapter.getCatalogName()).createPdfChapter(userId, chapter);
        }
    }


    public void clickReadStatus(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();

        ChapterDto chapterDto = mangaChapterRepository.findChapterDtoById(Long.valueOf(util.parseValue(callbackQuery.getData())[2]));
        Chapter chapter = mangaUtil.getChapterByDto(chapterDto);

        //Chapter chapter = mangaChapterRepository.findById(Long.valueOf(util.parseValue(callbackQuery.getData())[2])).orElseThrow();
        ReadStatus readStatus = readStatusRepository.findByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, chapter.getCatalogName());

        if (readStatus == null) {
            readStatusRepository.save(new ReadStatus(chapter.getMangaId(), chapter.getChapterId(), userId, new Timestamp(System.currentTimeMillis()), chapter.getCatalogName()));
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Главу отмечено как \"Прочитано\"")
                    .showAlert(false).build());
        } else {
            readStatusRepository.delete(readStatus);
        }

        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(serviceConfig.mangaServices().get(chapter.getCatalogName()).getPrevNextButtons(chapter, userId))
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId).build());
    }

    public void clickChangeMangaStatus(CallbackQuery callbackQuery) {
        String catalogName = util.parseValue(callbackQuery.getData())[0];
        String mangaDatabaseId = util.parseValue(callbackQuery.getData())[2];
        String mangaId = util.parseValue(callbackQuery.getData())[3];
        Long userId = callbackQuery.getFrom().getId();

        MangaStatusParameter mangaStatusParameter = null;
        if (mangaDatabaseId != null) {
            mangaStatusParameter = mangaStatusParameterRepository.findByMangaDatabaseIdAndUserId(Long.valueOf(mangaDatabaseId), userId);
        }

        if (mangaStatusParameter == null && catalogName.equals("desu.me")) {
            mangaStatusParameter = mangaStatusParameterRepository.findByMangaIdAndUserIdAndCatalogName(mangaId, userId, catalogName);
            if (mangaStatusParameter != null && mangaDatabaseId != null) {
                mangaStatusParameter.setMangaDatabaseId(Long.valueOf(mangaDatabaseId));
                mangaStatusParameterRepository.save(mangaStatusParameter);
            }
        }

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
                .replyMarkup(getKeyboardForChangeStatus(read, planned, finished, postponed, mangaDatabaseId, catalogName)).build());
    }


    public void clickMangaStatus(CallbackQuery callbackQuery) {
        String mangaDatabaseId = util.parseValue(callbackQuery.getData())[2];
        Long userId = callbackQuery.getFrom().getId();
        String parameter = util.parseValue(callbackQuery.getData())[3];

        Manga manga = mangaRepository.findById(Long.valueOf(mangaDatabaseId)).orElseThrow();

        String read = "Читаю";
        String planned = "В планах";
        String finished = "Прочитано";
        String postponed = "Отложено";

        MangaStatusParameter mangaStatusParameter = mangaStatusParameterRepository.findByMangaDatabaseIdAndUserId(Long.valueOf(mangaDatabaseId), userId);

        if (mangaStatusParameter == null) {
            mangaStatusParameter = new MangaStatusParameter(manga.getMangaId(), Long.valueOf(mangaDatabaseId), userId, parameter, manga.getName(), null, new Timestamp(System.currentTimeMillis()), manga.getCatalogName());

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
                .replyMarkup(getKeyboardForChangeStatus(read, planned, finished, postponed, mangaDatabaseId, manga.getCatalogName())).build());
    }


    private InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, String mangaDatabaseId, String catalog) {
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(read)).callbackData(catalog + "\nchangeMangaStatusRead\n" + mangaDatabaseId + "\nread").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(planned)).callbackData(catalog + "\nchangeMangaStatusPlanned\n" + mangaDatabaseId + "\nplanned").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(finished)).callbackData(catalog + "\nchangeMangaStatusFinished\n" + mangaDatabaseId + "\nfinished").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(postponed)).callbackData(catalog + "\nchangeMangaStatusPostponed\n" + mangaDatabaseId + "\npostponed").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData(catalog + "\nclickBackManga\n" + mangaDatabaseId).build())
        )));
    }

    public void clickBackManga(CallbackQuery callbackQuery) {
        Long mangaDatabaseId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        Manga manga = mangaRepository.findById(mangaDatabaseId).orElseThrow();
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(serviceConfig.mangaServices().get(manga.getCatalogName()).getMangaButtons(new MangaButtonData(callbackQuery.getFrom().getId(), manga.getMangaId(), mangaDatabaseId, manga.getLanguageCode())))
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());
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

    public void sendChapterToUser(Integer messageIdChapterInStorage, Chapter chapter, Long userId) {
        if (messageIdChapterInStorage != null) {
            InlineKeyboardMarkup inlineKeyboardMarkup = serviceConfig.mangaServices().get(chapter.getCatalogName()).getPrevNextButtons(chapter, userId);
            telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                    .messageId(messageIdChapterInStorage)
                    .replyMarkup(inlineKeyboardMarkup)
                    .chatId(userId)
                    .fromChatId(-1002092468371L).build());
        }
    }

    public void doBackup() {
//        ArrayList<Chapter> copyMessageMangas = mangaChapterRepository.findAllByBackupMessageIdIsNull();
//        for (Chapter copyMessageManga : copyMessageMangas) {
//            if (copyMessageManga.getMessageId() == null) {
//                continue;
//            }
//            try {
//                Thread.sleep(15000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            MessageId messageId = telegramSender.sendCopyMessage(CopyMessage.builder()
//                    .messageId(copyMessageManga.getMessageId())
//                    .fromChatId(-1002092468371L)
//                    .chatId(-1002119024676L).build());
//            if (messageId != null) {
//                copyMessageManga.setBackupMessageId(Math.toIntExact(messageId.getMessageId()));
//                mangaChapterRepository.save(copyMessageManga);
//            }
//        }
    }


}
