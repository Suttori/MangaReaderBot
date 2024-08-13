package com.suttori.service.interfaces;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.suttori.entity.Chapter;
import com.suttori.entity.Manga;
import com.suttori.entity.MangaButtonData;
import com.suttori.entity.MangaDesu.MangaDataDesu;
import com.suttori.entity.User;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegraph.api.objects.Node;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

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

    void sendTelegraphArticle(Long userId, Chapter chapter);

    void sendPDFChapter(Long userId, Chapter chapter);

    void preloadMangaChapter(Long userId, Chapter chapter);

    void preloadManhwaChapter(Long userId, Chapter chapter);

    void getRandomManga(Long userId);

}
