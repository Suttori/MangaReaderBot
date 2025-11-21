package com.suttori.service;

import com.suttori.dao.FriendEntityRepository;
import com.suttori.dao.UserRepository;
import com.suttori.entity.FriendEntity;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.sql.Timestamp;

@Service
public class UserService {


    private TelegramSender telegramSender;

    @Autowired
    public UserService(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendEntityRepository friendEntityRepository;

    public void save(Message message) {
        if (message.getText().contains("addFriend")) {
            String str = message.getText();
            int index = str.indexOf("addFriend") + "addFriend".length();
            Long result = Long.valueOf(str.substring(index));

            if (!result.equals(message.getFrom().getId()) && friendEntityRepository.findByUserIdAndFriendId(result, message.getFrom().getId()) == null && friendEntityRepository.findByUserIdAndFriendId(message.getFrom().getId(), result) == null) {
                friendEntityRepository.save(new FriendEntity(message.getFrom().getId(), result, new Timestamp(System.currentTimeMillis())));
                friendEntityRepository.save(new FriendEntity(result, message.getFrom().getId(), new Timestamp(System.currentTimeMillis())));
            }
        }

        if (userRepository.findByUserId(message.getFrom().getId()) != null) {
            userRepository.setPosition("START", message.getChatId());
            return;
        }

        String referral;
        if (message.getText().equals("/start")) {
            referral = null;
        } else if (message.getText().contains("/start")) {
            referral = message.getText().split("\\s")[1];
        } else {
            referral = null;
        }

        User user = new User(message.getFrom().getId(), message.getChatId(), message.getFrom().getFirstName(), message.getFrom().getLastName(),
                message.getFrom().getUserName(), false, message.getFrom().getIsPremium(), message.getFrom().getLanguageCode(),
                new Timestamp(System.currentTimeMillis()), referral, false, "START", 0, "desu.me");
        userRepository.save(user);
    }
    
    public void upsertUser(Message message) {
        userRepository.upsertUser(message.getFrom().getId(),
                message.getChatId(),
                message.getFrom().getFirstName(),
                message.getFrom().getLastName(),
                message.getFrom().getUserName(),
                message.getFrom().getLanguageCode(),
                message.getFrom().getIsPremium());
    }

    public boolean checkArabUser(Message message) {
        String code = "ar tr fa";
        return !code.contains(message.getFrom().getLanguageCode());
    }

    public void sendNoAccess(Message message) {
        telegramSender.send(SendMessage.builder()
                .text("The bot is not available in your region")
                .chatId(message.getFrom().getId()).build());
    }

    public void setPosition(Long userId, String position) {
        userRepository.setPosition(position, userId);
    }

    public User getUser(Message message) {
        return userRepository.findByUserId(message.getFrom().getId());
    }

    public User getUser(CallbackQuery callbackQuery) {
        return userRepository.findByUserId(callbackQuery.getFrom().getId());
    }

    public User getUser(InlineQuery inlineQuery) {
        return userRepository.findUserByChatId(inlineQuery.getFrom().getId());
    }

    public User getUser(Long userId) {
        return userRepository.findByUserId(userId);
    }

    public String getLocale(Long userId) {
        String locale = userRepository.findByUserId(userId).getLanguageCode();
        if (locale.equals("uk")) {
            locale = "uk";
        } else if (locale.equals("ru") || locale.equals("uz") || locale.equals("az") || locale.equals("be") || locale.equals("ka-ge") || locale.equals("kk")) {
            locale = "ru";
        } else {
            locale = "en";
        }
        return locale;
    }

    public void setLastActivity(Long userId) {
        userRepository.setLastActivity(new Timestamp(System.currentTimeMillis()), userId);
    }

    public void setSortParam(String param, Long userId) {
        userRepository.setSortParam(param, userId);
    }
}
