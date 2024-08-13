package com.suttori.service.interfaces;

import com.suttori.entity.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public interface SortFilterInterface {

    void clickSetSortFilterParams(Long userId, Integer messageId);

    void getSortParams(InlineQuery inlineQuery);

    void setSortParams(Message message);

    void getStatusParams(InlineQuery inlineQuery);

    void setStatusParams(Message message);

    void getGenreParams(InlineQuery inlineQuery);

    void setGenreParams(Message message);

    void resetAllSortAndFilterParams(CallbackQuery callbackQuery);

}
