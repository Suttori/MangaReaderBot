package com.suttori.processor;


import com.suttori.handler.CallbackQueryHandler;
import com.suttori.handler.InlineQueryHandler;
import com.suttori.handler.MediaGroupHandler;
import com.suttori.handler.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;

import java.util.List;

@Component
public class DefaultProcessor implements Processor {

    private final CallbackQueryHandler callbackQueryHandler;
    private final MessageHandler messageHandler;
    private final MediaGroupHandler mediaGroupHandler;
    private final InlineQueryHandler inlineQueryHandler;

    @Autowired
    public DefaultProcessor(CallbackQueryHandler callbackQueryHandler, MessageHandler messageHandler, MediaGroupHandler mediaGroupHandler, InlineQueryHandler inlineQueryHandler) {
        this.callbackQueryHandler = callbackQueryHandler;
        this.messageHandler = messageHandler;
        this.mediaGroupHandler = mediaGroupHandler;
        this.inlineQueryHandler = inlineQueryHandler;
    }

    @Override
    public void executeMessage(Update update) {
        messageHandler.choose(update);
    }

    @Override
    public void executeCallBackQuery(CallbackQuery callbackQuery) {
        callbackQueryHandler.choose(callbackQuery);
    }

    @Override
    public void executeInlineQuery(InlineQuery inlineQuery) {
        inlineQueryHandler.choose(inlineQuery);
    }

    @Override
    public void executeMediaGroup(List<Update> updates) {
        mediaGroupHandler.choose(updates);
    }
}
