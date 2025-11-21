package com.suttori.service;

import com.suttori.dao.*;
import com.suttori.entity.*;
import com.suttori.entity.Enums.UserStatus;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.MangaUtil;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ProfileService {

    private TelegramSender telegramSender;
    private Util util;
    private MangaStatusParameterRepository mangaStatusParameterRepository;
    private UserRepository userRepository;
    private HistoryEntityRepository historyEntityRepository;
    private FriendEntityRepository friendEntityRepository;
    private StatisticEntityRepository statisticEntityRepository;
    private MangaRepository mangaRepository;
    private MangaUtil mangaUtil;

    @Autowired
    public ProfileService(TelegramSender telegramSender, Util util,
                          MangaStatusParameterRepository mangaStatusParameterRepository, UserRepository userRepository,
                          HistoryEntityRepository historyEntityRepository, FriendEntityRepository friendEntityRepository,
                          StatisticEntityRepository statisticEntityRepository, MangaRepository mangaRepository, MangaUtil mangaUtil) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
        this.userRepository = userRepository;
        this.historyEntityRepository = historyEntityRepository;
        this.friendEntityRepository = friendEntityRepository;
        this.statisticEntityRepository = statisticEntityRepository;
        this.mangaRepository = mangaRepository;
        this.mangaUtil = mangaUtil;
    }

    public void clickProfile(Message message) {
        mangaUtil.checkDuplicateMangaStatusParameter(message.getFrom().getId());
        userRepository.setPosition("DEFAULT_POSITION", message.getFrom().getId());
        telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(getPhotoFieldId(message.getFrom().getId(), "AgACAgIAAxkBAAIHS2XPgbBhPyaF8R5oxtmOPPXHPLvTAAKY4jEbIRZ4Sv0mR_QE3jErAQADAgADcwADNAQ")))
                .caption(getTextForUserProfile(message.getFrom().getId()))
                .chatId(message.getFrom().getId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("История")).switchInlineQueryCurrentChat("history").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Друзья")).callbackData("clickMyFriend\n" + 1).build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Приватность")).callbackData("clickPrivateSetting").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Чат обсуждения")).url("https://t.me/manga_reader_chat").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Мои закладки")).callbackData("clickMyFavorites").build())))))
                .parseMode("HTML").build());
    }

    public void clickBackToProfile(CallbackQuery callbackQuery) {
        Integer messageId = telegramSender.sendEditMessageMedia(EditMessageMedia.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .media(new InputMediaPhoto(getPhotoFieldId(callbackQuery.getFrom().getId(), "AgACAgIAAxkBAAIHS2XPgbBhPyaF8R5oxtmOPPXHPLvTAAKY4jEbIRZ4Sv0mR_QE3jErAQADAgADcwADNAQ")))
                .chatId(callbackQuery.getFrom().getId()).build()).getMessageId();

        telegramSender.sendEditMessageCaption(EditMessageCaption.builder()
                .caption(getTextForUserProfile(callbackQuery.getFrom().getId()))
                .messageId(messageId)
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("История")).switchInlineQueryCurrentChat("history").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Друзья")).callbackData("clickMyFriend\n" + 1).build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Приватность")).callbackData("clickPrivateSetting").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Чат обсуждения")).url("https://t.me/manga_reader_chat").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Мои закладки")).callbackData("clickMyFavorites").build())))))
                .chatId(callbackQuery.getFrom().getId())
                .parseMode("HTML").build());
    }

    public void privateSettings(CallbackQuery callbackQuery, User user) {
        String privateSettingsText;
        String privateSettingsButtonText;
        if (user.getPrivateSettings() == null || user.getPrivateSettings().equals("ALL")) {
            privateSettingsText = "Сейчас твой профиль виден (если у тебя есть @юзернейм в телеграм)";
            privateSettingsButtonText = "Закрыть профиль";
        } else {
            privateSettingsText = "Сейчас твой профиль закрыт и отображается только твое имя";
            privateSettingsButtonText = "Открыть профиль";
        }

        telegramSender.sendEditMessageCaption(EditMessageCaption.builder()
                .caption("Здесь ты можешь изменить настройки приватности в боте. \n\nСсылку на твой аккаунт в телеграм могут увидеть в статистике бота, он будет кликабельным как " + user.getFirstName() + (user.getLastName() != null ? user.getLastName() : "") + ". Если отключить эту функцию, то в статистиках будет отображаться только твое имя. Если у тебя нет @юзернейм в телеграм, то в любом случае будет отображаться только твое имя. Эти настройки не касаются тех, кого ты добавил в друзья.\n\n" + privateSettingsText)
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(privateSettingsButtonText)).callbackData("setPrivateSettings").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickBackToProfile").build())))))
                .chatId(callbackQuery.getFrom().getId())
                .captionEntity(MessageEntity.builder()
                        .length((user.getFirstName() + (user.getLastName() != null ? user.getLastName() : "")).length())
                        .url("tg://settings")
                        .offset(150)
                        .type("text_link").build()).build());
    }

    public void clickSetPrivateSettings(CallbackQuery callbackQuery, User user) {
        if (user.getPrivateSettings() == null || user.getPrivateSettings().equals("ALL")) {
            user.setPrivateSettings("RESTRICT");
            userRepository.setPrivateSettings("RESTRICT", user.getUserId());
        } else {
            user.setPrivateSettings("ALL");
            userRepository.setPrivateSettings("ALL", user.getUserId());
        }
        privateSettings(callbackQuery, user);
    }

    public String getTextForUserProfile(Long userId) {
        User user = userRepository.findByUserId(userId);
        int downloadsSize = statisticEntityRepository.findAllByUserId(userId).size();

        return "Имя: " + user.getFirstName() +
                "\n\nСтатус: " + getStatusBotUser(downloadsSize) +
                "\n\nСтатистика:" +
                "\nЧитаю: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "read").size() +
                "\nВ планах: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "planned").size() +
                "\nПрочитано: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "finished").size() +
                "\nОтложено: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "postponed").size() +
                "\n\nЗагружено глав: " + downloadsSize;
    }

    public String getTextForFriendProfile(Long userId) {
        User user = userRepository.findByUserId(userId);
        int downloadsSize = statisticEntityRepository.findAllByUserId(userId).size();
        return "Имя: " + user.getFirstName() +
                "\n\nСтатус: " + getStatusBotUser(downloadsSize) +
                "\n\nСтатистика:" +
                "\nЧитает: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "read").size() +
                "\nВ планах: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "planned").size() +
                "\nПрочитано: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "finished").size() +
                "\nОтложено: " + mangaStatusParameterRepository.findAllByUserIdAndStatus(userId, "postponed").size() +
                "\n\nЗагружено глав: " + downloadsSize;
    }

    public String getStatusBotUser(int downloadsSize) {
        if (downloadsSize < 100) {
            return UserStatus.NEWBIE.getStatusName();
        } else if (downloadsSize < 500) {
            return UserStatus.STUDENT.getStatusName();
        } else if (downloadsSize < 1000) {
            return UserStatus.ADVANCED.getStatusName();
        } else if (downloadsSize < 1500) {
            return UserStatus.EXPERT.getStatusName();
        } else if (downloadsSize < 3000) {
            return UserStatus.GURU.getStatusName();
        } else if (downloadsSize < 4000) {
            return UserStatus.CHAMPION.getStatusName();
        } else if (downloadsSize < 5000) {
            return UserStatus.IDEAL.getStatusName();
        } else if (downloadsSize < 7000) {
            return UserStatus.LEGEND.getStatusName();
        } else if (downloadsSize < 10000) {
            return UserStatus.DEITY.getStatusName();
        } else {
            return null;
        }
    }

    public void clickMyFavorites(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();

        Integer messageId = callbackQuery.getMessage().getMessageId();
        if (callbackQuery.getData().contains("clickMyFavoritesViaFavorites")) {
            messageId = telegramSender.sendEditMessageMedia(EditMessageMedia.builder()
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .media(new InputMediaPhoto(getPhotoFieldId(userId, "AgACAgIAAxkBAAIHS2XPgbBhPyaF8R5oxtmOPPXHPLvTAAKY4jEbIRZ4Sv0mR_QE3jErAQADAgADcwADNAQ")))
                    .chatId(callbackQuery.getFrom().getId()).build()).getMessageId();
        }

        telegramSender.sendEditMessageCaption(EditMessageCaption.builder()
                .caption(getTextForUserProfile(callbackQuery.getFrom().getId()))
                .messageId(messageId)
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Читаю")).switchInlineQueryCurrentChat("read").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В планах")).switchInlineQueryCurrentChat("planned").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Прочитано")).switchInlineQueryCurrentChat("finished").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Отложено")).switchInlineQueryCurrentChat("postponed").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickBackToProfile").build())
                )))).build());
    }

    public void clickHistory(InlineQuery inlineQuery) {
        int offset = 30;
        if (!inlineQuery.getOffset().isEmpty()) {
            offset = Integer.parseInt(inlineQuery.getOffset());
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "updateAt");
        Pageable pageable = PageRequest.of(offset / 30 - 1, 30, sort);

        List<HistoryEntity> historyEntities = historyEntityRepository.findAllByUserId(inlineQuery.getFrom().getId(), pageable);
        if (offset == 30 && historyEntities.isEmpty()) {
            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(Collections.singletonList(InlineQueryResultArticle.builder()
                            .id(String.valueOf(inlineQuery.getFrom().getId()))
                            .title("Ой...")
                            .description("Твоя история прочтения пуста, нажми \"Поиск\" чтобы начать искать мангу")
                            .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                            .inputMessageContent(new InputTextMessageContent("Поиск")).build()))
                    .nextOffset(String.valueOf(offset + 30))
                    .inlineQueryId(inlineQuery.getId()).build());
            return;
        }

        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (HistoryEntity historyEntity : historyEntities) {
            if (historyEntity.getMangaDatabaseId() != null) {
                Manga manga = mangaRepository.findById(historyEntity.getMangaDatabaseId()).orElseThrow();
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(manga.getName())
                        .description(manga.getReleaseDate() +
                                " | Формат: " + manga.getType() + "\n" +
                                "Статус: " + manga.getStatus() + "\n" +
                                "Жанр: " + manga.getGenres())
                        .thumbnailUrl(manga.getCoverUrl() != null ? manga.getCoverUrl() : "https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                        .inputMessageContent(new InputTextMessageContent(manga.getCatalogName() + "\nmangaDatabaseId\n" + manga.getId())).build());
            } else {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(historyEntity.getName() != null ? historyEntity.getName() : historyEntity.getRussian())
                        .description("Просмотрено: " + dateFormat.format(historyEntity.getUpdateAt()))
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                        .inputMessageContent(new InputTextMessageContent(historyEntity.getCatalogName() + "\nmangaId\n" + historyEntity.getMangaId())).build());
            }
        }

        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .nextOffset(String.valueOf(offset + 30))
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public void getMangaByStatus(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();

        int offset = 30;
        if (!inlineQuery.getOffset().isEmpty()) {
            offset = Integer.parseInt(inlineQuery.getOffset());
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "addedAt");
        Pageable pageable = PageRequest.of(offset / 30 - 1, 30, sort);

        List<MangaStatusParameter> statusParameterList = mangaStatusParameterRepository.findAllByUserIdAndStatus(inlineQuery.getFrom().getId(), query, pageable);

        if (offset == 30 && statusParameterList.isEmpty()) {
            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(Collections.singletonList(InlineQueryResultArticle.builder()
                            .id(String.valueOf(inlineQuery.getFrom().getId()))
                            .description("В этой категории у тебя нет добавленной манги")
                            .title("Ой...")
                            .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                            .inputMessageContent(new InputTextMessageContent("Поиск")).build()))
                    .nextOffset(String.valueOf(offset + 30))
                    .inlineQueryId(inlineQuery.getId()).build());
            return;
        }
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (MangaStatusParameter mangaStatusParameter : statusParameterList) {
            if (mangaStatusParameter.getMangaDatabaseId() != null) {
                Manga manga = mangaRepository.findById(mangaStatusParameter.getMangaDatabaseId()).orElseThrow();
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(manga.getName())
                        .description(manga.getReleaseDate() +
                                " | Формат: " + manga.getType() + "\n" +
                                "Статус: " + manga.getStatus() + "\n" +
                                "Жанр: " + manga.getGenres())
                        .thumbnailUrl(manga.getCoverUrl() != null ? manga.getCoverUrl() : "https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                        .inputMessageContent(new InputTextMessageContent(manga.getCatalogName() + "\nmangaDatabaseId\n" + manga.getId())).build());
            } else {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(mangaStatusParameter.getName() != null ? mangaStatusParameter.getName() : mangaStatusParameter.getRussian())
                        .description("Добавлено: " + dateFormat.format(mangaStatusParameter.getAddedAt()))
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                        .inputMessageContent(new InputTextMessageContent(mangaStatusParameter.getCatalogName() + "\nmangaId\n" + mangaStatusParameter.getMangaId())).build());
            }
        }

        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .nextOffset(String.valueOf(offset + 30))
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public void clickMyFriend(CallbackQuery callbackQuery) {
        InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsFriendList(callbackQuery);
        String caption;
        Integer messageId;
        if (inlineKeyboardMarkup == null) {
            caption = "У тебя нет друзей( Скорее исправь это! \n\nВот твоя пригласительная ссылка для друзей: <code>https://t.me/MangaManhwa_bot?start=addFriend" + callbackQuery.getFrom().getId() + "</code>";
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickBackToProfile").build())
            )));
        } else {
            caption = "Ниже список твоих друзей, а выше ваше совместное фото. Читайте мангу вместе, делитесь впечатлениями в чате для обсуждений, следите за статистикой друг друга!\n\nВот твоя пригласительная ссылка для друзей: <code>https://t.me/MangaManhwa_bot?start=addFriend" + callbackQuery.getFrom().getId() + "</code>";
        }

        messageId = telegramSender.sendEditMessageMedia(EditMessageMedia.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .media(new InputMediaPhoto("AgACAgIAAxkBAAIHQmXPfhkKJ2xJCZQSpmm-_pWZotO9AAJK1jEb7Q6AStnnW0VbtY-IAQADAgADcwADNAQ"))
                .chatId(callbackQuery.getFrom().getId()).build()).getMessageId();

        telegramSender.sendEditMessageCaption(EditMessageCaption.builder()
                .chatId(callbackQuery.getFrom().getId())
                .messageId(messageId)
                .parseMode("HTML")
                .replyMarkup(inlineKeyboardMarkup)
                .caption(caption).build());
    }

    public InlineKeyboardMarkup getButtonsFriendList(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        int currentPage = Integer.parseInt(util.parseValue(callbackQuery.getData())[1]);

        List<FriendEntity> friendList = friendEntityRepository.findAllByUserId(userId);
        if (friendList.isEmpty()) {
            return null;
        }
        friendList.sort(Comparator.comparing(FriendEntity::getAddedAt).reversed());

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();

        int friendPerPage = 10;
        int startIndex = currentPage * friendPerPage - friendPerPage;
        int endIndex = Math.min(startIndex + friendPerPage, friendList.size());

        for (int i = startIndex; i < endIndex; i++) {
            User friend = userRepository.findByUserId(friendList.get(i).getFriendId());
            String buttonText = String.valueOf(friend.getFirstName());
            row.add(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(buttonText)).callbackData("getInfoAboutFriend\n" + friend.getUserId()).build());
            keyboard.add(row);
            row = new InlineKeyboardRow();
        }

        InlineKeyboardRow paginationRow = new InlineKeyboardRow();
        if (currentPage > 1) {
            var previousButton = new InlineKeyboardButton("<< Назад");
            previousButton.setCallbackData("click_previous_page_friend_page\n" + (currentPage - 1));
            paginationRow.add(previousButton);
        }
        if (endIndex < friendList.size()) {
            var nextButton = new InlineKeyboardButton("Дальше >>");
            nextButton.setCallbackData("click_next_page_friend_page\n" + (currentPage + 1));
            paginationRow.add(nextButton);
        }
        if (!paginationRow.isEmpty()) {
            keyboard.add(paginationRow);
        }

        keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickBackToProfile").build()));
        return new InlineKeyboardMarkup(keyboard);
    }

    public void getInfoAboutFriend(CallbackQuery callbackQuery) {
        Integer messageId = telegramSender.sendEditMessageMedia(EditMessageMedia.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .media(new InputMediaPhoto(getPhotoFieldId(Long.valueOf(util.parseValue(callbackQuery.getData())[1]), "AgACAgIAAxkBAAIHS2XPgbBhPyaF8R5oxtmOPPXHPLvTAAKY4jEbIRZ4Sv0mR_QE3jErAQADAgADcwADNAQ")))
                .chatId(callbackQuery.getFrom().getId()).build()).getMessageId();

        telegramSender.sendEditMessageCaption(EditMessageCaption.builder()
                .caption(getTextForFriendProfile(Long.valueOf(util.parseValue(callbackQuery.getData())[1])))
                .messageId(messageId)
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickMyFriendBack\n" + 1).build()
                        )))))
                .chatId(callbackQuery.getFrom().getId())
                .parseMode("HTML").build());
    }

    public String getPhotoFieldId(Long userId, String fieldId) {
        UserProfilePhotos userProfilePhotos = telegramSender.getUserProfilePhotos(new GetUserProfilePhotos(userId));
        if (!userProfilePhotos.getPhotos().isEmpty()) {
            return userProfilePhotos.getPhotos().get(0).stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow().getFileId();
        } else {
            return fieldId;
        }
    }


}
