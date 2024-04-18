package com.suttori.service;

import com.suttori.dao.*;
import com.suttori.entity.Enums.UserStatus;
import com.suttori.entity.FriendEntity;
import com.suttori.entity.HistoryEntity;
import com.suttori.entity.MangaDesu.MangaData;
import com.suttori.entity.MangaStatusParameter;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramSender;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ProfileService {

    private TelegramSender telegramSender;
    private Util util;
    private MangaService mangaService;
    private MangaStatusParameterRepository mangaStatusParameterRepository;
    private UserRepository userRepository;
    private HistoryEntityRepository historyEntityRepository;
    private FriendEntityRepository friendEntityRepository;
    private StatisticEntityRepository statisticEntityRepository;

    @Autowired
    public ProfileService(TelegramSender telegramSender, Util util, MangaService mangaService,
                          MangaStatusParameterRepository mangaStatusParameterRepository, UserRepository userRepository,
                          HistoryEntityRepository historyEntityRepository, FriendEntityRepository friendEntityRepository,
                          StatisticEntityRepository statisticEntityRepository) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaService = mangaService;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
        this.userRepository = userRepository;
        this.historyEntityRepository = historyEntityRepository;
        this.friendEntityRepository = friendEntityRepository;
        this.statisticEntityRepository = statisticEntityRepository;
    }

    public void clickProfile(Message message) {
        telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(getPhotoFieldId(message.getFrom().getId(), "AgACAgIAAxkBAAIHS2XPgbBhPyaF8R5oxtmOPPXHPLvTAAKY4jEbIRZ4Sv0mR_QE3jErAQADAgADcwADNAQ")))
                .caption(getTextForUserProfile(message.getFrom().getId()))
                .chatId(message.getFrom().getId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("История")).switchInlineQueryCurrentChat("history").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Друзья")).callbackData("clickMyFriend\n" + 1).build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Чат обсуждения")).url("https://t.me/manga_reader_chat").build()),
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
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Чат обсуждения")).url("https://t.me/manga_reader_chat").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Мои закладки")).callbackData("clickMyFavorites").build())))))
                .chatId(callbackQuery.getFrom().getId())
                .parseMode("HTML").build());
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
        } else if (downloadsSize < 1500) {
            return UserStatus.ADVANCED.getStatusName();
        } else if (downloadsSize < 4500) {
            return UserStatus.EXPERT.getStatusName();
        } else if (downloadsSize < 10000) {
            return UserStatus.GURU.getStatusName();
        } else if (downloadsSize < 20000) {
            return UserStatus.CHAMPION.getStatusName();
        } else if (downloadsSize < 30000) {
            return UserStatus.IDEAL.getStatusName();
        } else if (downloadsSize < 50000) {
            return UserStatus.LEGEND.getStatusName();
        } else if (downloadsSize < 100000) {
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
                .caption("Мои закладки:")
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
        if (historyEntities.isEmpty()) {
            return;
        }
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        for (HistoryEntity historyEntity : historyEntities) {
            MangaData mangaData = mangaService.getMangaData(historyEntity.getMangaId());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Рейтинг: ").append(mangaData.getScore());
            stringBuilder.append(" | Год: ").append(new SimpleDateFormat("yyyy").format(new Date(mangaData.getAired_on() * 1000))).append(" | Тип: ").append(mangaData.getKind()).append("\n");
            stringBuilder.append("Статус: ").append(util.getStatus(mangaData.getStatus())).append("\n");
            stringBuilder.append("Жанр: ").append(util.getGenres(mangaData.getGenres()));
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(mangaData.getRussian())
                    .description(stringBuilder.toString())
                    .thumbnailUrl(mangaData.getImage().getOriginal())
                    .inputMessageContent(new InputTextMessageContent("mangaId\n" + mangaData.getId())).build());
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
        if (statusParameterList.isEmpty()) {
            return;
        }
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        for (MangaStatusParameter mangaStatusParameter : statusParameterList) {
            MangaData mangaData = mangaService.getMangaData(mangaStatusParameter.getMangaId());

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
                    .inputMessageContent(new InputTextMessageContent("mangaId\n" + mangaData.getId())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .nextOffset(String.valueOf(offset + 30))
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public void sendMangaViaProfile(CallbackQuery callbackQuery) {
        Long mangaId = Long.valueOf(util.parseValue(callbackQuery.getData())[1]);
        Long userId = callbackQuery.getFrom().getId();

        InlineKeyboardMarkup inlineKeyboardMarkup = mangaService.getMangaButtonsViaProfile(callbackQuery.getData(), mangaId, userId);

        if (callbackQuery.getData().contains("sendMangaViaFavorites")) {
            inlineKeyboardMarkup.getKeyboard().add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickMyFavoritesViaFavorites").build()));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("clickBackToHistory\n" + 1).build()));
        }

        MangaData mangaData = mangaService.getMangaData(mangaId);
        Message message = telegramSender.sendEditMessageMedia(EditMessageMedia.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .media(new InputMediaPhoto(mangaData.getImage().getOriginal()))
                .chatId(userId).build());

        telegramSender.sendEditMessageCaption(EditMessageCaption.builder()
                .chatId(userId)
                .messageId(message.getMessageId())
                .parseMode("HTML")
                .replyMarkup(inlineKeyboardMarkup)
                .caption(mangaService.getMangaText(mangaData)).build());
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
