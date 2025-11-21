package com.suttori.service;

import com.suttori.dao.StatisticEntityRepository;
import com.suttori.dao.UserRepository;
import com.suttori.dto.UserChapterStatisticsDTO;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RatingService {

    private TelegramSender telegramSender;
    private StatisticEntityRepository statisticEntityRepository;
    private UserRepository userRepository;
    private Util util;

    @Autowired
    public RatingService(TelegramSender telegramSender, StatisticEntityRepository statisticEntityRepository, UserRepository userRepository, Util util) {
        this.telegramSender = telegramSender;
        this.statisticEntityRepository = statisticEntityRepository;
        this.userRepository = userRepository;
        this.util = util;
    }

    public void clickReadersRating(Long userId) {
        telegramSender.send(SendMessage.builder()
                .text("Выбери какой рейтинг ты хочешь посмотреть")
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("По загрузкам глав за все время")).callbackData("getStatAboutAllTimeDownloadChapters").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("По загрузкам глав за сутки")).callbackData("getStatAboutDailyDownloadChapters").build())
//                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Рейтинг подборок")).callbackData("clickSetSortFilterParams").build()),
                )))).build());
    }

    public void getRatingAboutChapterDownload(CallbackQuery callbackQuery) {
        List<UserChapterStatisticsDTO> results;
        if (callbackQuery.getData().equals("getStatAboutAllTimeDownloadChapters")) {
            results = statisticEntityRepository.findUserChapterStatisticsDto();
        } else if (callbackQuery.getData().equals("getStatAboutDailyDownloadChapters")) {
            results = statisticEntityRepository.findUserChapterStatisticsToday(Timestamp.valueOf(LocalDateTime.now().toLocalDate().atStartOfDay()));
        } else {
            return;
        }
        getStatAboutDownloadChapters(callbackQuery, results);
    }

    private void getStatAboutDownloadChapters(CallbackQuery callbackQuery, List<UserChapterStatisticsDTO> results) {
        StringBuilder stringBuilder = new StringBuilder("Рейтинг пользователей по загрузкам глав:\n\n");
        results.sort(Comparator.comparing(UserChapterStatisticsDTO::getTotalChapters).reversed());
        List<MessageEntity> messageEntities = new ArrayList<>();
        boolean flag = false;
        for (int i = 0; i < results.size(); i++) {
            if (flag) {
                getUserRow(results, i, stringBuilder, messageEntities);
            }
            if (i > 19 && !results.get(i).getUserId().equals(callbackQuery.getFrom().getId())) {
                continue;
            }
            if (i > 19 && results.get(i).getUserId().equals(callbackQuery.getFrom().getId())) {
                stringBuilder.append("...\n");
            }

            getUserRow(results, i, stringBuilder, messageEntities);

            if (i > 19 && results.get(i).getUserId().equals(callbackQuery.getFrom().getId())) {
                flag = true;
                i = results.size() - 4;
                stringBuilder.append("...\n");
            }
        }

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text(stringBuilder.toString())
                .chatId(callbackQuery.getFrom().getId())
                .entities(messageEntities)
                .messageId(callbackQuery.getMessage().getMessageId())
                .disableWebPagePreview(true).build());
    }

    private void getUserRow(List<UserChapterStatisticsDTO> results, int i, StringBuilder stringBuilder, List<MessageEntity> messageEntities) {
        User user = userRepository.findByUserId(results.get(i).getUserId());
        String userFirstLastName = user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "");
        stringBuilder.append(i + 1).append(". ");
        if ((user.getPrivateSettings() != null && !user.getPrivateSettings().equals("RESTRICT") && user.getUserName() != null) || (user.getPrivateSettings() == null && user.getUserName() != null)) {
            messageEntities.add(util.getUrlEntity(user.getUserName(), stringBuilder.length(), userFirstLastName.length()));
        }
        stringBuilder.append(userFirstLastName).append(" - ").append(results.get(i).getTotalChapters()).append("\n");
    }
}
