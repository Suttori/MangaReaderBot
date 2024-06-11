package com.suttori.service;


import com.suttori.dao.ReferralChannelRepository;
import com.suttori.dao.UserRepository;
import com.suttori.entity.ReferralChannel;
import com.suttori.telegram.TelegramSender;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReferralService {
    private TelegramSender telegramSender;
    private LocaleService localeService;

    @Autowired
    ReferralChannelRepository referralChannelRepository;
    @Autowired
    UserRepository userRepository;

    public ReferralService(TelegramSender telegramSender, LocaleService localeService) {
        this.telegramSender = telegramSender;
        this.localeService = localeService;
    }

    public boolean checkAccess(Message message) {
        List<ReferralChannel> referralChannels = referralChannelRepository.findAllByEnableChannel(true);
        List<ReferralChannel> channelList = new ArrayList<>();
        if (referralChannels.isEmpty()) {
            return true;
        }

        if (userRepository.findByUserId(message.getChatId()).getPremiumBotUser() != null) {
            return true;
        }

        for (ReferralChannel channel : referralChannels) {
            if (channel.isBot()) {
                if (!telegramSender.isBotUser(channel.getBotToken(), message)) {
                    channelList.add(channel);
                }
            } else {
                if (!telegramSender.isMember(message.getChatId(), channel.getChannelId())) {
                    channelList.add(channel);
                }
            }
        }

        if (!channelList.isEmpty()) {
            channelList.sort(Comparator.comparing(ReferralChannel::getAddInReferral));
            userRepository.setAccessStatus(false, message.getChatId());
            String text = localeService.getBundle("referralService.notSubscribe");
            telegramSender.send(SendMessage.builder()
                    .text(EmojiParser.parseToUnicode(text))
                    .chatId(message.getChatId())
                    .replyMarkup(createButtonWithChannel(channelList)).build());
            return false;
        }

        if (!userRepository.findByUserId(message.getChatId()).getAccessStatus()) {
            userRepository.setAccessStatus(true, message.getChatId());
        }
        return true;
    }

    public InlineKeyboardMarkup createButtonWithChannel(List<ReferralChannel> channelList) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int i = 0; i < channelList.size(); i++) {
            var button = new InlineKeyboardButton(channelList.get(i).getChannelName());
            button.setUrl(channelList.get(i).getLink());
            if (row.size() == 2) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
            row.add(button);
            if (i == channelList.size() - 1) {
                keyboard.add(row);
            }
        }

        return new InlineKeyboardMarkup(keyboard);
    }
}
