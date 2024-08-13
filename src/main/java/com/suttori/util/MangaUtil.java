package com.suttori.util;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.suttori.dao.*;
import com.suttori.dto.ChapterDto;
import com.suttori.entity.*;
import com.suttori.exception.CatalogNotFoundException;
import com.suttori.telegram.TelegramSender;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegraph.api.objects.Node;
import org.telegram.telegraph.api.objects.NodeElement;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

@Service
@Slf4j
public class MangaUtil {

    private ReadStatusRepository readStatusRepository;
    private TelegramSender telegramSender;
    private Util util;

    private MangaChapterRepository mangaChapterRepository;
    private UserSortPreferencesRepository userSortPreferencesRepository;
    private MangaStatusParameterRepository mangaStatusParameterRepository;

    @Autowired
    public MangaUtil(ReadStatusRepository readStatusRepository, TelegramSender telegramSender, Util util, MangaChapterRepository mangaChapterRepository, UserSortPreferencesRepository userSortPreferencesRepository, MangaStatusParameterRepository mangaStatusParameterRepository) {
        this.readStatusRepository = readStatusRepository;
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaChapterRepository = mangaChapterRepository;
        this.userSortPreferencesRepository = userSortPreferencesRepository;
        this.mangaStatusParameterRepository = mangaStatusParameterRepository;
    }


    public void createAnswerInlineQueryButtons(InlineQuery inlineQuery, List<Chapter> sortedChapters, User user) {
        String catalogName;
        try {
            catalogName = util.getSourceName(inlineQuery.getQuery());
        } catch (CatalogNotFoundException e) {
            log.error("Ошибка при получении каталога", e);
            util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", user.getUserId());
            return;
        }

        int offset = 0;
        if (!inlineQuery.getOffset().isEmpty()) {
            offset = Integer.parseInt(inlineQuery.getOffset());
        }

        int limit = offset + 49;
        if (limit > sortedChapters.size()) {
            limit = sortedChapters.size();
        }

        if (user.getSortParam() != null && user.getSortParam().equals("sortDESC")) {
            Collections.reverse(sortedChapters);
        }

        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        if (offset == 0) {
            if (user.getSortParam() == null || user.getSortParam().equals("sortASC")) {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "0")
                        .title(EmojiParser.parseToUnicode("Отсортировать с конца"))
                        .description("Нажми чтобы отсортировать главы от последней к первой")
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/free-icon-sort-1102420.png")
                        .inputMessageContent(new InputTextMessageContent("sortDESC")).build());
            } else {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "0")
                        .title(EmojiParser.parseToUnicode("Отсортировать с начала"))
                        .description("Нажми чтобы отсортировать главы от первой к последней")
                        .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/free-icon-sort-1102420.png")
                        .inputMessageContent(new InputTextMessageContent("sortASC")).build());
            }
        }

        int i = 1;
        String downloadStatus;
        String readStatus;
        for (int j = offset; j < limit; j++) {
            if ((user.getMangaFormatParameter() == null && (isNotLongStripMangaDex(sortedChapters.get(j)) || isMangaDesuMe(sortedChapters.get(j)))) || (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("telegraph"))) {
                if (sortedChapters.get(j).getTelegraphStatusDownload() != null && sortedChapters.get(j).getTelegraphStatusDownload().equals("finished")) {
                    downloadStatus = "✔️ Загружена";
                } else {
                    downloadStatus = "Не загружена";
                }
            } else {
                if (sortedChapters.get(j).getPdfStatusDownload() != null && sortedChapters.get(j).getPdfStatusDownload().equals("finished")) {
                    downloadStatus = "✔️ Загружена";
                } else {
                    downloadStatus = "Не загружена";
                }
            }


            readStatus = readStatusRepository.existsByMangaIdAndChapterIdAndUserIdAndCatalogName(sortedChapters.get(j).getMangaId(), sortedChapters.get(j).getChapterId(), user.getUserId(), sortedChapters.get(0).getCatalogName()) ? "✔️ Прочитана" : "Не прочитана";

            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title("Том " + sortedChapters.get(j).getVol() + ". Глава " + sortedChapters.get(j).getChapter())
                    .description(downloadStatus + "\n" + readStatus)
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/hand-drawn-vintage-comic-illustration_23-2149624608.jpg")
                    .inputMessageContent(new InputTextMessageContent(catalogName + "\nchapterId\n" + sortedChapters.get(j).getId())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .nextOffset(String.valueOf(limit))
                .cacheTime(1)
                .isPersonal(true)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public boolean isNotLongStripMangaDex(Chapter chapter) {
        return chapter.getCatalogName().equals("mangadex.org") && chapter.getFormat() != null && !chapter.getFormat().contains("Long Strip");
    }

    public boolean isMangaDesuMe(Chapter chapter) {
        return chapter.getCatalogName().equals("desu.me") && chapter.getType() != null && (chapter.getType().equals("manga") || chapter.getType().equals("one_shot") || chapter.getType().equals("comics"));
    }

    public File getJpeg(File folder, File file, String fileName) {
        try {
            FFmpeg ffmpeg = new FFmpeg();
            FFprobe ffprobe = new FFprobe();
            FFmpegStream photoStream = ffprobe.probe(file.getPath()).getStreams().get(0);
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(file.getPath())
                    .overrideOutputFiles(true)
                    .addOutput(new java.io.File(folder + File.separator + "output_img" + fileName + ".jpeg").getPath())
                    .setVideoCodec("mjpeg")
                    .setVideoResolution(photoStream.width, photoStream.height)
                    .done();
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();
            file.delete();
            return new File(folder + File.separator + "output_img" + fileName + ".jpeg");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Node createImage(String imageUrl) {
        NodeElement image = new NodeElement();
        image.setTag("img");
        image.setAttrs(new HashMap<>());
        image.getAttrs().put("src", imageUrl);
        return image;
    }

    public void executeBuilder(FFmpeg ffmpeg, FFprobe ffprobe, PdfDocument pdfDoc, Document doc, String fileName, File file, FFmpegBuilder builder, File folder) {
        try {
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
            ImageData imgData = ImageDataFactory.create(folder + File.separator + fileName + "output.jpeg");
            Image image = new Image(imgData);
            PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
            pdfDoc.addNewPage(pageSize);
            image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
            doc.add(image);
            file.delete();
            new File(folder + File.separator + fileName + "output.jpeg").delete();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer sendWaitGIFAndAction(Long userId) {
        Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                .chatId(userId)
                .caption("Твоя глава уже загружается, обычно это занимает не больше минуты, спасибо за ожидание\n\nВ боте предусмотрена автоматическая предзагрузка глав, поэтому пока ты будешь читать текующую главу, следующая уже будет загружена")
                .document(new InputFile("CgACAgQAAxkBAAICV2XKAyJ_d0xIoK5tTXiI14xVYCB5AAKJCwACye1AUZtzbClFKHTFNAQ")).build()).getMessageId();
        telegramSender.sendChatAction(userId, "upload_document");
        return messageId;
    }

    public Chapter getChapterByDto(ChapterDto chapterDto) {
        Chapter chapter = convertToChapter(chapterDto);
        if (chapter == null) {
            return null;
        }
        chapter.setPrevChapter(convertToChapter(mangaChapterRepository.findChapterDtoByChapterId(chapterDto.getPrevChapterId())));
        chapter.setNextChapter(convertToChapter(mangaChapterRepository.findChapterDtoByChapterId(chapterDto.getNextChapterId())));
        return chapter;

    }

    public Chapter convertToChapter(ChapterDto dto) {
        if (dto == null) {
            return null;
        }
        Chapter chapter = new Chapter();
        chapter.setId(dto.getId());
        chapter.setMessageId(dto.getMessageId());
        chapter.setBackupMessageId(dto.getBackupMessageId());
        chapter.setCatalogName(dto.getCatalogName());
        chapter.setMangaId(dto.getMangaId());
        chapter.setMangaDataBaseId(dto.getMangaDataBaseId());
        chapter.setChapterId(dto.getChapterId());
        chapter.setType(dto.getType());
        chapter.setFormat(dto.getFormat());
        chapter.setName(dto.getName());
        chapter.setTelegraphUrl(dto.getTelegraphUrl());
        chapter.setVol(dto.getVol());
        chapter.setChapter(dto.getChapter());
        chapter.setStatus(dto.getStatus());
        chapter.setAddedAt(dto.getAddedAt());
        chapter.setLanguageCode(dto.getLanguageCode());
        chapter.setPdfMessageId(dto.getPdfMessageId());
        chapter.setPdfStatusDownload(dto.getPdfStatusDownload());
        chapter.setTelegraphMessageId(dto.getTelegraphMessageId());
        chapter.setTelegraphStatusDownload(dto.getTelegraphStatusDownload());
        return chapter;
    }

    public List<Chapter> getChaptersInOrder(List<Chapter> chapters) {
        Chapter firstChapter = chapters.stream()
                .filter(chapter -> chapter.getPrevChapter() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No starting chapter found"));

        List<Chapter> sortedChapters = new ArrayList<>();
        Chapter currentChapter = firstChapter;

        while (currentChapter != null) {
            sortedChapters.add(currentChapter);
            currentChapter = currentChapter.getNextChapter();
        }

        return sortedChapters;
    }



    public UserSortPreferences getUserSortPreferences(Long userId, String catalogName) {
        return userSortPreferencesRepository.findByUserIdAndCatalogName(userId, catalogName);
    }

    public void checkDuplicateMangaStatusParameter(Long userId) {
        List<MangaStatusParameter> statusParameterList = mangaStatusParameterRepository.findAllByUserId(userId);
        for (MangaStatusParameter statusParameter : statusParameterList) {
            for (MangaStatusParameter mangaStatusParameter : statusParameterList) {
                if (!statusParameter.getId().equals(mangaStatusParameter.getId()) && statusParameter.getMangaId().equals(mangaStatusParameter.getMangaId()) && statusParameter.getCatalogName().equals(mangaStatusParameter.getCatalogName())) {
                    if (statusParameter.getAddedAt().after(mangaStatusParameter.getAddedAt())) {
                        mangaStatusParameterRepository.delete(mangaStatusParameter);
                    } else {
                        mangaStatusParameterRepository.delete(statusParameter);
                    }
                }
            }
        }
    }

}
