package com.suttori.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.suttori.dao.*;
import com.suttori.dto.ChapterDto;
import com.suttori.entity.*;
import com.suttori.entity.MangaDex.Tag.TagData;
import com.suttori.entity.MangaDex.Tag.TagResponse;
import com.suttori.entity.NotificationChapterMapping;
import com.suttori.entity.NotificationEntity;
import com.suttori.entity.MangaDex.Chapter.*;
import com.suttori.entity.MangaDex.Manga.*;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.MangaDexApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.telegram.TelegraphApiFeignClient;
import com.suttori.util.MangaUtil;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegraph.api.methods.CreatePage;
import org.telegram.telegraph.api.objects.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MangaDexService implements MangaServiceInterface<MangaDataMangaDex> {

    @Value("${telegraphApiToken}")
    private String telegraphApiToken;
    @Value("${telegraphAuthorName}")
    private String telegraphAuthorName;
    @Value("${telegraphAuthorUrl}")
    private String telegraphAuthorUrl;
    @Value("${mangaDex}")
    private String mangaDex;

    private MangaDexApiFeignClient mangaDexApiFeignClient;
    private TelegraphApiFeignClient telegraphApiFeignClient;

    private TelegramSender telegramSender;
    private Util util;
    private MangaUtil mangaUtil;
    private SenderService senderService;


    private NotificationEntityRepository notificationEntityRepository;
    private UserRepository userRepository;
    private MangaRepository mangaRepository;
    private MangaChapterRepository mangaChapterRepository;
    private NotificationChapterMappingRepository notificationChapterMappingRepository;
    private ReadStatusRepository readStatusRepository;
    private UserFilterPreferencesRepository userFilterPreferencesRepository;


    @Autowired
    public MangaDexService(MangaDexApiFeignClient mangaDexApiFeignClient, TelegramSender telegramSender, Util util, NotificationEntityRepository notificationEntityRepository, UserRepository userRepository, MangaRepository mangaRepository, MangaChapterRepository mangaChapterRepository, NotificationChapterMappingRepository notificationChapterMappingRepository, ReadStatusRepository readStatusRepository, TelegraphApiFeignClient telegraphApiFeignClient, MangaUtil mangaUtil, SenderService senderService, UserFilterPreferencesRepository userFilterPreferencesRepository) {
        this.mangaDexApiFeignClient = mangaDexApiFeignClient;
        this.telegramSender = telegramSender;
        this.util = util;
        this.notificationEntityRepository = notificationEntityRepository;
        this.userRepository = userRepository;
        this.mangaRepository = mangaRepository;
        this.mangaChapterRepository = mangaChapterRepository;
        this.notificationChapterMappingRepository = notificationChapterMappingRepository;
        this.readStatusRepository = readStatusRepository;
        this.telegraphApiFeignClient = telegraphApiFeignClient;
        this.mangaUtil = mangaUtil;
        this.senderService = senderService;
        this.userFilterPreferencesRepository = userFilterPreferencesRepository;
    }

    @Override
    public void getSearchResult(InlineQuery inlineQuery, User user) {
        try {
            String languageCodeForCatalog = userRepository.findByUserId(inlineQuery.getFrom().getId()).getLanguageCodeForCatalog();
            int offset = !inlineQuery.getOffset().isEmpty() ? Integer.parseInt(inlineQuery.getOffset()) : 30;

            ObjectMapper objectMapper = new ObjectMapper();
            Response response = mangaDexApiFeignClient.searchMangaIncludesCoverArt(getSearchParams(inlineQuery, offset, languageCodeForCatalog));
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaListResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaListResponse.class);
            if (mangaResponse.getResponse() == null) {
                return;
            }

            List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
            int i = 0;
            for (MangaDataMangaDex mangaData : mangaResponse.getData()) {
                MangaAttributes attributes = mangaData.getAttributes();
                String text = getYear(attributes.getYear()) +
                        " | Формат: " + getFormat(attributes.getTags(), languageCodeForCatalog) + "\n" +
                        "Статус: " + attributes.getStatus() + "\n" +
                        "Жанр: " + getGenres(attributes.getTags(), languageCodeForCatalog);

                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(getTitle(attributes.getTitle(), languageCodeForCatalog))
                        .description(text)
                        .thumbnailUrl(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "256"))
                        .inputMessageContent(new InputTextMessageContent(mangaDex + "\nmangaId\n" + mangaData.getId())).build());
            }

            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(inlineQueryResultList)
                    .nextOffset(String.valueOf(offset + 30))
                    .cacheTime(1)
                    .isPersonal(true)
                    .inlineQueryId(inlineQuery.getId()).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, List<String>> getSearchParams(InlineQuery inlineQuery, int offset, String languageCodeForCatalog) {
        Map<String, List<String>> searchParams = new HashMap<>();
        Map<String, List<String>> filterPreferences = getUserFilterPreferences(inlineQuery.getFrom().getId(), mangaDex);


        UserSortPreferences userSortPreferences = mangaUtil.getUserSortPreferences(inlineQuery.getFrom().getId(), mangaDex);
        if (userSortPreferences != null) {
            searchParams.put(userSortPreferences.getSortName(), List.of(userSortPreferences.getSortType()));
        }


        if (filterPreferences != null) {
            for (Map.Entry<String, List<String>> filterPreference : filterPreferences.entrySet()) {
                if (filterPreference.getKey().equals("includedTags[]")) {
                    searchParams.put("includedTags[]", getTagsId(filterPreference.getValue()));
                } else {
                    searchParams.put(filterPreference.getKey(), filterPreference.getValue());
                }
            }
        }

        searchParams.put("title", Collections.singletonList(inlineQuery.getQuery()));

        searchParams.put("limit", List.of("30"));
        searchParams.put("offset", List.of(String.valueOf(offset - 30)));
        //searchParams.put("contentRating[]", List.of("safe", "suggestive", "erotica", "pornographic"));
        searchParams.put("availableTranslatedLanguage[]", List.of(languageCodeForCatalog));
        searchParams.put("hasAvailableChapters", Collections.singletonList("true"));
        return searchParams;
    }

    private Map<String, List<String>> getUserFilterPreferences(Long userId, String catalogName) {
        List<UserFilterPreference> filterPreferenceList = userFilterPreferencesRepository.findAllByUserIdAndCatalogName(userId, catalogName);
        if (!filterPreferenceList.isEmpty()) {
            return filterPreferenceList.stream()
                    .collect(Collectors.groupingBy(
                            UserFilterPreference::getFilterTag,
                            Collectors.mapping(UserFilterPreference::getFilterValue, Collectors.toList())
                    ));
        } else {
            return Collections.emptyMap();
        }
    }

    private List<String> getTagsId(List<String> includedTagNames) {
        try {
            Response response = mangaDexApiFeignClient.getTagsId();
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);

            TagResponse mangaResponse = mapper.readValue(jsonResponse, TagResponse.class);

            return mangaResponse.getData().stream()
                    .filter(tagData -> {
                        String tagName = tagData.getAttributes().getName().get("en");
                        return includedTagNames.contains(tagName);
                    })
                    .map(TagData::getId)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void sendMangaById(Long userId, String mangaId) {
        String languageCode = userRepository.findByUserId(userId).getLanguageCodeForCatalog();
        Manga manga = mangaRepository.findByMangaIdAndCatalogNameAndLanguageCode(mangaId, mangaDex, languageCode);
        MangaDataMangaDex mangaData = getMangaData(mangaId);

        if (manga == null) {
            manga = saveManga(mangaData, languageCode);
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverUrl()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId(), manga.getLanguageCode())))
                    .caption(getMangaText(manga)).build()));
            mangaRepository.setCoverFileId(coverFileId, manga.getId());
        } else {
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverFileId() != null ? manga.getCoverFileId() : manga.getCoverUrl()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId(), manga.getLanguageCode())))
                    .caption(getMangaText(manga)).build()));
            if (manga.getCoverFileId() == null) {
                mangaRepository.setCoverFileId(coverFileId, manga.getId());
            }

            if (manga.getCoverUrl() == null) {
                mangaRepository.setCoverUrl(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "512"), manga.getId());
            }
        }
    }

    @Override
    public void sendMangaById(Long userId, String mangaId, String languageCode) {
        Manga manga = mangaRepository.findByMangaIdAndCatalogNameAndLanguageCode(mangaId, mangaDex, languageCode);
        MangaDataMangaDex mangaData = getMangaData(mangaId);

        if (manga == null) {
            manga = saveManga(mangaData, languageCode);
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverUrl()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId(), manga.getLanguageCode())))
                    .caption(getMangaText(manga)).build()));
            mangaRepository.setCoverFileId(coverFileId, manga.getId());
        } else {
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverUrl()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId(), manga.getLanguageCode())))
                    .caption(getMangaText(manga)).build()));
            if (manga.getCoverFileId() == null) {
                mangaRepository.setCoverFileId(coverFileId, manga.getId());
            }
            if (manga.getCoverUrl() == null) {
                mangaRepository.setCoverUrl(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "512"), manga.getId());
            }
        }
    }

    private MangaDataMangaDex getMangaData(String mangaId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = mangaDexApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getData();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Manga saveManga(MangaDataMangaDex mangaData, String languageCode) {
        return mangaRepository.save(new Manga(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "512"), mangaData.getId(), mangaDex,
                getTitle(mangaData.getAttributes().getTitle(), languageCode), mangaData.getType(), mangaData.getAttributes().getStatus(),
                getGenres(mangaData.getAttributes().getTags(), languageCode), getDescription(mangaData.getAttributes().getDescription(), languageCode),
                String.valueOf(mangaData.getAttributes().getYear()), null, null, getFormat(mangaData.getAttributes().getTags(), languageCode), languageCode));
    }

    @Override
    public void sendMangaByDatabaseId(Long userId, String mangaDatabaseId) {
        Manga manga = mangaRepository.findById(Long.valueOf(mangaDatabaseId)).orElseThrow();
        String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(manga.getCoverUrl()))
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(getMangaButtons(new MangaButtonData(userId, manga.getMangaId(), manga.getId(), manga.getLanguageCode())))
                .caption(getMangaText(manga)).build()));
        if (manga.getCoverFileId() == null) {
            mangaRepository.setCoverFileId(coverFileId, manga.getId());
        }
    }

    @Override
    public InlineKeyboardMarkup getMangaButtons(MangaButtonData mangaButtonData) {
        String whiteCheckMark = notificationEntityRepository.findByMangaIdAndUserId(mangaButtonData.getMangaId(), mangaButtonData.getUserId()) != null ? " :white_check_mark:" : "";
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В закладки")).callbackData(mangaDex + "\nchangeStatus\n" + mangaButtonData.getMangaDatabaseId() + "\n" + mangaButtonData.getLanguageCode()).build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Уведомления" + whiteCheckMark)).callbackData(mangaDex + "\nnotification\n" + mangaButtonData.getMangaDatabaseId() + "\n" + mangaButtonData.getLanguageCode()).build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к чтению")).switchInlineQueryCurrentChat(mangaDex + "\nmangaId:\n" + mangaButtonData.getMangaId() + "\n" + mangaButtonData.getLanguageCode()).build())
        )));
    }

    private String getMangaText(Manga manga) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b>").append(manga.getName()).append("</b>").append("\n\n");
        if (!manga.getReleaseDate().equals("0")) {
            stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(manga.getReleaseDate()).append("\n");
        }
        stringBuilder.append("<b>").append("Формат: ").append("</b>").append(manga.getFormat()).append("\n");
        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(manga.getStatus()).append("\n");
        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(manga.getGenres()).append("</i>\n\n");
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(manga.getDescription().replace("<", "").replace(">", ""));

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {
        Long mangaDatabaseId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        String languageCode = util.parseValue(callbackQuery.getData())[3];
        String mangaId = mangaRepository.findById(mangaDatabaseId).get().getMangaId();
        Long userId = callbackQuery.getFrom().getId();

        NotificationEntity notificationEntity = notificationEntityRepository.findByMangaDatabaseIdAndUserId(mangaDatabaseId, userId);
        if (notificationEntity != null) {
            notificationEntityRepository.delete(notificationEntity);
        } else {
            List<Map.Entry<String, String>> chapters = getChaptersFromSource(mangaId, languageCode);
            if (chapters == null) {
                util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", userId);
                return;
            }
            String lastChapter = util.parseValue(chapters.get(0).getKey())[1];
            notificationEntityRepository.save(new NotificationEntity(mangaId, mangaDatabaseId, callbackQuery.getFrom().getId(), mangaDex));
            if (notificationChapterMappingRepository.findByMangaDatabaseId(mangaDatabaseId) == null) {
                notificationChapterMappingRepository.save(new NotificationChapterMapping(mangaId, mangaDatabaseId, lastChapter, mangaDex));
            }
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Теперь ты будешь получать уведомление о выходе новых глав!")
                    .showAlert(true).build());
        }
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, mangaDatabaseId, languageCode)))
                .chatId(userId)
                .messageId(callbackQuery.getMessage().getMessageId()).build());
    }

    @Override
    public void getMangaChaptersButton(InlineQuery inlineQuery) {
        User user = userRepository.findByUserId(inlineQuery.getFrom().getId());
        String mangaId = util.parseValue(inlineQuery.getQuery())[2];
        String languageCode = util.parseValue(inlineQuery.getQuery())[3];
        Manga manga = mangaRepository.findByMangaIdAndCatalogNameAndLanguageCode(mangaId, mangaDex, languageCode);

        List<Map.Entry<String, String>> volChList = getChaptersFromSource(mangaId, languageCode);
        if (volChList == null) {
            util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", user.getUserId());
            return;
        }
        List<Chapter> sortedChapters = saveChapters(volChList, manga);
        mangaUtil.createAnswerInlineQueryButtons(inlineQuery, sortedChapters, user);
    }

    private List<Map.Entry<String, String>> getChaptersFromSource(String mangaId, String languageCode) {
        try {
            Map<String, List<String>> params = new HashMap<>();
            params.put("translatedLanguage[]", List.of(languageCode));
            Response response = mangaDexApiFeignClient.getChapterListAggregate(mangaId, params);
            if (response.status() != 200) {
                return null;
            }
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ChapterListAggregate chapterListAggregate = new ObjectMapper().readValue(jsonResponse, ChapterListAggregate.class);
            Map<String, String> volChMap = new LinkedHashMap<>();
            for (Volume volume : chapterListAggregate.getVolumes().values()) {
                for (ChapterMangaDex chapterMangaDex : volume.getChapters().values()) {
                    volChMap.put(volume.getVolume() + "\n" + chapterMangaDex.getChapter(), chapterMangaDex.getId());
                }
            }
            return new ArrayList<>(volChMap.entrySet());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    private List<Chapter> saveChapters(List<Map.Entry<String, String>> volChList, Manga manga) {
//        List<Chapter> chapterList = mangaChapterRepository.findAllByMangaIdAndCatalogNameAndLanguageCode(manga.getMangaId(), mangaDex, manga.getLanguageCode());
//        Map<String, Chapter> chapterMap = chapterList.stream()
//                .collect(Collectors.toMap(Chapter::getChapterId, chapter -> chapter));
//        Collections.reverse(volChList);
//
//        if (chapterList.isEmpty()) {
//            for (Map.Entry<String, String> stringEntry : volChList) {
//                Chapter chapter = getNewChapter(manga, stringEntry);
//                chapter.setType(manga.getType());
//                chapterList.add(chapter);
//            }
//
//            for (int i = 0; i < volChList.size(); i++) {
//                Chapter currentChapter = chapterList.get(i);
//                Chapter prevChapter = (i > 0) ? chapterList.get(i - 1) : null;
//                Chapter nextChapter = (i < chapterList.size() - 1) ? chapterList.get(i + 1) : null;
//                currentChapter.setPrevChapter(prevChapter);
//                currentChapter.setNextChapter(nextChapter);
////                try {
////                    chapterList.get(i).setPrevChapter(chapterList.get(i - 1));
////                } catch (IndexOutOfBoundsException e) {
////                    chapterList.get(i).setPrevChapter(null);
////                }
////
////                try {
////                    chapterList.get(i).setNextChapter(chapterList.get(i + 1));
////                } catch (IndexOutOfBoundsException e) {
////                    chapterList.get(i).setNextChapter(null);
////                }
//            }
//            mangaChapterRepository.saveAll(chapterList);
//        } else {
//            for (int i = 0; i < volChList.size(); i++) {
//                if (!chapterMap.containsKey(volChList.get(i).getValue())
//                        || chapterMap.get(volChList.get(i).getValue()).getNextChapter() == null
//                        || chapterMap.get(volChList.get(i).getValue()).getPrevChapter() == null) {
//                    Chapter currentChapter = mangaChapterRepository.findByChapterId(volChList.get(i).getValue());
//                    if (currentChapter == null) {
//                        currentChapter = getNewChapter(manga, volChList.get(i));
//                        mangaChapterRepository.save(currentChapter);
//                    }
//
//                    if (i != 0 && (currentChapter.getPrevChapter() == null || !currentChapter.getPrevChapter().getChapterId().equals(volChList.get(i - 1).getValue()))) {
//                        mangaChapterRepository.setPrevChapter(volChList.get(i - 1).getValue(), currentChapter.getId());
//                        try {
//                            mangaChapterRepository.setNextChapterByChapterId(currentChapter.getChapterId(), volChList.get(i - 1).getValue());
//                        } catch (IndexOutOfBoundsException e) {
//                            log.warn("Next Chapter not found");
//                        }
//                    }
//                    if (i != volChList.size() - 1 && (currentChapter.getNextChapter() == null || !currentChapter.getNextChapter().getChapterId().equals(volChList.get(i + 1).getValue()))) {
//                        Chapter nextChapter = mangaChapterRepository.findByChapterId(volChList.get(i + 1).getValue());
//                        if (nextChapter == null) {
//                            nextChapter = getNewChapter(manga, volChList.get(i + 1));
//                        }
//                        nextChapter.setChapterId(volChList.get(i + 1).getValue());
//                        try {
//                            nextChapter.setNextChapter(mangaChapterRepository.findByChapterId(volChList.get(i + 2).getValue()));
//                        } catch (IndexOutOfBoundsException e) {
//                            nextChapter.setNextChapter(null);
//                        }
//                        nextChapter.setPrevChapter(currentChapter);
//                        mangaChapterRepository.save(nextChapter);
//                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());
//
//                        try {
//                            mangaChapterRepository.setPrevChapterByChapterId(nextChapter.getChapterId(), volChList.get(i + 2).getValue());
//                        } catch (IndexOutOfBoundsException e) {
//                            log.warn("Prev Chapter not found");
//                        }
//                    }
//                }
//
//            }
//        }
//        return mangaChapterRepository.getChaptersInOrderWithLanguageCode(manga.getMangaId(), mangaDex, manga.getLanguageCode());
//    }


    private List<Chapter> saveChapters(List<Map.Entry<String, String>> volChList, Manga manga) {
        List<ChapterDto> chapterDtoList = mangaChapterRepository.findAllByMangaIdAndCatalogNameAndLanguageCode(manga.getMangaId(), mangaDex, manga.getLanguageCode());

        Map<String, Chapter> chapterMap = chapterDtoList.stream()
                .collect(Collectors.toMap(ChapterDto::getChapterId, chapterDto -> mangaUtil.convertToChapter(chapterDto)));

        Collections.reverse(volChList);

        if (chapterDtoList.isEmpty()) {
            List<Chapter> newChapters = new ArrayList<>();
            List<Chapter> saveChapters = new ArrayList<>();

            for (Map.Entry<String, String> stringEntry : volChList) {
                Chapter chapter = getNewChapter(manga, stringEntry);
                chapter.setType(manga.getType());
                newChapters.add(chapter);
            }
            mangaChapterRepository.saveAll(newChapters);
            for (int i = 0; i < newChapters.size(); i++) {
                Chapter currentChapter = newChapters.get(i);
                Chapter prevChapter = (i > 0) ? newChapters.get(i - 1) : null;
                Chapter nextChapter = (i < newChapters.size() - 1) ? newChapters.get(i + 1) : null;
                currentChapter.setPrevChapter(prevChapter);
                currentChapter.setNextChapter(nextChapter);
                saveChapters.add(currentChapter);
            }
            mangaChapterRepository.saveAllChapters(saveChapters);
        } else {
//            for (ChapterDto dto : chapterDtoList) {
//                if (dto.getNextChapterId() != null) {
//                    Chapter nextChapter = chapterMap.get(dto.getNextChapterId());
//                    chapterMap.get(dto.getChapterId()).setNextChapter(nextChapter);
//                }
//                if (dto.getPrevChapterId() != null) {
//                    Chapter prevChapter = chapterMap.get(dto.getPrevChapterId());
//                    chapterMap.get(dto.getChapterId()).setPrevChapter(prevChapter);
//                }
//            }
//
//            List<Chapter> chaptersToSave = new ArrayList<>();
//
//            for (int i = 0; i < volChList.size(); i++) {
//                String chapterId = volChList.get(i).getValue();
//
//                if (!chapterMap.containsKey(chapterId) ||
//                        chapterMap.get(chapterId).getNextChapter() == null ||
//                        chapterMap.get(chapterId).getPrevChapter() == null) {
//
//                    Chapter currentChapter = chapterMap.getOrDefault(chapterId, getNewChapter(manga, volChList.get(i)));
//                    chapterMap.putIfAbsent(chapterId, currentChapter);
//
//                    if (i != 0) {
//                        Chapter prevChapter = chapterMap.getOrDefault(volChList.get(i - 1).getValue(), null);
//                        if (prevChapter != null && (currentChapter.getPrevChapter() == null ||
//                                !currentChapter.getPrevChapter().getChapterId().equals(prevChapter.getChapterId()))) {
//                            currentChapter.setPrevChapter(prevChapter);
//                            prevChapter.setNextChapter(currentChapter);
//                            chaptersToSave.add(prevChapter);
//                        }
//                    }
//
//                    if (i != volChList.size() - 1) {
//                        Chapter nextChapter = chapterMap.getOrDefault(volChList.get(i + 1).getValue(), getNewChapter(manga, volChList.get(i + 1)));
//                        if (nextChapter != null && (currentChapter.getNextChapter() == null ||
//                                !currentChapter.getNextChapter().getChapterId().equals(nextChapter.getChapterId()))) {
//                            currentChapter.setNextChapter(nextChapter);
//                            nextChapter.setPrevChapter(currentChapter);
//                            chaptersToSave.add(nextChapter);
//                            chapterMap.putIfAbsent(volChList.get(i + 1).getValue(), nextChapter);
//                        }
//                    }
//
//                    chaptersToSave.add(currentChapter);
//                }
//            }
//            mangaChapterRepository.saveAllChapters(chaptersToSave);
            for (ChapterDto dto : chapterDtoList) {
                if (dto.getNextChapterId() != null) {
                    Chapter nextChapter = chapterMap.get(dto.getNextChapterId());
                    chapterMap.get(dto.getChapterId()).setNextChapter(nextChapter);
                }
                if (dto.getPrevChapterId() != null) {
                    Chapter prevChapter = chapterMap.get(dto.getPrevChapterId());
                    chapterMap.get(dto.getChapterId()).setPrevChapter(prevChapter);
                }
//                if (dto.getMangaDataBaseId() == null) {
//                    mangaChapterRepository.setMangaDatabaseIdAndType(manga.getId(), manga.getType(), dto.getId());
//                }
            }
            for (int i = 0; i < volChList.size(); i++) {
                if (!chapterMap.containsKey(volChList.get(i).getValue())
                        || chapterMap.get(volChList.get(i).getValue()).getNextChapter() == null
                        || chapterMap.get(volChList.get(i).getValue()).getPrevChapter() == null) {
                    //Chapter currentChapter = mangaChapterRepository.findByChapterId(volChList.get(i).getValue());
                    Chapter currentChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(volChList.get(i).getValue()));

                    if (currentChapter == null) {
                        currentChapter = getNewChapter(manga, volChList.get(i));
                        mangaChapterRepository.save(currentChapter);
                    }

                    if (i != 0 && (currentChapter.getPrevChapter() == null || !currentChapter.getPrevChapter().getChapterId().equals(volChList.get(i - 1).getValue()))) {
                        mangaChapterRepository.setPrevChapter(volChList.get(i - 1).getValue(), currentChapter.getId());
                        try {
                            mangaChapterRepository.setNextChapterByChapterId(currentChapter.getChapterId(), volChList.get(i - 1).getValue());
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Next Chapter not found");
                        }
                    }
                    if (i != volChList.size() - 1 && (currentChapter.getNextChapter() == null || !currentChapter.getNextChapter().getChapterId().equals(volChList.get(i + 1).getValue()))) {
                        //Chapter nextChapter = mangaChapterRepository.findByChapterId(volChList.get(i + 1).getValue());
                        Chapter nextChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(volChList.get(i + 1).getValue()));
                        if (nextChapter == null) {
                            nextChapter = getNewChapter(manga, volChList.get(i + 1));
                        }
                        nextChapter.setChapterId(volChList.get(i + 1).getValue());
                        mangaChapterRepository.save(nextChapter);

                        try {
                            mangaChapterRepository.setNextChapter(mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(volChList.get(i + 2).getValue())).getChapterId(), nextChapter.getId());
                        } catch (IndexOutOfBoundsException | NullPointerException e) {
                            mangaChapterRepository.setNextChapter(null, nextChapter.getId());
                        }

                        mangaChapterRepository.setPrevChapter(currentChapter.getChapterId(), nextChapter.getId());
                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());

//                        nextChapter.setPrevChapter(currentChapter);
//                        mangaChapterRepository.save(nextChapter);
//                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());

                        try {
                            mangaChapterRepository.setPrevChapterByChapterId(nextChapter.getChapterId(), volChList.get(i + 2).getValue());
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Prev Chapter not found");
                        }
                    }
                }

            }
        }
        List<ChapterDto> chapters = mangaChapterRepository.findAllByMangaIdAndCatalogNameAndLanguageCode(manga.getMangaId(), mangaDex, manga.getLanguageCode());
        Map<String, Chapter> chapterMapResult = chapters.stream()
                .collect(Collectors.toMap(ChapterDto::getChapterId, chapterDto -> mangaUtil.convertToChapter(chapterDto)));
        Collections.reverse(volChList);

        List<Chapter> chaptersResult = new ArrayList<>();
        for (ChapterDto dto : chapters) {
            if (dto.getNextChapterId() != null) {
                Chapter nextChapter = chapterMapResult.get(dto.getNextChapterId());
                chapterMapResult.get(dto.getChapterId()).setNextChapter(nextChapter);
            }
            if (dto.getPrevChapterId() != null) {
                Chapter prevChapter = chapterMapResult.get(dto.getPrevChapterId());
                chapterMapResult.get(dto.getChapterId()).setPrevChapter(prevChapter);
            }
            chaptersResult.add(chapterMapResult.get(dto.getChapterId()));
        }
        return mangaUtil.getChaptersInOrder(chaptersResult);
        //return mangaChapterRepository.getChaptersInOrderWithLanguageCode(manga.getMangaId(), mangaDex, manga.getLanguageCode());
    }

    private Chapter getNewChapter(Manga manga, Map.Entry<String, String> stringEntry) {
        return new Chapter(mangaDex, manga.getMangaId(), stringEntry.getValue(),
                manga.getName(), stringEntry.getKey().split("\n")[0], stringEntry.getKey().split("\n")[1],
                new Timestamp(System.currentTimeMillis()), manga.getFormat(), manga.getId(), manga.getType(), manga.getLanguageCode());
    }

    private List<String> getUrlPageList(String chapterId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Response response = mangaDexApiFeignClient.getChapterPageIds(chapterId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ChapterUrlResponse chapterUrlResponse = objectMapper.readValue(jsonResponse, ChapterUrlResponse.class);
            String hash = chapterUrlResponse.getChapter().getHash();
            List<String> urlList = new ArrayList<>();
            for (String pageUrl : chapterUrlResponse.getChapter().getData()) {
                urlList.add("https://uploads.mangadex.org/data/" + hash + "/" + pageUrl);
            }
            return urlList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void preloadMangaChapter(Long userId, Chapter chapter) {
        try {
            Chapter nextChapter = chapter.getNextChapter();
            if (nextChapter == null || (nextChapter.getTelegraphStatusDownload() != null && nextChapter.getTelegraphStatusDownload().equals("finished")) || (nextChapter.getTelegraphStatusDownload() != null && nextChapter.getTelegraphStatusDownload().equals("process"))) {
                return;
            }

            List<String> urlList = getUrlPageList(chapter.getNextChapter().getChapterId());
            if (urlList.isEmpty()) {
                return;
            }
            List<Node> content = new ArrayList<>();
            for (String url : urlList) {
                content.add(mangaUtil.createImage(url));
            }

            mangaChapterRepository.setTelegraphStatusDownload("process", nextChapter.getId());


            CreatePage createPage = new CreatePage(telegraphApiToken, nextChapter.getName() + " Vol " + nextChapter.getVol() + ". Chapter " + nextChapter.getChapter(), content)
                    .setAuthorName(telegraphAuthorName)
                    .setAuthorUrl(telegraphAuthorUrl)
                    .setReturnContent(true);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Response response = telegraphApiFeignClient.createPage(createPage);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            PageResponse result = objectMapper.readValue(jsonResponse, PageResponse.class);
            Page page = result.getResult();

            List<MessageEntity> messageEntityList = new ArrayList<>();
            messageEntityList.add(MessageEntity.builder()
                    .type("bold")
                    .length(nextChapter.getName().length())
                    .offset(0).build());

            messageEntityList.add(MessageEntity.builder()
                    .type("text_link")
                    .url(page.getUrl())
                    .length(nextChapter.getName().length())
                    .offset(0).build());

            Integer messageId = telegramSender.send(SendMessage.builder()
                    .text(nextChapter.getName() + "\n" + "Том " + nextChapter.getVol() + ". Глава " + nextChapter.getChapter())
                    .chatId(-1002092468371L)
                    .entities(messageEntityList).build()).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setTelegraphMessageId(messageId, nextChapter.getId());
                mangaChapterRepository.setTelegraphStatusDownload("finished", nextChapter.getId());
                mangaChapterRepository.setTelegraphUrl(page.getUrl(), nextChapter.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void preloadManhwaChapter(Long userId, Chapter chapter) {
        try {
            Chapter nextChapter = chapter.getNextChapter();
            if (nextChapter == null || (nextChapter.getStatus() != null && nextChapter.getStatus().equals("finished")) || (nextChapter.getStatus() != null && nextChapter.getStatus().equals("process"))) {
                return;
            }
            mangaChapterRepository.setPdfStatusDownload("process", nextChapter.getId());

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            List<String> urlList = getUrlPageList(nextChapter.getChapterId());

            if (urlList == null || urlList.isEmpty()) {
                return;
            }

            File pdfFolder = util.createStorageFolder("TempPdfStorage");
            String pdfFileName = pdfFolder + File.separator + nextChapter.getName().replace(" ", "_").replace(".", "_").replace("/", "_") + "_Vol_" + nextChapter.getVol().replace(".", "_") + "_Chapter_" + nextChapter.getChapter().replace(".", "_") + "_From_" + userId + "_" + dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (int i = 0; i < urlList.size(); i++) {
                if (urlList.get(i).endsWith(".webp") || urlList.get(i).endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFileWithoutReferrer(folder, new URL(urlList.get(i)), "temp_img" + nextChapter.getName().replace(" ", "_") + "_Vol_" + nextChapter.getVol() + "_Chapter_" + nextChapter.getChapter() + "_Page_" + urlList.get(i) + "_From_" + userId + ".webp");
                    File jpegFile = mangaUtil.getJpeg(folder, file, nextChapter.getName().replace(" ", "_") + "_Vol_" + nextChapter.getVol() + "_Chapter_" + nextChapter.getChapter() + "_Page_" + i + "_From_" + userId);
                    ImageData imgData = ImageDataFactory.create(jpegFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    jpegFile.delete();
                } else {
                    ImageData imgData = util.downloadImage(urlList.get(i));
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();
            File pdfFile = compressImages(pdfFileName, nextChapter, userId, 0.9, urlList);

            Integer messageId = telegramSender.sendDocument(SendDocument.builder()
                    .document(new InputFile(pdfFile))
                    .caption(nextChapter.getName() + "\n" + "Том " + nextChapter.getVol() + ". Глава " + nextChapter.getChapter())
                    .chatId(-1002092468371L).build()).getMessageId();
            pdfFile.delete();

            if (messageId != null) {
                mangaChapterRepository.setPdfMessageId(messageId, nextChapter.getId());
                mangaChapterRepository.setPdfStatusDownload("finished", nextChapter.getId());
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private File compressImages(String pdfFileName, Chapter chapter, Long userId, double compressParam, List<String> urlList) {
        try {
            File pdfFile = new File(pdfFileName);
            if (pdfFile.length() >= 52000000) {
                FFmpeg ffmpeg = new FFmpeg();
                FFprobe ffprobe = new FFprobe();
                pdfFile.delete();
                pdfFile = new File(pdfFileName);
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(pdfFileName));
                Document doc = new Document(pdfDoc);
                for (int i = 0; i < urlList.size(); i++) {
                    String fileName = chapter.getName().replace(" ", "_").replace("/", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + i + "_From_" + userId;
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFileWithoutReferrer(folder, new URL(urlList.get(i)), fileName);
                    if (urlList.get(i).endsWith(".webp") || urlList.get(i).endsWith(".WEBP")) {
                        FFmpegStream photoStream = ffprobe.probe(file.getPath()).getStreams().get(0);
                        FFmpegBuilder builder = new FFmpegBuilder()
                                .setInput(file.getPath())
                                .overrideOutputFiles(true)
                                .addOutput(folder + File.separator + fileName + "output.jpeg")
                                .setVideoCodec("mjpeg")
                                .setVideoResolution(photoStream.width, photoStream.height)
                                .setVideoFilter("scale=iw*" + compressParam + ":ih*" + compressParam)
                                .done();
                        mangaUtil.executeBuilder(ffmpeg, ffprobe, pdfDoc, doc, fileName, file, builder, folder);
                    } else {
                        FFmpegBuilder builder = new FFmpegBuilder()
                                .setInput(file.getPath())
                                .overrideOutputFiles(true)
                                .addOutput(folder + File.separator + fileName + "output.jpeg")
                                .setFormat("mjpeg")
                                .setVideoFilter("scale=iw*" + compressParam + ":ih*" + compressParam)
                                .done();
                        mangaUtil.executeBuilder(ffmpeg, ffprobe, pdfDoc, doc, fileName, file, builder, folder);
                    }
                }
                doc.close();
            } else {
                return pdfFile;
            }
            if (pdfFile.length() >= 52000000 && compressParam >= 0.3) {
                compressImages(pdfFileName, chapter, userId, compressParam - 0.1, urlList);
            }
            return pdfFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void sendTelegraphArticle(Long userId, Chapter chapter) {
        List<String> urlList = getUrlPageList(chapter.getChapterId());
        if (urlList == null || urlList.isEmpty()) {
            util.sendErrorMessage("Данная глава доступна только на странице создателя, к сожалению это можно проверить только при ее загрузке, ссылки на все главы можно найти тут: \nhttps://mangadex.org/title/" + chapter.getMangaId() + "/", userId);
            return;
        }
        try {
            List<Node> content = new ArrayList<>();
            for (String url : urlList) {
                content.add(mangaUtil.createImage(url));
            }

            mangaChapterRepository.setTelegraphStatusDownload("process", chapter.getId());

            CreatePage createPage = new CreatePage(telegraphApiToken, chapter.getName() + " Vol " + chapter.getVol() + ". Chapter " + chapter.getChapter(), content)
                    .setAuthorName(telegraphAuthorName)
                    .setAuthorUrl(telegraphAuthorUrl)
                    .setReturnContent(true);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Response response = telegraphApiFeignClient.createPage(createPage);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            PageResponse result = objectMapper.readValue(jsonResponse, PageResponse.class);
            Page page = result.getResult();

            List<MessageEntity> messageEntityList = new ArrayList<>();
            messageEntityList.add(MessageEntity.builder()
                    .type("bold")
                    .length(chapter.getName().length())
                    .offset(0).build());

            messageEntityList.add(MessageEntity.builder()
                    .type("text_link")
                    .url(page.getUrl())
                    .length(chapter.getName().length())
                    .offset(0).build());

            SendMessage sendMessage = new SendMessage("-1002092468371L", chapter.getName() + "\n" + "Том " + chapter.getVol() + ". Глава " + chapter.getChapter());
            sendMessage.setEntities(messageEntityList);

            InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(chapter, userId);

            Integer messageId = telegramSender.send(sendMessage).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setTelegraphMessageId(messageId, chapter.getId());
                mangaChapterRepository.setTelegraphStatusDownload("finished", chapter.getId());
                mangaChapterRepository.setTelegraphUrl(page.getUrl(), chapter.getId());

                if (inlineKeyboardMarkup != null) {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .replyMarkup(inlineKeyboardMarkup)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                } else {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPDFChapter(Long userId, Chapter chapter) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            List<String> urlList = getUrlPageList(chapter.getChapterId());

            if (urlList == null || urlList.isEmpty()) {
                util.sendErrorMessage("Данная глава доступна только на странице создателя, к сожалению это можно проверить только при ее загрузке, ссылку на главы можно найти тут: \nhttps://mangadex.org/title/" + chapter.getMangaId() + "/", userId);
                return;
            }
            mangaChapterRepository.setPdfStatusDownload("process", chapter.getId());

            File pdfFolder = util.createStorageFolder("TempPdfStorage");

            Integer messageIdForDelete = mangaUtil.sendWaitGIFAndAction(userId);
            String pdfFileName = pdfFolder + File.separator + chapter.getName().replace(" ", "_").replace(".", "_").replace("/", "_") + "_Vol_" + chapter.getVol().replace(".", "_") + "_Chapter_" + chapter.getChapter().replace(".", "_") + "_From_" + userId + "_" + dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (int i = 0; i < urlList.size(); i++) {
                if (urlList.get(i).endsWith(".webp") || urlList.get(i).endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");

                    File file = util.downloadFileWithoutReferrer(folder, new URL(urlList.get(i)), "temp_img" + chapter.getName().replace(" ", "_").replace("/", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + urlList.get(i) + "_From_" + userId + ".webp");
                    File jpegFile = mangaUtil.getJpeg(folder, file, chapter.getName().replace(" ", "_").replace("/", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + i + "_From_" + userId);
                    ImageData imgData = ImageDataFactory.create(jpegFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    jpegFile.delete();
                } else {
                    ImageData imgData = util.downloadImage(urlList.get(i));
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();
            File pdfFile = compressImages(pdfFileName, chapter, userId, 0.9, urlList);
            SendDocument sendDocument = new SendDocument("-1002092468371L", new InputFile(pdfFile));
            sendDocument.setCaption(chapter.getName() + "\n" + "Том " + chapter.getVol() + ". Глава " + chapter.getChapter());
            InlineKeyboardMarkup inlineKeyboardMarkup = getPrevNextButtons(chapter, userId);

            Integer messageId = telegramSender.sendDocument(sendDocument).getMessageId();

            if (messageId != null) {
                mangaChapterRepository.setPdfMessageId(messageId, chapter.getId());
                mangaChapterRepository.setPdfStatusDownload("finished", chapter.getId());

                if (inlineKeyboardMarkup != null) {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .replyMarkup(inlineKeyboardMarkup)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                } else {
                    telegramSender.sendCopyMessageFromStorage(CopyMessage.builder()
                            .messageId(messageId)
                            .chatId(userId)
                            .fromChatId(-1002092468371L).build());
                }
            }
            pdfFile.delete();

            telegramSender.deleteMessageById(String.valueOf(userId), messageIdForDelete);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public InlineKeyboardMarkup getPrevNextButtons(Chapter chapter, Long userId) {
        InlineKeyboardMarkup inlineKeyboardMarkup;
        //String readStatus = readStatusRepository.findByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, mangaDex) != null ? "✅" : "\uD83D\uDCD5";
        String readStatus = readStatusRepository.existsByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, mangaDex) ? "✅" : "\uD83D\uDCD5";

        if (chapter.getPrevChapter() == null && chapter.getNextChapter() == null) {
            inlineKeyboardMarkup = null;
        } else if (chapter.getPrevChapter() == null) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat(mangaDex + "\nmangaId:\n" + chapter.getMangaId() + "\n" + chapter.getLanguageCode()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData(mangaDex + "\nreadStatus\n" + chapter.getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующая глава")).callbackData(mangaDex + "\nnextChapter\n" + chapter.getNextChapter().getId()).build())
            )));
        } else if (chapter.getNextChapter() == null) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat(mangaDex + "\nmangaId:\n" + chapter.getMangaId() + "\n" + chapter.getLanguageCode()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущая глава")).callbackData(mangaDex + "\nprevChapter\n" + chapter.getPrevChapter().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData(mangaDex + "\nreadStatus\n" + chapter.getId()).build())
            )));
        } else {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat(mangaDex + "\nmangaId:\n" + chapter.getMangaId() + "\n" + chapter.getLanguageCode()).build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData(mangaDex + "\nprevChapter\n" + chapter.getPrevChapter().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData(mangaDex + "\nreadStatus\n" + chapter.getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData(mangaDex + "\nnextChapter\n" + chapter.getNextChapter().getId()).build())
            )));
        }
        return inlineKeyboardMarkup;
    }

    @Override
    public void getRandomManga(Long userId) {
        util.sendInfoMessage("К сожалению, для этого каталога случайная манга недоступна, я как можно скорее постараюсь заменить этот пункт на что-то другое, спасибо за понимание.", userId);
    }

    public void sendNotificationAboutNewChapter() {
        Map<NotificationEntity, List<Long>> prepareSendList = notificationEntityRepository.findAllByCatalogName(mangaDex).stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.mapping(NotificationEntity::getUserId, Collectors.toList())
                ));
        for (NotificationEntity notificationEntity : prepareSendList.keySet()) {
            Manga manga = mangaRepository.findById(notificationEntity.getMangaDatabaseId()).orElseThrow();
            List<Map.Entry<String, String>> chapters = getChaptersFromSource(notificationEntity.getMangaId(), manga.getLanguageCode());
            if (chapters == null) {
                log.error("Возникла ошибка при получении глав сайта mangaDex.org, id манги: " + notificationEntity.getMangaId());
                continue;
            }
            MangaDataMangaDex mangaData = getMangaData(notificationEntity.getMangaId());
            if (mangaData == null) {
                log.error("Возникла ошибка при получении MangaData сайта mangaDex.org, id манги: " + notificationEntity.getMangaId());
                continue;
            }

            String lastChapter = util.parseValue(chapters.get(0).getKey())[1];
            NotificationChapterMapping chapterMapping = notificationChapterMappingRepository.findByMangaDatabaseId(manga.getId());
            if (chapterMapping != null && !chapterMapping.getChapter().equals(lastChapter)) {
                notificationChapterMappingRepository.setChapter(lastChapter, manga.getId());
                senderService.sendNotificationToUsers(prepareSendList.get(notificationEntity), manga, lastChapter);
            }
        }
    }


    private String getYear(int year) {
        if (year != 0) {
            return "Год: " + year;
        } else {
            return "";
        }
    }

    private String getTitle(Map<String, String> titleMap, String languageCodeForCatalog) {
        if (titleMap.isEmpty()) {
            return " ";
        }
        return titleMap.getOrDefault(languageCodeForCatalog, titleMap.getOrDefault("en", titleMap.getOrDefault("ja", titleMap.values().iterator().next())));
    }

    private String getDescription(Map<String, String> titleMap, String languageCodeForCatalog) {
        if (titleMap.isEmpty()) {
            return " ";
        }
        return titleMap.getOrDefault(languageCodeForCatalog, titleMap.getOrDefault("en", titleMap.getOrDefault("ja", titleMap.values().iterator().next())));
    }

    private String getCoverMangaDex(List<MangaRelationship> relationships, String mangaId, String pix) {
        MangaRelationship coverArtRelationship = relationships.stream()
                .filter(relationship -> "cover_art".equals(relationship.getType()))
                .findFirst().get();
        return "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverArtRelationship.getAttributes().getFileName() + "." + pix + ".jpg";

    }

    private String getGenres(List<MangaTag> tags, String languageCode) {
        StringBuilder stringBuilder = new StringBuilder();
        for (MangaTag mangaTag : tags) {
            if (mangaTag.getAttributes().getGroup().equals("genre")) {
                stringBuilder.append(mangaTag.getAttributes().getName().getOrDefault(languageCode, mangaTag.getAttributes().getName().getOrDefault("en", mangaTag.getAttributes().getName().values().iterator().next()))).append(" ");
            }
        }
        return stringBuilder.toString();
    }

    private String getFormat(List<MangaTag> tags, String languageCode) {
        if (tags.isEmpty()) {
            return " ";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (MangaTag mangaTag : tags) {
            if (mangaTag.getAttributes().getGroup().equals("format")) {
                stringBuilder.append(mangaTag.getAttributes().getName().getOrDefault(languageCode, mangaTag.getAttributes().getName().getOrDefault("en", mangaTag.getAttributes().getName().values().iterator().next()))).append(" ");
            }
        }
        return stringBuilder.toString();
    }
}
