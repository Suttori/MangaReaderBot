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
import com.suttori.entity.MangaDesu.*;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.DesuMeApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.telegram.TelegraphApiFeignClient;
import com.suttori.util.MangaUtil;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class DesuMeService implements MangaServiceInterface<MangaDataDesu> {

    @Value("${telegraphApiToken}")
    private String telegraphApiToken;
    @Value("${telegraphAuthorName}")
    private String telegraphAuthorName;
    @Value("${telegraphAuthorUrl}")
    private String telegraphAuthorUrl;
    @Value("${desuMe}")
    private String desuMe;

    private DesuMeApiFeignClient desuMeApiFeignClient;
    private TelegramSender telegramSender;
    private Util util;
    private MangaUtil mangaUtil;
    private SenderService senderService;
    private AwsServerService awsServerService;

    private NotificationEntityRepository notificationEntityRepository;
    private UserRepository userRepository;
    private MangaChapterRepository mangaChapterRepository;
    private NotificationChapterMappingRepository notificationChapterMappingRepository;
    private MangaRepository mangaRepository;
    private ReadStatusRepository readStatusRepository;
    private UserFilterPreferencesRepository userFilterPreferencesRepository;
    private AwsUrlRepository awsUrlRepository;
    private PhotoProcessingService photoProcessingService;

    private TelegraphApiFeignClient telegraphApiFeignClient;

    @Autowired
    public DesuMeService(DesuMeApiFeignClient desuMeApiFeignClient, TelegramSender telegramSender, Util util, MangaUtil mangaUtil, SenderService senderService, AwsServerService awsServerService, NotificationEntityRepository notificationEntityRepository, UserRepository userRepository, MangaChapterRepository mangaChapterRepository, NotificationChapterMappingRepository notificationChapterMappingRepository, MangaRepository mangaRepository, ReadStatusRepository readStatusRepository, UserFilterPreferencesRepository userFilterPreferencesRepository, AwsUrlRepository awsUrlRepository, PhotoProcessingService photoProcessingService, TelegraphApiFeignClient telegraphApiFeignClient) {
        this.desuMeApiFeignClient = desuMeApiFeignClient;
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaUtil = mangaUtil;
        this.senderService = senderService;
        this.awsServerService = awsServerService;
        this.notificationEntityRepository = notificationEntityRepository;
        this.userRepository = userRepository;
        this.mangaChapterRepository = mangaChapterRepository;
        this.notificationChapterMappingRepository = notificationChapterMappingRepository;
        this.mangaRepository = mangaRepository;
        this.readStatusRepository = readStatusRepository;
        this.userFilterPreferencesRepository = userFilterPreferencesRepository;
        this.awsUrlRepository = awsUrlRepository;
        this.photoProcessingService = photoProcessingService;
        this.telegraphApiFeignClient = telegraphApiFeignClient;
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
    public void getSearchResult(InlineQuery inlineQuery, User user) {
        try {
            int offset = 30;
            if (!inlineQuery.getOffset().isEmpty()) {
                offset = Integer.parseInt(inlineQuery.getOffset());
            }

            String page = String.valueOf(offset / 30);
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, String> searchParams = getSearchParams(inlineQuery, page);

            boolean hasYaoi = searchParams.values().stream()
                    .anyMatch(value -> value != null && value.toLowerCase().contains("yaoi"));

            if (hasYaoi) {
                Sort sort = Sort.by(Sort.Direction.DESC, "rating");
                Pageable pageable = PageRequest.of(offset / 30 - 1, 30, sort);
                List<Manga> mangas = mangaRepository.findAllByGenresContainingAndCatalogName("Яой", desuMe, pageable);
                List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
                int i = 0;
                for (Manga manga : mangas) {
                    inlineQueryResultList.add(InlineQueryResultArticle.builder()
                            .id(inlineQuery.getFrom().getId() + "" + i++)
                            .title(manga.getName())
                            .description(getDescriptionForSearchResultFromDb(manga))
                            .thumbnailUrl(manga.getCoverUrl().replace("desu.me", "x.desu.city"))
                            .inputMessageContent(new InputTextMessageContent(desuMe + "\nmangaId\n" + manga.getMangaId())).build());
                }
                telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                        .results(inlineQueryResultList)
                        .nextOffset(String.valueOf(offset + 30))
                        .cacheTime(1)
                        .isPersonal(true)
                        .inlineQueryId(inlineQuery.getId()).build());
                return;
            }

            Response response = desuMeApiFeignClient.searchManga(searchParams);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaSearchResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaSearchResponse.class);

            if (mangaResponse.getResponse() == null) {
                return;
            }

            List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
            int i = 0;
            for (MangaDataAsSearchResult mangaData : mangaResponse.getResponse()) {
                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(mangaData.getRussian())
                        .description(getDescriptionForSearchResult(mangaData))
                        .thumbnailUrl(mangaData.getImage().getOriginal().replace("desu.me", "x.desu.city"))
                        .inputMessageContent(new InputTextMessageContent(desuMe + "\nmangaId\n" + mangaData.getId())).build());
            }
            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(inlineQueryResultList)
                    .nextOffset(String.valueOf(offset + 30))
                    .cacheTime(1)
                    .isPersonal(true)
                    .inlineQueryId(inlineQuery.getId()).build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getSearchParams(InlineQuery inlineQuery, String page) {
        Map<String, List<String>> filterPreferences = getUserFilterPreferences(inlineQuery.getFrom().getId(), desuMe);
        Map<String, String> params = new HashMap<>();
        params.put("page", page);
        params.put("limit", "30");

        UserSortPreferences userSortPreferences = mangaUtil.getUserSortPreferences(inlineQuery.getFrom().getId(), desuMe);
        if (userSortPreferences != null) {
            params.put(userSortPreferences.getSortName(), userSortPreferences.getSortType());
        }

        if (!filterPreferences.isEmpty()) {
            for (Map.Entry<String, List<String>> filterPreference : filterPreferences.entrySet()) {
                params.put(filterPreference.getKey(), String.join(",", filterPreference.getValue()));
            }
        }

        params.put("search", inlineQuery.getQuery());
        return params;
    }

    private Map<String, List<String>> getUserFilterPreferences(Long userId, String catalogName) {
        List<UserFilterPreference> filterPreferenceList = userFilterPreferencesRepository.findAllByUserIdAndCatalogName(userId, catalogName);
        if (!filterPreferenceList.isEmpty()) {
            return filterPreferenceList.stream()
                    .collect(Collectors.groupingBy(
                            UserFilterPreference::getFilterType,
                            Collectors.mapping(UserFilterPreference::getFilterValue, Collectors.toList())
                    ));
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public void sendMangaById(Long userId, String mangaId) {
        Manga manga = mangaRepository.findByMangaIdAndCatalogName(mangaId, desuMe);
        MangaDataDesu mangaDataDesu = getMangaData(mangaId);
        if (mangaDataDesu == null) {
            util.sendErrorMessage("Произошла ошибка при запросе манги у сайта desu.me, попробуй еще раз и, если ошибка повторится, то напиши в поддержку", userId);
            log.error("Сайт desu.me вернул null. MangaData для метода sendMangaById не найдена. Id манги: " + mangaId);
            return;
        }

        if (mangaDataDesu.getChapters() == null) {
            util.sendErrorMessage("Произошла ошибка при запросе глав у сайта desu.me, скорее всего на сайте не добавлено ни одной главы, ты можешь это проверить и если это не так, то напиши в поддержку", userId);
            log.error("Сайт desu.me вернул пустой список глав. Id манги: " + mangaId);
            return;
        }

        if (manga == null) {
            manga = saveManga(mangaDataDesu);
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverUrl()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId())))
                    .caption(getMangaText(manga)).build()));
                   //.caption(getMangaText(manga).replace("<","\"").replace(">", "\"")).build()));
            mangaRepository.setCoverFileId(coverFileId, manga.getId());
        } else {
            String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile(manga.getCoverUrl()))
                    .chatId(userId)
                    .parseMode("HTML")
                    .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId())))
                    .caption(getMangaText(manga)).build()));
                    //.caption(getMangaText(manga).replace("<","\"").replace(">", "\"")).build()));
            if (manga.getCoverFileId() == null) {
                mangaRepository.setCoverFileId(coverFileId, manga.getId());
            }
            if (manga.getCoverUrl() == null) {
                mangaRepository.setCoverUrl(mangaDataDesu.getImage().getOriginal(), manga.getId());
            }
            mangaRepository.setNumberOfChapters(mangaDataDesu.getChapters().getLast().getCh(), manga.getId());
        }
    }

    @Override
    public void sendMangaByDatabaseId(Long userId, String mangaDatabaseId) {
        Manga manga = mangaRepository.findById(Long.valueOf(mangaDatabaseId)).orElseThrow();
        String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(manga.getCoverFileId()))
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(getMangaButtons(new MangaButtonData(userId, manga.getMangaId(), manga.getId())))
                .caption(getMangaText(manga)).build()));
        if (manga.getCoverFileId() == null) {
            mangaRepository.setCoverFileId(coverFileId, manga.getId());
        }
    }

    private Manga saveManga(MangaDataDesu mangaData) {
        return mangaRepository.save(new Manga(mangaData.getImage().getOriginal(), String.valueOf(mangaData.getId()), desuMe,
                mangaData.getRussian(), mangaData.getKind(), mangaData.getStatus(),
                getGenres(mangaData.getGenres()), mangaData.getDescription(),
                new SimpleDateFormat("yyyy").format(new Date(mangaData.getAired_on() * 1000)), String.valueOf(mangaData.getScore()), mangaData.getChapters().getLast().getCh(), mangaData.getKind(), null));
    }

    @Override
    public InlineKeyboardMarkup getMangaButtons(MangaButtonData mangaButtonData) {
        String whiteCheckMark = notificationEntityRepository.findByMangaIdAndUserId(mangaButtonData.getMangaId(), mangaButtonData.getUserId()) != null ? " :white_check_mark:" : "";
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В закладки")).callbackData(desuMe + "\nchangeStatus\n" + mangaButtonData.getMangaDatabaseId() + "\n" + mangaButtonData.getMangaId()).build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Уведомления" + whiteCheckMark)).callbackData(desuMe + "\nnotification\n" + mangaButtonData.getMangaDatabaseId()).build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к чтению")).switchInlineQueryCurrentChat(desuMe + "\nmangaId:\n" + mangaButtonData.getMangaId()).build())
        )));
    }

    private MangaDataDesu getMangaData(String mangaId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Response response = desuMeApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getResponse();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {
        Long mangaDatabaseId = Long.valueOf(util.parseValue(callbackQuery.getData())[2]);
        String mangaId = mangaRepository.findById(mangaDatabaseId).orElseThrow().getMangaId();
        Long userId = callbackQuery.getFrom().getId();

        NotificationEntity notificationEntity = notificationEntityRepository.findByMangaIdAndUserId(mangaId, userId);
        if (notificationEntity != null) {
            notificationEntityRepository.delete(notificationEntity);
        } else {
            MangaDataDesu mangaData = getMangaData(mangaId);
            if (mangaData == null) {
                util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", userId);
                return;
            }
            String lastChapter = mangaData.getChapters().getLast().getCh();
            notificationEntityRepository.save(new NotificationEntity(mangaId, mangaDatabaseId, callbackQuery.getFrom().getId(), desuMe));
            if (notificationChapterMappingRepository.findByMangaIdAndCatalogName(mangaId, desuMe) == null) {
                notificationChapterMappingRepository.save(new NotificationChapterMapping(String.valueOf(mangaId), mangaDatabaseId, lastChapter, desuMe));
            }
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Теперь ты будешь получать уведомление о выходе новых глав!")
                    .showAlert(true).build());
        }
        telegramSender.sendEditMessageReplyMarkup(EditMessageReplyMarkup.builder()
                .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, mangaDatabaseId)))
                .chatId(userId)
                .messageId(callbackQuery.getMessage().getMessageId()).build());
    }

    @Override
    public void getMangaChaptersButton(InlineQuery inlineQuery) {
        User user = userRepository.findByUserId(inlineQuery.getFrom().getId());
        String mangaId = util.parseValue(inlineQuery.getQuery())[2];
        Manga manga = mangaRepository.findByMangaIdAndCatalogName(mangaId, desuMe);

        List<MangaChapterItem> mangaChapterItems = getChaptersFromSource(mangaId);
        if (mangaChapterItems == null || mangaChapterItems.isEmpty()) {
            util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", user.getUserId());
            return;
        }
        List<Chapter> sortedChapters = saveChapters(mangaChapterItems, manga);
        mangaUtil.createAnswerInlineQueryButtons(inlineQuery, sortedChapters, user);
    }

    private List<MangaChapterItem> getChaptersFromSource(String mangaId) {
        try {
            Response response = desuMeApiFeignClient.getMangaById(mangaId);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = new ObjectMapper().readValue(jsonResponse, MangaResponse.class);
            MangaDataDesu mangaDataDesu = mangaResponse.getResponse();
            return mangaDataDesu.getChapters().getList();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Chapter> saveChapters(List<MangaChapterItem> mangaChapterItems, Manga manga) {
        List<ChapterDto> chapterDtoList = mangaChapterRepository.findAllByMangaIdAndCatalogName(manga.getMangaId(), desuMe);

        Map<String, Chapter> chapterMap = chapterDtoList.stream()
                .collect(Collectors.toMap(ChapterDto::getChapterId, chapterDto -> mangaUtil.convertToChapter(chapterDto)));
        Collections.reverse(mangaChapterItems);

        if (chapterDtoList.isEmpty()) {
            List<Chapter> newChapters = new ArrayList<>();
            List<Chapter> saveChapters = new ArrayList<>();

            for (MangaChapterItem chapterItem : mangaChapterItems) {
                Chapter chapter = getNewChapter(manga, chapterItem);
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
            for (ChapterDto dto : chapterDtoList) {
                if (dto.getNextChapterId() != null) {
                    Chapter nextChapter = chapterMap.get(dto.getNextChapterId());
                    chapterMap.get(dto.getChapterId()).setNextChapter(nextChapter);
                }
                if (dto.getPrevChapterId() != null) {
                    Chapter prevChapter = chapterMap.get(dto.getPrevChapterId());
                    chapterMap.get(dto.getChapterId()).setPrevChapter(prevChapter);
                }
                if (dto.getMangaDataBaseId() == null) {
                    mangaChapterRepository.setMangaDatabaseIdAndType(manga.getId(), manga.getType(), dto.getId());
                }
            }

            Set<Long> volChIds = mangaChapterItems.stream()
                    .map(MangaChapterItem::getId)
                    .collect(Collectors.toSet());

            for (String chapterId : chapterMap.keySet()) {
                if (!volChIds.toString().contains(chapterId)) {
                    mangaChapterRepository.deleteChapterById(chapterId);
                }
            }

            for (int i = 0; i < mangaChapterItems.size(); i++) {
                if (!chapterMap.containsKey(String.valueOf(mangaChapterItems.get(i).getId())) || chapterMap.get(String.valueOf(mangaChapterItems.get(i).getId())).getNextChapter() == null || chapterMap.get(String.valueOf(mangaChapterItems.get(i).getId())).getPrevChapter() == null) {
                    Chapter currentChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(String.valueOf(mangaChapterItems.get(i).getId())));
                    if (currentChapter == null) {
                        currentChapter = getNewChapter(manga, mangaChapterItems.get(i));
                        mangaChapterRepository.save(currentChapter);
                    }

                    if (i != 0 && (currentChapter.getPrevChapter() == null || !currentChapter.getPrevChapter().getChapterId().equals(String.valueOf(mangaChapterItems.get(i - 1).getId())))) {
                        mangaChapterRepository.setPrevChapter(null, chapterMap.get(chapterMap.get(String.valueOf(mangaChapterItems.get(i - 1).getId())).getNextChapter().getChapterId()).getId());
                        mangaChapterRepository.setPrevChapter(String.valueOf(mangaChapterItems.get(i - 1).getId()), currentChapter.getId());
                        try {
                            mangaChapterRepository.setNextChapterByChapterId(currentChapter.getChapterId(), String.valueOf(mangaChapterItems.get(i - 1).getId()));
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Next Chapter not found");
                        }
                    }
                    if (i != mangaChapterItems.size() - 1 && (currentChapter.getNextChapter() == null || !currentChapter.getNextChapter().getChapterId().equals(String.valueOf(mangaChapterItems.get(i + 1).getId())))) {
                        Chapter nextChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(String.valueOf(mangaChapterItems.get(i + 1).getId())));
                        if (nextChapter == null) {
                            nextChapter = getNewChapter(manga, mangaChapterItems.get(i + 1));
                        }
                        nextChapter.setChapterId(String.valueOf(mangaChapterItems.get(i + 1).getId()));
                        mangaChapterRepository.save(nextChapter);

                        try {
                            mangaChapterRepository.setNextChapter(mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(String.valueOf(mangaChapterItems.get(i + 2).getId()))).getChapterId(), nextChapter.getId());
                        } catch (IndexOutOfBoundsException | NullPointerException e) {
                            mangaChapterRepository.setNextChapter(null, nextChapter.getId());
                        }
                        mangaChapterRepository.setPrevChapter(currentChapter.getChapterId(), nextChapter.getId());
                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());

                        try {
                            mangaChapterRepository.setPrevChapterByChapterId(nextChapter.getChapterId(), String.valueOf(mangaChapterItems.get(i + 2).getId()));
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Prev Chapter not found");
                        }
                    }
                }
            }
        }

        List<ChapterDto> chapters = mangaChapterRepository.findAllByMangaIdAndCatalogName(manga.getMangaId(), desuMe);
        Map<String, Chapter> chapterMapResult = chapters.stream()
                .collect(Collectors.toMap(ChapterDto::getChapterId, chapterDto -> mangaUtil.convertToChapter(chapterDto)));
        Collections.reverse(mangaChapterItems);

        List<Chapter> chaptersResult = new ArrayList<>();

        for (ChapterDto dto : chapters) {
            if (dto.getNextChapterId() != null && chapterMapResult.get(dto.getChapterId()) != null) {
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
    }

    private Chapter getNewChapter(Manga manga, MangaChapterItem chapterItem) {
        return new Chapter(desuMe, manga.getMangaId(), String.valueOf(chapterItem.getId()),
                manga.getName(), chapterItem.getVol(), chapterItem.getCh(),
                new Timestamp(System.currentTimeMillis()), manga.getFormat(), manga.getId(), manga.getType(), null, null);
    }

    private MangaDataDesu getMangaDataChapters(String mangaId, String mangaChapterItemsId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Response response = desuMeApiFeignClient.getChapter(mangaId, mangaChapterItemsId);
            log.info(response.status() + " " + response.reason());
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaResponse.class);
            return mangaResponse.getResponse();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void preloadMangaChapter(Long userId, Chapter chapter) {
        try {
            Chapter nextChapter = chapter.getNextChapter();
            User user = userRepository.findByUserId(userId);
            if (nextChapter == null || (nextChapter.getTelegraphStatusDownload() != null && nextChapter.getTelegraphStatusDownload().equals("finished")) || (nextChapter.getTelegraphStatusDownload() != null && nextChapter.getTelegraphStatusDownload().equals("process"))) {
                return;
            }
            mangaChapterRepository.setTelegraphStatusDownload("process", nextChapter.getId());

            List<MangaPage> pageList = getMangaDataChapters(nextChapter.getMangaId(), nextChapter.getChapterId()).getPages().getList();
            List<AwsUrl> awsUrls = new ArrayList<>();
            for (MangaPage mangaPage : pageList) {
                String fileExtension = mangaPage.getImg().substring(mangaPage.getImg().lastIndexOf('.') + 1);
                String fileName = userId + "_" + UUID.randomUUID() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH_mm_ss_dd_yyyy")) + "." + fileExtension;
                double aspectRatio = (double) mangaPage.getHeight() / mangaPage.getWidth();
                if (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("telegraph") && aspectRatio > 2.5) {
                    File file = util.downloadFileWithReferrer(util.createStorageFolder("TempImageStorage"), new URL(mangaPage.getImg().replace("desu.me", "x.desu.city")), fileName, "https://desu.win/");
                    List<File> resizeFileList = photoProcessingService.photoProcessing(util.createStorageFolder("ResizeImageStorage"), file, user.getUserId());
                    for (File resizeFile : resizeFileList) {
                        URL urlAws = awsServerService.uploadLocalFile(resizeFile.getPath(), "MangaManhwaBot/" + "/" + resizeFile.getName(), resizeFile.length());
                        awsUrls.add(new AwsUrl(nextChapter.getId(), urlAws.toString(), 0, 0, resizeFile.length(), userId, new Timestamp(System.currentTimeMillis())));
                        resizeFile.delete();
                    }
                    file.delete();
                } else if (user.getMangaFormatParameter() == null && aspectRatio > 2.5) {
                    mangaChapterRepository.setTelegraphStatusDownload(null, nextChapter.getId());
                    preloadManhwaChapter(userId, chapter);
                    return;
                } else {
                    long fileSize = util.getFileSize(mangaPage.getImg().replace("desu.me", "desu.win"), "https://x.desu.city/");
                    URL urlAws = awsServerService.uploadFileFromUrl(mangaPage.getImg().replace("desu.me", "x.desu.city"), "MangaManhwaBot/" + "/" + fileName, fileSize, "https://desu.win/");
                    awsUrls.add(new AwsUrl(nextChapter.getId(), urlAws.toString(), mangaPage.getHeight(), mangaPage.getWidth(), fileSize, userId, new Timestamp(System.currentTimeMillis())));
                }
            }

            if (awsUrls.isEmpty()) {
                util.sendErrorMessage("Возникла ошибка при сохранении изображений, попробуй еще раз или обратись в поддержку", userId);
                return;
            }

            awsUrlRepository.saveAll(awsUrls);
            List<Node> content = fillContentForTelegraphPage(new ArrayList<>(), awsUrls);

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void preloadManhwaChapter(Long userId, Chapter chapter) {
        try {
            Chapter nextChapter = chapter.getNextChapter();
            if (nextChapter == null || (nextChapter.getPdfStatusDownload() != null && nextChapter.getPdfStatusDownload().equals("finished")) || (nextChapter.getPdfStatusDownload() != null && nextChapter.getPdfStatusDownload().equals("process"))) {
                return;
            }
            mangaChapterRepository.setPdfStatusDownload("process", nextChapter.getId());

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            List<MangaPage> pageList = getMangaDataChapters(nextChapter.getMangaId(), nextChapter.getChapterId()).getPages().getList();

            File pdfFolder = util.createStorageFolder("TempPdfStorage");
            String pdfFileName = pdfFolder + File.separator + truncateFileName(nextChapter.getName(), 50).replace(" ", "_").replace(".", "_").replace("/", "_") + "_Vol_" + nextChapter.getVol().replace(".", "_") + "_Chapter_" + nextChapter.getChapter().replace(".", "_") + "_From_" + userId + "_" + dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".pdf";
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (MangaPage page : pageList) {
                if (page.getImg().endsWith(".gif") || page.getImg().endsWith(".GIF")) {
                    continue;
                }
                if (page.getImg().endsWith(".webp") || page.getImg().endsWith(".WEBP")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFile(folder, new URL(page.getImg()), "temp_img" + nextChapter.getName().replace(" ", "_") + "_Vol_" + nextChapter.getVol() + "_Chapter_" + nextChapter.getChapter() + "_Page_" + page.getPage() + "_From_" + userId + ".webp");
                    File jpegFile = mangaUtil.getJpeg(folder, file, truncateFileName(nextChapter.getName(), 50).replace(" ", "_") + "_Vol_" + nextChapter.getVol() + "_Chapter_" + nextChapter.getChapter() + "_Page_" + page.getPage() + "_From_" + userId);
                    if (jpegFile == null) {
                        continue;
                    }
                    ImageData imgData = ImageDataFactory.create(jpegFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    jpegFile.delete();
                    file.delete();
                } else {
                    ImageData imgData = util.downloadImageWithReferer(page.getImg().replace("desu.me", "x.desu.city"));
                    if (imgData == null) {
                        continue;
                    }
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();

            File pdfFile = compressImages(pdfFileName, nextChapter, userId, 0.9, pageList);

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


    private File compressImages(String pdfFileName, Chapter chapter, Long userId, double compressParam, List<MangaPage> pageList) {
        try {
            File pdfFile = new File(pdfFileName);
            if (pdfFile.length() >= 52000000) {
                FFmpeg ffmpeg = new FFmpeg();
                FFprobe ffprobe = new FFprobe();
                pdfFile.delete();
                pdfFile = new File(pdfFileName);
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(pdfFileName));
                Document doc = new Document(pdfDoc);
                for (MangaPage page : pageList) {
                    String fileName = chapter.getName().replace(" ", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + page.getPage() + "_From_" + userId;
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFile(folder, new URL(page.getImg()), fileName);
                    if (page.getImg().endsWith(".webp") || page.getImg().endsWith(".WEBP")) {
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
                compressImages(pdfFileName, chapter, userId, compressParam - 0.1, pageList);
            }
            return pdfFile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public Integer createTelegraphArticleChapter(Long userId, Chapter chapter, EditMessageCaption editMessageCaption) {
        User user = userRepository.findByUserId(userId);
        List<MangaPage> pageList = getMangaDataChapters(chapter.getMangaId(), chapter.getChapterId()).getPages().getList();
        mangaChapterRepository.setTelegraphStatusDownload("process", chapter.getId());

        List<AwsUrl> awsUrls = new ArrayList<>();
        int i = 0;

        String caption = "";
        if (editMessageCaption != null) {
            caption = editMessageCaption.getCaption();
        }
        for (MangaPage mangaPage : pageList) {
            String fileExtension = mangaPage.getImg().substring(mangaPage.getImg().lastIndexOf('.') + 1);
            String fileName = userId + "_" + UUID.randomUUID() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH_mm_ss_dd_yyyy")) + "." + fileExtension;
            double aspectRatio = (double) mangaPage.getHeight() / mangaPage.getWidth();
            if (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("telegraph") && aspectRatio > 2.5) {
                File file = util.downloadFileWithReferrer(util.createStorageFolder("TempImageStorage"), new URL(mangaPage.getImg().replace("desu.me", "x.desu.city")), fileName, "https://x.desu.city/");
                List<File> resizeFileList = photoProcessingService.photoProcessing(util.createStorageFolder("ResizeImageStorage"), file, user.getUserId());
                for (File resizeFile : resizeFileList) {
                    URL urlAws = awsServerService.uploadLocalFile(resizeFile.getPath(), "MangaManhwaBot/" + "/" + resizeFile.getName(), resizeFile.length());
                    awsUrls.add(new AwsUrl(chapter.getId(), urlAws.toString(), 0, 0, resizeFile.length(), userId, new Timestamp(System.currentTimeMillis())));
                    resizeFile.delete();
                }
                file.delete();
            } else if (user.getMangaFormatParameter() == null && aspectRatio > 2.5) {
                mangaChapterRepository.setTelegraphStatusDownload(null, chapter.getId());
                return createPdfChapter(userId, chapter);
            } else {
                long fileSize = util.getFileSize(mangaPage.getImg().replace("desu.me", "desu.win"), "https://x.desu.city/");
                URL urlAws = awsServerService.uploadFileFromUrl(mangaPage.getImg().replace("desu.me", "x.desu.city"), "MangaManhwaBot/" + "/" + fileName, fileSize, "https://desu.win/");
                awsUrls.add(new AwsUrl(chapter.getId(), urlAws.toString(), mangaPage.getHeight(), mangaPage.getWidth(), fileSize, userId, new Timestamp(System.currentTimeMillis())));
            }
            i++;
            if (editMessageCaption != null && i % 3 == 0) {
                editMessageCaption.setCaption(caption + "\n\nСтатус загрузки страниц: " + i + " из " + pageList.size());
                telegramSender.sendEditMessageCaption(editMessageCaption);
            }
        }

        if (awsUrls.isEmpty()) {
            util.sendErrorMessage("Возникла ошибка при сохранении изображений, попробуй еще раз или обратись в поддержку", userId);
            return null;
        }

        awsUrlRepository.saveAll(awsUrls);
        List<Node> content = fillContentForTelegraphPage(new ArrayList<>(), awsUrls);

        if (content == null) {
            mangaChapterRepository.setTelegraphStatusDownload(null, chapter.getId());
            return createPdfChapter(userId, chapter);
        }

        Page page = mangaUtil.createTelegraphPage(chapter, content);
        Integer messageIdChapterInStorage = mangaUtil.sendChapterInStorage(chapter, page);
        if (messageIdChapterInStorage != null) {
            mangaChapterRepository.setTelegraphMessageId(messageIdChapterInStorage, chapter.getId());
            mangaChapterRepository.setTelegraphStatusDownload("finished", chapter.getId());
            mangaChapterRepository.setTelegraphUrl(page.getUrl(), chapter.getId());
        }
        return messageIdChapterInStorage;
    }

    public List<Node> fillContentForTelegraphPage(List<Node> content, List<AwsUrl> pageList) {
        for (AwsUrl page : pageList) {
            Node image = mangaUtil.createImage(page.getAwsUrl().replace("https://gorillastorage.s3.eu-north-1.amazonaws.com/", "https://drym3wnf5xeuy.cloudfront.net/"));
            content.add(image);
        }
        return content;
    }

    @Override
    public Integer createPdfChapter(Long userId, Chapter chapter) {
        if (chapter == null || (chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("process"))) {
            return null;
        }

        if ((chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("finished"))) {
            return chapter.getPdfMessageId();
        }

        mangaChapterRepository.setPdfStatusDownload("process", chapter.getId());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        List<MangaPage> pageList = getMangaDataChapters(chapter.getMangaId(), chapter.getChapterId()).getPages().getList();
        File pdfFolder = util.createStorageFolder("TempPdfStorage");
        String pdfFileName = pdfFolder + File.separator + truncateFileName(chapter.getName(), 50).replace(" ", "_")
                .replace(".", "_").replace(",", "_").replace("/", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" +
                chapter.getChapter().replace(".", "_") + "_From_" + userId + "_" +
                dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".pdf";

        File pdfFile = createPdf(pageList, pdfFileName, chapter, userId);


        Integer messageIdChapterInStorage = telegramSender.sendDocument(SendDocument.builder()
                .caption(chapter.getName() + "\n" + "Том " + chapter.getVol() + ". Глава " + chapter.getChapter())
                .document(new InputFile(pdfFile))
                .chatId("-1002092468371L").build()).getMessageId();

        if (messageIdChapterInStorage != null) {
            mangaChapterRepository.setPdfMessageId(messageIdChapterInStorage, chapter.getId());
            mangaChapterRepository.setPdfStatusDownload("finished", chapter.getId());
        }
        pdfFile.delete();
        return messageIdChapterInStorage;
    }

    public String truncateFileName(String fileName, int maxLength) {
        if (fileName.length() > maxLength) {
            return fileName.substring(0, maxLength);
        }
        return fileName;
    }

    public Integer createCbzChapter(Long userId, Chapter chapter) {
//        if (chapter == null || (chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("process"))) {
//            return null;
//        }
//
//        if ((chapter.getPdfStatusDownload() != null && chapter.getPdfStatusDownload().equals("finished"))) {
//            return chapter.getPdfMessageId();
//        }

//        mangaChapterRepository.setPdfStatusDownload("process", chapter.getId());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        List<MangaPage> pageList = getMangaDataChapters(chapter.getMangaId(), chapter.getChapterId()).getPages().getList();
        File pdfFolder = util.createStorageFolder("TempCbzStorage");
        String pdfFileName = pdfFolder + File.separator + chapter.getName().replace(" ", "_")
                .replace(".", "_").replace("/", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" +
                chapter.getChapter().replace(".", "_") + "_From_" + userId + "_" +
                dateFormat.format(new Timestamp(System.currentTimeMillis())) + ".cbz";

        File cbzFile = createCbz(pageList, pdfFileName, chapter, userId);


        Integer messageIdChapterInStorage = telegramSender.sendDocument(SendDocument.builder()
                .caption(chapter.getName() + "\n" + "Том " + chapter.getVol() + ". Глава " + chapter.getChapter())
                .document(new InputFile(cbzFile))
                .chatId("-1002092468371L").build()).getMessageId();

        if (messageIdChapterInStorage != null) {
//            mangaChapterRepository.setPdfMessageId(messageIdChapterInStorage, chapter.getId());
//            mangaChapterRepository.setPdfStatusDownload("finished", chapter.getId());
        }
        cbzFile.delete();
        return messageIdChapterInStorage;
    }

    public File createPdf(List<MangaPage> pageList, String pdfFileName, Chapter chapter, Long userId) {
        try {
            PdfWriter pdfWriter = new PdfWriter(pdfFileName);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document doc = new Document(pdfDoc);

            for (MangaPage page : pageList) {
                if (page.getImg().contains(".gif?") || page.getImg().contains(".GIF?")) {
                    continue;
                }
                if (page.getImg().contains(".webp?") || page.getImg().contains(".WEBP>")) {
                    File folder = util.createStorageFolder("TempImgStorage");
                    File file = util.downloadFile(folder, new URL(page.getImg()), "temp_img" + chapter.getName().replace(" ", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + page.getPage() + "_From_" + userId + ".webp");
                    File jpegFile = mangaUtil.getJpeg(folder, file, chapter.getName().replace(" ", "_") + "_Vol_" + chapter.getVol() + "_Chapter_" + chapter.getChapter() + "_Page_" + page.getPage() + "_From_" + userId);
                    if (jpegFile == null) {
                        continue;
                    }
                    ImageData imgData = ImageDataFactory.create(jpegFile.getPath());
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                    jpegFile.delete();
                    file.delete();
                } else {
                    ImageData imgData = util.downloadImageWithReferer(page.getImg().replace("desu.me", "x.desu.city"));
                    if (imgData == null) {
                        continue;
                    }
                    Image image = new Image(imgData);
                    PageSize pageSize = new PageSize(image.getImageWidth(), image.getImageHeight());
                    pdfDoc.addNewPage(pageSize);
                    image.setFixedPosition(pdfDoc.getNumberOfPages(), 0, 0);
                    doc.add(image);
                }
            }
            doc.close();
            return compressImages(pdfFileName, chapter, userId, 0.9, pageList);
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public File createCbz(List<MangaPage> pageList, String cbzFileName, Chapter chapter, Long userId) {
        try {
            // Создаем временную директорию для хранения изображений
            File tempDir = Files.createTempDirectory("tempMangaImages").toFile();

            // Скачиваем и сохраняем все страницы в временную директорию
            for (MangaPage page : pageList) {
                File savedFile = null;
                if (page.getImg().endsWith(".webp") || page.getImg().endsWith(".WEBP")) {
                    // Скачиваем webp изображение и конвертируем его в jpeg
                    File file = util.downloadFile(tempDir, new URL(page.getImg()), "temp_img_" + page.getPage() + ".webp");
                    File jpegFile = mangaUtil.getJpeg(tempDir, file, "page_" + page.getPage());
                    if (jpegFile != null) {
                        savedFile = jpegFile;
                    }
                } else {
                    savedFile = util.downloadImageWithReferer(page.getImg().replace("desu.me", "x.desu.city"), tempDir, "page_" + page.getPage() + ".png");
                }

            }

            // Создаем CBZ архив (ZIP)
            File cbzFile = new File(cbzFileName);
            try (FileOutputStream fos = new FileOutputStream(cbzFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ZipEntry zipEntry = new ZipEntry(file.getName());
                            zos.putNextEntry(zipEntry);

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }

            // Удаляем временную директорию с изображениями
//            for (File file : Objects.requireNonNull(tempDir.listFiles())) {
//                file.delete();
//            }
//            tempDir.delete();

            return cbzFile;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create CBZ file", e);
        }
    }

    @Override
    public InlineKeyboardMarkup getPrevNextButtons(Chapter chapter, Long userId) {
        InlineKeyboardMarkup inlineKeyboardMarkup;
        String readStatus = readStatusRepository.existsByMangaIdAndChapterIdAndUserIdAndCatalogName(chapter.getMangaId(), chapter.getChapterId(), userId, desuMe) ? "✅" : "\uD83D\uDCD5";
        User user = userRepository.findByUserId(userId);
        user.setIsPremiumBotUser(true);
        if (user.getNumberOfChaptersSent() == null) {
            user.setNumberOfChaptersSent("3");
            userRepository.setNumberOfChaptersSent("3", userId);
        }

        if (chapter.getPrevChapter() == null && chapter.getNextChapter() == null) {
            inlineKeyboardMarkup = null;
        } else if (chapter.getPrevChapter() == null) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat(desuMe + "\nmangaId:\n" + chapter.getMangaId()).build()),
                    user.getIsPremiumBotUser() != null && user.getIsPremiumBotUser() && user.getNumberOfChaptersSent() != null ? new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующие " + user.getNumberOfChaptersSent())).callbackData(desuMe + "\nnextChaptersPack\n" + chapter.getNextChapter().getId() + "\n" + user.getNumberOfChaptersSent()).build()) : new InlineKeyboardRow(),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData(desuMe + "\nreadStatus\n" + chapter.getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующая глава")).callbackData(desuMe + "\nnextChapter\n" + chapter.getNextChapter().getId()).build())
            )));
        } else if (chapter.getNextChapter() == null) {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat(desuMe + "\nmangaId:\n" + chapter.getMangaId()).build()),
                    user.getIsPremiumBotUser() != null && user.getIsPremiumBotUser() && user.getNumberOfChaptersSent() != null ? new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущие " + user.getNumberOfChaptersSent())).callbackData(desuMe + "\nprevChaptersPack\n" + chapter.getPrevChapter().getId() + "\n" + user.getNumberOfChaptersSent()).build()) : new InlineKeyboardRow(),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущая глава")).callbackData(desuMe + "\nprevChapter\n" + chapter.getPrevChapter().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData(desuMe + "\nreadStatus\n" + chapter.getId()).build())
            )));
        } else {
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Список глав")).switchInlineQueryCurrentChat(desuMe + "\nmangaId:\n" + chapter.getMangaId()).build()),
                    user.getIsPremiumBotUser() != null && user.getIsPremiumBotUser() && user.getNumberOfChaptersSent() != null ? new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Предыдущие " + user.getNumberOfChaptersSent())).callbackData(desuMe + "\nprevChaptersPack\n" + chapter.getPrevChapter().getId() + "\n" + user.getNumberOfChaptersSent()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Следующие " + user.getNumberOfChaptersSent())).callbackData(desuMe + "\nnextChaptersPack\n" + chapter.getNextChapter().getId() + "\n" + user.getNumberOfChaptersSent()).build()) : new InlineKeyboardRow(),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData(desuMe + "\nprevChapter\n" + chapter.getPrevChapter().getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(readStatus)).callbackData(desuMe + "\nreadStatus\n" + chapter.getId()).build(),
                            InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Дальше")).callbackData(desuMe + "\nnextChapter\n" + chapter.getNextChapter().getId()).build())
            )));
        }
        return inlineKeyboardMarkup;
    }

    @Override
    public void getRandomManga(Long userId) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("order", "updated");
            params.put("limit", "1");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Response response = desuMeApiFeignClient.searchManga(params);
            String jsonResponse = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            MangaSearchResponse mangaResponse = objectMapper.readValue(jsonResponse, MangaSearchResponse.class);
            MangaDataAsSearchResult mangaData = mangaResponse.getResponse().get(0);
            Random random = new Random();
            sendMangaById(userId, String.valueOf(random.nextLong(mangaData.getId() + 1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDescriptionForSearchResult(MangaDataAsSearchResult mangaData) {
        return "Рейтинг: " + mangaData.getScore() +
                " | Год: " + new SimpleDateFormat("yyyy").format(new Date(mangaData.getAired_on() * 1000)) +
                " | Тип: " + mangaData.getKind() + "\nСтатус: " + getStatus(mangaData.getStatus()) + "\n" +
                "Жанр: " + mangaData.getGenres();
    }

    private String getDescriptionForSearchResultFromDb(Manga manga) {
        return "Рейтинг: " + manga.getRating() +
                " | Год: " + manga.getReleaseDate() +
                " | Тип: " + manga.getType() + "\nСтатус: " + getStatus(manga.getStatus()) + "\n" +
                "Жанр: " + manga.getGenres();
    }

    private String getStatus(String status) {
        return switch (status) {
            case "ongoing" -> "Выходит";
            case "released" -> "Издано";
            case "continued" -> "Переводится";
            case "completed" -> "Завершено";
            default -> "none";
        };
    }

    private String getGenres(List<MangaGenre> mangaGenreList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (MangaGenre mangaGenre : mangaGenreList) {
            stringBuilder.append(mangaGenre.getRussian()).append(", ");
        }
        return stringBuilder.toString();
    }

    private String getMangaText(Manga manga) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b>").append(manga.getName()).append("</b>").append("\n\n");
        stringBuilder.append("<b>").append("Рейтинг: ").append("</b>").append(manga.getRating()).append("\n");
        stringBuilder.append("<b>").append("Год выпуска: ").append("</b>").append(manga.getReleaseDate()).append("\n");
        stringBuilder.append("<b>").append("Тип: ").append("</b>").append(manga.getType()).append("\n");
        stringBuilder.append("<b>").append("Статус: ").append("</b>").append(manga.getStatus()).append("\n");
        stringBuilder.append("<b>").append("Глав: ").append("</b>").append(manga.getNumberOfChapters()).append("\n");
        stringBuilder.append("<b>").append("Жанры: ").append("</b><i>").append(manga.getGenres()).append("</i>\n\n");
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(manga.getDescription().replace("<","\"").replace(">", "\""));

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    public void sendNotificationAboutNewChapter() {
        Map<String, List<Long>> desuMePrepareSendList = notificationEntityRepository.findAllByCatalogName(desuMe).stream()
                .collect(Collectors.groupingBy(NotificationEntity::getMangaId,
                        Collectors.mapping(NotificationEntity::getUserId, Collectors.toList())));
        for (String mangaId : desuMePrepareSendList.keySet()) {
            MangaDataDesu mangaData = getMangaData(mangaId);
            if (mangaData == null) {
                log.error("Сайт desu.me вернул null. MangaData для уведомления не найдена. Id манги: " + mangaId);
                continue;
            }
            String lastChapter = mangaData.getChapters().getLast().getCh();
            NotificationChapterMapping chapterMapping = notificationChapterMappingRepository.findByMangaIdAndCatalogName(mangaId, desuMe);
            if (chapterMapping != null && !chapterMapping.getChapter().equals(lastChapter)) {
                Manga manga = getOrSaveManga(mangaData);
                if (manga == null) {
                    log.error("Манга для уведомления не найдена и не была создана. Id манги: " + mangaId);
                    continue;
                }
                notificationChapterMappingRepository.setChapter(lastChapter, mangaId, desuMe);
                senderService.sendNotificationToUsers(desuMePrepareSendList.get(mangaId), manga, lastChapter);
            }
        }
    }

    private Manga getOrSaveManga(MangaDataDesu mangaData) {
        Manga manga = mangaRepository.findByMangaIdAndCatalogName(String.valueOf(mangaData.getId()), desuMe);
        return Objects.requireNonNullElseGet(manga, () -> saveManga(mangaData));
    }

}
