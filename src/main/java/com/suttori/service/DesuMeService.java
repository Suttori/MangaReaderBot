package com.suttori.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.suttori.dao.NotificationEntityRepository;
import com.suttori.entity.MangaDesu.*;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.DesuMeApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegraph.api.objects.Node;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DesuMeService implements MangaServiceInterface<MangaDataDesu, Long> {

    private DesuMeApiFeignClient desuMeApiFeignClient;
    private TelegramSender telegramSender;
    private Util util;
    private MangaService mangaService;

    private NotificationEntityRepository notificationEntityRepository;

    @Autowired
    public DesuMeService(DesuMeApiFeignClient desuMeApiFeignClient, TelegramSender telegramSender, Util util, MangaService mangaService, NotificationEntityRepository notificationEntityRepository) {
        this.desuMeApiFeignClient = desuMeApiFeignClient;
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaService = mangaService;
        this.notificationEntityRepository = notificationEntityRepository;
    }

    private static final Map<String, String> TAG_TRANSLATIONS = new HashMap<>();

    static {
        TAG_TRANSLATIONS.put("безумие", "Dementia");
        TAG_TRANSLATIONS.put("боевые_искусства", "Martial_Arts");
        TAG_TRANSLATIONS.put("в_цвете", "Color");
        TAG_TRANSLATIONS.put("вампиры", "Vampire");
        TAG_TRANSLATIONS.put("веб", "Web");
        TAG_TRANSLATIONS.put("гарем", "Harem");
        TAG_TRANSLATIONS.put("героическое_фэнтези", "Heroic_Fantasy");
        TAG_TRANSLATIONS.put("демоны", "Demons");
        TAG_TRANSLATIONS.put("детектив", "Mystery");
        TAG_TRANSLATIONS.put("дзёсей", "Josei");
        TAG_TRANSLATIONS.put("драма", "Drama");
        TAG_TRANSLATIONS.put("ёнкома", "Yonkoma");
        TAG_TRANSLATIONS.put("игры", "Game");
        TAG_TRANSLATIONS.put("исекай", "Isekai");
        TAG_TRANSLATIONS.put("исторический", "Historical");
        TAG_TRANSLATIONS.put("космос", "Space");
        TAG_TRANSLATIONS.put("литRPG", "LitRPG");
        TAG_TRANSLATIONS.put("магия", "Magic");
        TAG_TRANSLATIONS.put("меха", "Mecha");
        TAG_TRANSLATIONS.put("мистика", "Mystic");
        TAG_TRANSLATIONS.put("музыка", "Music");
        TAG_TRANSLATIONS.put("научная фантастика", "Sci-Fi");
        TAG_TRANSLATIONS.put("пародия", "Parody");
        TAG_TRANSLATIONS.put("повседневность", "Slice_of_Life");
        TAG_TRANSLATIONS.put("постапокалиптика", "Post_Apocalyptic");
        TAG_TRANSLATIONS.put("приключения", "Adventure");
        TAG_TRANSLATIONS.put("психологическое", "Psychological");
        TAG_TRANSLATIONS.put("романтика", "Romance");
        TAG_TRANSLATIONS.put("сверхъестественное", "Supernatural");
        TAG_TRANSLATIONS.put("сёдзе", "Shoujo");
        TAG_TRANSLATIONS.put("сёдзе_ай", "Shoujo_Ai");
        TAG_TRANSLATIONS.put("сейнен", "Seinen");
        TAG_TRANSLATIONS.put("сёнен", "Shounen");
        TAG_TRANSLATIONS.put("сёнен_ай", "Shounen_Ai");
        TAG_TRANSLATIONS.put("смена_пола", "Gender_Bender");
        TAG_TRANSLATIONS.put("спорт", "Sports");
        TAG_TRANSLATIONS.put("супер_сила", "Super_Power");
        TAG_TRANSLATIONS.put("трагедия", "Tragedy");
        TAG_TRANSLATIONS.put("триллер", "Thriller");
        TAG_TRANSLATIONS.put("ужасы", "Horror");
        TAG_TRANSLATIONS.put("фантастика", "Fiction");
        TAG_TRANSLATIONS.put("фэнтези", "Fantasy");
        TAG_TRANSLATIONS.put("хентай", "Hentai");
        TAG_TRANSLATIONS.put("школа", "School");
        TAG_TRANSLATIONS.put("экшен", "Action");
        TAG_TRANSLATIONS.put("этти", "Ecchi");
        TAG_TRANSLATIONS.put("юри", "Yuri");
        TAG_TRANSLATIONS.put("яой", "Yaoi");
        TAG_TRANSLATIONS.put("манга", "manga");
        TAG_TRANSLATIONS.put("манхва", "manhwa");
        TAG_TRANSLATIONS.put("манхьхуа", "manhua");
        TAG_TRANSLATIONS.put("ваншот", "one_shot");
        TAG_TRANSLATIONS.put("комикс", "comics");
    }

    @Override
    public void getSearchResult(InlineQuery inlineQuery) {
        try {
            int offset = 30;
            if (!inlineQuery.getOffset().isEmpty()) {
                offset = Integer.parseInt(inlineQuery.getOffset());
            }

            String page = String.valueOf(offset / 30);
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = desuMeApiFeignClient.searchManga(getSearchParams(inlineQuery, page));
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaSearchResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaSearchResponse.class);

            if (mangaResponse.getResponse() == null) {
                return;
            }
            List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
            int i = 0;
            for (MangaDataAsSearchResult mangaData : mangaResponse.getResponse()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Рейтинг: ").append(mangaData.getScore());
                stringBuilder.append(" | Год: ").append(new SimpleDateFormat("yyyy").format(new Date(mangaData.getAired_on() * 1000))).append(" | Тип: ").append(mangaData.getKind()).append("\n");
                stringBuilder.append("Статус: ").append(util.getStatus(mangaData.getStatus())).append("\n");
                stringBuilder.append("Жанр: ").append(mangaData.getGenres());
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(mangaData.getRussian())
                        .description(stringBuilder.toString())
                        .thumbnailUrl(mangaData.getImage().getOriginal())
                        .inputMessageContent(new InputTextMessageContent("mangaId\n" + mangaData.getId())).build());
            }
            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(inlineQueryResultList)
                    .nextOffset(String.valueOf(offset + 30))
                    .cacheTime(1)
                    .isPersonal(true)
                    .inlineQueryId(inlineQuery.getId()).build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Map<String, String> getSearchParams(InlineQuery inlineQuery, String page) {
        Map<String, String> params = new HashMap<>();
        String query = inlineQuery.getQuery();

        if (query.contains("popular")) {
            params.put("order", "popular");
            params.put("page", page);
            params.put("limit", "30");
        } else if (query.contains("last updated")) {
            params.put("order", "updated");
            params.put("page", page);
            params.put("limit", "30");
        } else if (query.contains("hashtag")) {
            params.put("page", page);
            params.put("limit", "30");
            String[] lines = query.split("\n");
            String[] tags = lines[1].split(" ");
            StringBuilder genresBuilder = new StringBuilder();
            StringBuilder kindBuilder = new StringBuilder();
            StringBuilder statusBuilder = new StringBuilder();

            for (String tag : tags) {
                String cleanTag = tag.replace("#", "");
                String translatedTag = TAG_TRANSLATIONS.get(cleanTag.toLowerCase());

                if (translatedTag == null) {
                    continue;
                }

                if (translatedTag.equals("manga") || translatedTag.equals("manhwa") || translatedTag.equals("comics") || translatedTag.equals("manhua") || translatedTag.equals("one_shot")) {
                    appendWithComma(kindBuilder, translatedTag);
                } else if (translatedTag.equals("ongoing") || translatedTag.equals("released") || translatedTag.equals("continued") || translatedTag.equals("completed")) {
                    appendWithComma(statusBuilder, translatedTag);
                } else {
                    appendWithComma(genresBuilder, translatedTag.replace("_", " "));
                }
            }

            if (!genresBuilder.isEmpty()) {
                params.put("genres", genresBuilder.toString());
            }
            if (!kindBuilder.isEmpty()) {
                params.put("kinds", kindBuilder.toString());
            }
            if (!statusBuilder.isEmpty()) {
                params.put("status", statusBuilder.toString());
            }
        } else {
            params.put("search", query);
            params.put("limit", "30");
            params.put("page", page);
        }
        return params;
    }

    private void appendWithComma(StringBuilder builder, String value) {
        if (!builder.isEmpty()) {
            builder.append(",");
        }
        builder.append(value);
    }

    @Override
    public void sendMangaById(Long userId, String string) {
        String mangaId = util.parseValue(string)[1];
        MangaDataDesu mangaDataDesu = getMangaData(mangaId);
        telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(mangaDataDesu.getImage().getOriginal()))
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(mangaService.getMangaButtons(userId, mangaId))
                .caption(getMangaText(mangaDataDesu)).build());
    }

    @Override
    public MangaDataDesu getMangaData(String mangaId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = desuMeApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {

    }

    public String getMangaText(MangaDataDesu mangaDataDesu) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b>").append(mangaDataDesu.getRussian()).append("</b>").append("\n\n");
        stringBuilder.append("<b>").append("Рейтинг: ").append("</b>").append(mangaDataDesu.getScore()).append("\n");
        stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(new SimpleDateFormat("yyyy").format(new Date(mangaDataDesu.getAired_on() * 1000))).append("\n");
        stringBuilder.append("<b>").append("Тип: ").append("</b>").append(mangaDataDesu.getKind()).append("\n");
        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(util.getStatus(mangaDataDesu.getStatus())).append("\n");
        stringBuilder.append("<b>").append("Глав: ").append("</b>").append(mangaDataDesu.getChapters().getLast().getCh()).append("\n");
        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(util.getGenres(mangaDataDesu.getGenres())).append("</i>\n\n");
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(mangaDataDesu.getDescription());

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    @Override
    public List<MangaChapterItem> getChapters(String mangaId) {
        try {
            Response response = desuMeApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = new ObjectMapper().readValue(jsonResponse, MangaResponse.class);
            MangaDataDesu mangaDataDesu = mangaResponse.getResponse();
            return mangaDataDesu.getChapters().getList();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void getMangaChaptersButton(CallbackQuery callbackQuery) {
        String mangaId = util.parseValue(callbackQuery.getData())[1];
        List<MangaChapterItem> mangaChapterItems = getChapters(mangaId);
        Collections.reverse(mangaChapterItems);

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        int chapterPerPage = 10;
        int gap = 15;
        int currentPage = Integer.parseInt(util.parseValue(callbackQuery.getData())[2]);
        if (currentPage == 1) {
            gap = 14;
        }
        int lastPage;
        if (mangaChapterItems.size() % chapterPerPage == 0) {
            lastPage =mangaChapterItems.size() / chapterPerPage;
        } else {
            lastPage = mangaChapterItems.size() / chapterPerPage + 1;
        }
        currentPage = getCurrentPage(callbackQuery, currentPage, lastPage, gap);
        if (currentPage == 0) {
            return;
        }

        int startIndex = currentPage * chapterPerPage - chapterPerPage;
        int endIndex = Math.min(startIndex + chapterPerPage,mangaChapterItems.size());

        InlineKeyboardRow row = new InlineKeyboardRow();
        keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В начало")).callbackData("onFirstPage\n" + mangaId + "\n" + currentPage).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(String.valueOf(currentPage))).callbackData("currentPage\n" + mangaId + "\n" + currentPage).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В конец")).callbackData("onLastPage\n" + mangaId + "\n" + currentPage).build()));

        for (int i = startIndex; i < endIndex; i++) {
            String buttonText = "Том " + mangaChapterItems.get(i).getVol() + ". " + "Глава " + mangaChapterItems.get(i).getCh();
            if (row.size() == 2) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
            row.add(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(buttonText)).callbackData("chapter\n" + mangaId + "\n" + mangaChapterItems.get(i).getId()).build());
            if (i == endIndex - 1) {
                keyboard.add(row);
            }
        }

        keyboard.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("<<")).callbackData("prevGapPage\n" + mangaId + "\n" + (currentPage - gap)).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("prevPage\n" + mangaId + "\n" + (currentPage - 1)).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData("nextPage\n" + mangaId + "\n" + (currentPage + 1)).build(),
                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(">>")).callbackData("nextGapPage\n" + mangaId + "\n" + (currentPage + gap)).build()));


        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .chatId(callbackQuery.getFrom().getId())
                .replyMarkup(new InlineKeyboardMarkup(keyboard))
                .messageId(callbackQuery.getMessage().getMessageId()).build());

    }

    @Override
    public int getCurrentPage(CallbackQuery callbackQuery, int currentPage, int lastPage, int gap) {
        String query = util.parseValue(callbackQuery.getData())[0];
        if ((query.contains("onFirstPage") && currentPage == 1) || (currentPage == 0 && query.contains("prevPage")) || (query.contains("prevGapPage") && currentPage == 1 - gap)) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .text("Вы уже находитесть на первой странице")
                    .showAlert(false)
                    .callbackQueryId(callbackQuery.getId()).build());
            return 0;
        } else if ((query.contains("prevGapPage") && currentPage <= 0) || query.contains("onFirstPage")) {
            return 1;
        } else if ((query.contains("onLastPage") && currentPage == lastPage) || (currentPage == lastPage + 1 && query.contains("nextPage")) || (query.contains("nextGapPage") && currentPage == lastPage + gap)) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .text("Вы уже находитесть на последней странице")
                    .showAlert(false)
                    .callbackQueryId(callbackQuery.getId()).build());
            return 0;
        } else if ((query.contains("nextGapPage") && currentPage >= lastPage) || query.contains("onLastPage")) {
            return lastPage;
        }
        return currentPage;
    }


    @Override
    public void getMangaDataChapters(Long mangaId, Long mangaChapterItemsId) {

    }

    @Override
    public void getChapterFromCallbackHandler(CallbackQuery callbackQuery) {

    }

    @Override
    public void getChapterHandler(MangaDataDesu mangaDataDesu, Long userId) {

    }

    @Override
    public void writeHistory(MangaDataDesu mangaDataDesu, Long userId) {

    }

    @Override
    public void writeStatistic(MangaDataDesu mangaDataDesu, Long userId) {

    }

    @Override
    public void waitForUploadManhwa(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void waitForUploadManga(Long userId, Long copyMessageMangaId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void preloadMangaChapter(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void preloadManhwaChapter(MangaDataDesu mangaDataDesu, Long userId) {

    }

    @Override
    public File compressImages(String pdfFileName, MangaDataDesu mangaDataDesu, Long userId, double compressParam) {
        return null;
    }

    @Override
    public void executeBuilder(FFmpeg ffmpeg, FFprobe ffprobe, PdfDocument pdfDoc, Document doc, String fileName, File file, FFmpegBuilder builder, File folder) {

    }

    @Override
    public void sendCopyMessageMangaFromMangaStorage(Integer messageId, Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void sendTelegraphArticle(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public void sendPDFChapter(Long userId, MangaDataDesu mangaDataDesu) {

    }

    @Override
    public File getJpg(File folder, URL imgUrl, String fileName) {
        return null;
    }

    @Override
    public InlineKeyboardMarkup getPrevNextButtons(MangaDataDesu mangaDataDesu) {
        return null;
    }

    @Override
    public Node createImage(String imageUrl) {
        return null;
    }

    @Override
    public Integer sendWaitGIFAndAction(Long userId) {
        return null;
    }

    @Override
    public Integer sendWaitForUploadManhwa(Long userId) {
        return null;
    }

    @Override
    public void deleteKeyboard(Integer messageId, Long userId) {

    }

    @Override
    public void getRandomManga(Long userId) {

    }

    @Override
    public void clickChangeMangaStatus(CallbackQuery callbackQuery) {

    }

    @Override
    public void clickMangaStatus(CallbackQuery callbackQuery) {

    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatus(String read, String planned, String finished, String postponed, Long mangaId) {
        return null;
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForChangeStatusViaProfile(String read, String planned, String finished, String postponed, Long mangaId, String viaProfile) {
        return null;
    }

    @Override
    public void clickBackManga(CallbackQuery callbackQuery) {

    }
}
