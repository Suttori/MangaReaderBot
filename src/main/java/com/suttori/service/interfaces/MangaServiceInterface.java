package com.suttori.service.interfaces;

import com.suttori.entity.Chapter;
import com.suttori.entity.MangaButtonData;
import com.suttori.entity.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
public interface MangaServiceInterface<T> {

    void getSearchResult(InlineQuery inlineQuery, User user);

    void sendMangaById(Long userId, String mangaId);

    void sendMangaById(Long userId, String mangaId, String languageCode);

    void sendMangaByDatabaseId(Long userId, String mangaDatabaseId);

    void clickNotification(CallbackQuery callbackQuery);

    void getMangaChaptersButton(InlineQuery inlineQuery);

    InlineKeyboardMarkup getPrevNextButtons(Chapter chapter, Long userId);

    InlineKeyboardMarkup getMangaButtons(MangaButtonData mangaButtonData);

    Integer createTelegraphArticleChapter(Long userId, Chapter chapter);

    Integer createPdfChapter(Long userId, Chapter chapter);

    Integer createCbzChapter(Long userId, Chapter chapter);

    void preloadMangaChapter(Long userId, Chapter chapter);

    void preloadManhwaChapter(Long userId, Chapter chapter);

    void getRandomManga(Long userId);

}
