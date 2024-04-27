package com.suttori.service.interfaces;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.suttori.entity.MangaDesu.MangaChapters;
import com.suttori.entity.MangaDesu.MangaDataDesu;
import com.suttori.entity.MangaDex.Manga.MangaDataMangaDex;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegraph.api.objects.Node;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Component
public interface MangaServiceInterface<T, R> {

    void getSearchResult(InlineQuery inlineQuery);

    void sendMangaById(Long userId, String string);

    T getMangaData(String mangaId);

    String getMangaText(T mangaData);

    void clickNotification(CallbackQuery callbackQuery);

    List<?> getChapters(String mangaId);

    int getCurrentPage(CallbackQuery callbackQuery, int currentPage, int lastPage, int gap);

    void getMangaChaptersButton(CallbackQuery callbackQuery);

    void getMangaDataChapters(Long mangaId, Long mangaChapterItemsId);

    void getChapterFromCallbackHandler(CallbackQuery callbackQuery);

    void getChapterHandler(MangaDataDesu mangaDataDesu, Long userId);

    void writeHistory(MangaDataDesu mangaDataDesu, Long userId);

    void writeStatistic(MangaDataDesu mangaDataDesu, Long userId);

    void waitForUploadManhwa(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu);

    void waitForUploadManga(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu);

    void preloadMangaChapter(Long userId, MangaDataDesu mangaDataDesu);

    void preloadManhwaChapter(MangaDataDesu mangaDataDesu, Long userId);

    File compressImages(String pdfFileName, MangaDataDesu mangaDataDesu, Long userId, double compressParam);

    void executeBuilder(FFmpeg ffmpeg, FFprobe ffprobe, PdfDocument pdfDoc, Document doc, String fileName, File file, FFmpegBuilder builder, File folder);

    void sendCopyMessageMangaFromMangaStorage(Integer messageId, Long userId, MangaDataDesu mangaDataDesu);

    void sendTelegraphArticle(Long userId, MangaDataDesu mangaDataDesu);

    void sendPDFChapter(Long userId, MangaDataDesu mangaDataDesu);

    File getJpg(java.io.File folder, URL imgUrl, String fileName);

    InlineKeyboardMarkup getPrevNextButtons(MangaDataDesu mangaDataDesu);

    Node createImage(String imageUrl);

    Integer sendWaitGIFAndAction(Long userId);

    Integer sendWaitForUploadManhwa(Long userId);

    void deleteKeyboard(Integer messageId, Long userId);

    void getRandomManga(Long userId);

    void clickChangeMangaStatus(CallbackQuery callbackQuery);

    void clickMangaStatus(CallbackQuery callbackQuery);

    InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, Long mangaId);

    InlineKeyboardMarkup getKeyboardForChangeStatusViaProfile(String read, String planned, String finished, String postponed, Long mangaId, String viaProfile);

    void clickBackManga(CallbackQuery callbackQuery);

}
