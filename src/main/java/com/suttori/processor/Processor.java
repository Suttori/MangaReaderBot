package com.suttori.processor;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;

import java.util.LinkedList;
import java.util.List;

@Component
public interface Processor {

    void executeMessage(Update update);

    void executeCallBackQuery(CallbackQuery callbackQuery);

    void executeInlineQuery(InlineQuery inlineQuery);

    void executeMediaGroup(List<Update> updates);

    default void process(List<Update> updates) {
        List<Update> updateList = new LinkedList<>();
        for (int i = 0; i < updates.size(); i++) {
            if (updates.get(i).hasMessage() && updates.get(i).getMessage().getMediaGroupId() == null) {
                executeMessage(updates.get(i));
            } else if (updates.get(i).hasCallbackQuery()) {
                executeCallBackQuery(updates.get(i).getCallbackQuery());
            } else if (updates.get(i).hasInlineQuery()) {
                executeInlineQuery(updates.get(i).getInlineQuery());
            } else if (updates.get(i).getMessage() != null && updates.get(i).getMessage().getMediaGroupId() != null) {
                if (updateList.isEmpty()) {
                    updateList.add(updates.get(i));
                } else if (updates.get(i).getMessage().getMediaGroupId().equals(updateList.get(updateList.size() - 1).getMessage().getMediaGroupId())) {
                    updateList.add(updates.get(i));
                    if (updates.size() == i + 1) {
                        executeMediaGroup(updateList);
                        break;
                    }
                    if (updates.get(i + 1).getMessage().getMediaGroupId() == null) {
                        executeMediaGroup(updateList);
                        updateList = new LinkedList<>();
                    }
                } else {
                    executeMediaGroup(updateList);
                    updateList = new LinkedList<>();
                    updateList.add(updates.get(i));
                }
            }
        }
    }
}
