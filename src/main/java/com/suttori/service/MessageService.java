package com.suttori.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class MessageService {

    private final Logger logger = LoggerFactory.getLogger(MessageService.class);

    public SendMessage createSendMessageWithMaxLength(String text, Long chatId) {
        return SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .disableWebPagePreview(true)
                .parseMode("HTML")
                .build();
    }

    public EditMessageText createEditMessageWithoutButton(String textMessage, Integer messageId, Long chatId) {
        logger.info("createEditMessageWithoutButton " + chatId);
        var editMessageText = new EditMessageText("");
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(textMessage);
        return editMessageText;
    }

    public EditMessageText createEditMessageWithButton(String textMessage, String textButton, Integer messageId, String callbackData, Long chatId) {
        logger.info("createEditMessageWithButton " + chatId);
        var editMessageText = new EditMessageText("");
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(textMessage);
        var buttonOne = new InlineKeyboardButton(textButton);
        buttonOne.setCallbackData(callbackData);
        editMessageText.setReplyMarkup(createButton(Collections.singletonList(buttonOne)));
        return editMessageText;
    }

    public SendMessage createSendMessageWithTwoButtons(String textMessage, String textButtonOne, String textButtonTwo, String callbackDataOne, String callbackDataTwo, Long chatId) {
        logger.info("createSendMessageWithTwoButtons " + chatId);
        var buttonOne = new InlineKeyboardButton(textButtonOne);
        var buttonTwo = new InlineKeyboardButton(textButtonTwo);
        buttonOne.setCallbackData(callbackDataOne);
        buttonTwo.setCallbackData(callbackDataTwo);
        List<InlineKeyboardButton> buttonList = new LinkedList<>();
        buttonList.add(buttonOne);
        buttonList.add(buttonTwo);
        return SendMessage.builder()
                .text(textMessage)
                .chatId(chatId)
                .replyMarkup(createButton(buttonList)).build();
    }

    public InlineKeyboardMarkup createButton(List<InlineKeyboardButton> buttonList) {
        logger.info("createSendMessageWithTwoButtons");
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow rowInLine = new InlineKeyboardRow(buttonList);
        keyboard.add(rowInLine);
        return new InlineKeyboardMarkup(keyboard);
    }
}
