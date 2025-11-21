package com.suttori.service;

import com.suttori.config.WebDriverSingleton;
import com.suttori.dao.MangaChapterRepository;
import com.suttori.dao.MangaRepository;
import com.suttori.dao.NotificationEntityRepository;
import com.suttori.dao.UserRepository;
import com.suttori.dto.ChapterDto;
import com.suttori.entity.*;
import com.suttori.entity.MangaDesu.MangaChapterItem;
import com.suttori.entity.MangaDesu.MangaDataAsSearchResult;
import com.suttori.entity.MangaDex.Manga.MangaDataMangaDex;
import com.suttori.entity.Usagi.MangaPage;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.MangaUtil;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsagiService implements MangaServiceInterface {

    private TelegramSender telegramSender;
    @Value("${usagi}")
    private String usagi;

    private Util util;
    private MangaUtil mangaUtil;

    private MangaRepository mangaRepository;
    private NotificationEntityRepository notificationEntityRepository;
    private UserRepository userRepository;
    private MangaChapterRepository mangaChapterRepository;

    @Autowired
    public UsagiService(TelegramSender telegramSender, Util util, MangaUtil mangaUtil, MangaRepository mangaRepository, NotificationEntityRepository notificationEntityRepository, UserRepository userRepository, MangaChapterRepository mangaChapterRepository) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.mangaUtil = mangaUtil;
        this.mangaRepository = mangaRepository;
        this.notificationEntityRepository = notificationEntityRepository;
        this.userRepository = userRepository;
        this.mangaChapterRepository = mangaChapterRepository;
    }

    @Override
    public void getSearchResult(InlineQuery inlineQuery, User user) {
        try {
            String src = "https://web.usagi.one/list?sortType=RATING";
            int offset = 50;
            if (!inlineQuery.getOffset().isEmpty()) {
                offset = Integer.parseInt(inlineQuery.getOffset());
                src = "https://web.usagi.one/list?sortType=RATING&offset=" + (offset - 50);
            }
            Document document = Jsoup.connect(src)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36")
                    .timeout(10000) // 10 секунд
                    .get();
            // Найти все блоки с мангой
            Elements mangaTiles = document.select(".tile");
            List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
            int i = 0;
            for (Element tile : mangaTiles) {
                // Название манги
                String title = tile.select(".desc h3 a").text();

                // Обложка манги (ссылка на изображение)
                String coverUrl = tile.select(".img img").attr("data-original");
                String mangaPageUrl = tile.select(".desc h3 a").attr("href");
                // Жанры
                Elements genres = tile.select(".desc .badge-dark.elem_genre");
                StringBuilder genresList = new StringBuilder();
                for (Element genre : genres) {
                    genresList.append(genre.text()).append(", ");
                }

                if (!genresList.isEmpty()) {
                    genresList.setLength(genresList.length() - 2);
                }
                String chapterCount = tile.select(".badge-secondary.amount-badge").text();

                inlineQueryResultList.add(InlineQueryResultArticle.builder()
                        .id(inlineQuery.getFrom().getId() + "" + i++)
                        .title(title)
                        .description("Глав: " + chapterCount + "\nЖанры: " + genresList)
                        .thumbnailUrl(coverUrl)
                        .inputMessageContent(new InputTextMessageContent(usagi + "\nmangaId\n" + mangaPageUrl)).build());
            }

            telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                    .results(inlineQueryResultList)
                    .nextOffset(String.valueOf(offset + 50))
                    .cacheTime(1)
                    .isPersonal(true)
                    .inlineQueryId(inlineQuery.getId()).build());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void sendMangaById(Long userId, String mangaId) {
        mangaId = mangaId.substring(1);
        Manga manga = mangaRepository.findByMangaIdAndCatalogName(mangaId, usagi);

        if (manga == null) {
            manga = saveManga(mangaId);
        }
        if (manga == null) {
            return;
        }
        String coverFileId = util.getPhotoFieldId(telegramSender.sendPhoto(SendPhoto.builder()
                .photo(new InputFile(manga.getCoverFileId() != null ? manga.getCoverFileId() : manga.getCoverUrl()))
                .chatId(userId)
                .parseMode("HTML")
                .replyMarkup(getMangaButtons(new MangaButtonData(userId, mangaId, manga.getId(), manga.getLanguageCode())))
                .caption(getMangaText(manga)).build()));
        if (manga.getCoverFileId() == null) {
            mangaRepository.setCoverFileId(coverFileId, manga.getId());
        }

//        if (manga.getCoverUrl() == null) {
//            mangaRepository.setCoverUrl(getCoverMangaDex(mangaData.getRelationships(), mangaData.getId(), "512"), manga.getId());
//        }

    }

    private Manga saveManga(String mangaId) {
        try {
            Document doc = Jsoup.connect("https://web.usagi.one/" + mangaId)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36")
                    //.timeout(10000) // 10 секунд
                    .get();

            String title = doc.select("meta[itemprop=name]").attr("content");
            Elements genresElements = doc.select(".subject-meta a.badge.badge-dark");
            StringBuilder genres = new StringBuilder();
            for (Element genre : genresElements) {
                genres.append(genre.text()).append(", ");
            }
            if (!genres.isEmpty()) {
                genres.setLength(genres.length() - 2);
            }
            String category = doc.select(".elem_category a").text();
            String year = doc.select(".elem_year a").text();
            Elements formatElements = doc.select(".elem_another  a");
            StringBuilder formats = new StringBuilder();
            for (Element format : formatElements) {
                formats.append(format.text()).append(", ");
            }
            if (!formats.isEmpty()) {
                formats.setLength(formats.length() - 2);
            }

            String description = doc.select("meta[itemprop=description]").attr("content");
            //TODO
            String chapters = doc.select(".read-last-chapter").text();
            Elements statusElements = doc.select(".subject-meta span.badge");
            List<String> statuses = statusElements.eachText();
            String status = String.join(", ", statuses); // Combine statuses into a single string
            String rating = doc.select(".user-rating meta[itemprop=ratingValue]").attr("content");
            String coverUrl = doc.select(".subject-cover img").attr("src");
            return mangaRepository.save(new Manga(coverUrl, mangaId, usagi, title, category, status, genres.toString(), description, year, rating, chapters, formats.toString(), null));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
        stringBuilder.append("<b>").append("Описание: ").append("</b>").append(manga.getDescription());

        if (stringBuilder.length() > 1024) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 1024));
            stringBuilder.append("...");
        }
        return stringBuilder.toString();
    }

    @Override
    public void sendMangaByDatabaseId(Long userId, String mangaDatabaseId) {

    }

    @Override
    public void clickNotification(CallbackQuery callbackQuery) {

    }

    @Override
    public void getMangaChaptersButton(InlineQuery inlineQuery) {
        User user = userRepository.findByUserId(inlineQuery.getFrom().getId());
        String mangaId = util.parseValue(inlineQuery.getQuery())[2];
        Manga manga = mangaRepository.findByMangaIdAndCatalogName(mangaId, usagi);

        List<Map.Entry<String, String>> chaptersListMap = getChaptersFromSource(mangaId);
        if (chaptersListMap.isEmpty()) {
            util.sendErrorMessage("Возникла ошибка при получении глав, обратись в поддержку", user.getUserId());
            return;
        }
        List<Chapter> sortedChapters = saveChapters(chaptersListMap, manga);
        mangaUtil.createAnswerInlineQueryButtons(inlineQuery, sortedChapters, user);
    }

    private List<Map.Entry<String, String>> getChaptersFromSource(String mangaId) {
        try {
            Document doc = Jsoup.connect("https://web.usagi.one/" + mangaId)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36")
                    //.timeout(10000)
                    .get();

            Map<String, String> chaptersMap = new LinkedHashMap<>();
            Elements chapterLinks = doc.select("table.table-hover a.chapter-link[href]");
            for (Element link : chapterLinks) {
                chaptersMap.put(link.attr("href"), parseChapterString(link.text()));
            }
            return new ArrayList<>(chaptersMap.entrySet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String parseChapterString(String input) {
        String result = "";

        // Регулярное выражение для строк
        Pattern pattern = Pattern.compile(
                "^(\\d+)\\s*-?\\s*(\\d+|[\\p{L}\\p{N}]+)?\\s*(.*?)$"
        );
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            // Группа 1: номер тома
            result = result + matcher.group(1) + "\n";

            // Группа 2: номер главы или текст (например, "Экстра")
            String chapterOrText = matcher.group(2);
            result = result + (chapterOrText != null ? chapterOrText.trim() + "\n" : "");

            // Группа 3: название главы (если есть)
            String chapterTitle = matcher.group(3);
            result = result + (chapterTitle != null ? chapterTitle + "\n" : "");
        }

        return result;
    }

    private List<Chapter> saveChapters(List<Map.Entry<String, String>> volChList, Manga manga) {
        List<ChapterDto> chapterDtoList = mangaChapterRepository.findAllByMangaIdAndCatalogName(manga.getMangaId(), usagi);

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

            Set<String> volChIds = volChList.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            for (String chapterId : chapterMap.keySet()) {
                if (!volChIds.contains(chapterId)) {
                    mangaChapterRepository.deleteChapterById(chapterId);
                }
            }

            for (int i = 0; i < volChList.size(); i++) {
                if (!chapterMap.containsKey(volChList.get(i).getKey())
                        || chapterMap.get(volChList.get(i).getKey()).getNextChapter() == null
                        || chapterMap.get(volChList.get(i).getKey()).getPrevChapter() == null) {
                    //Chapter currentChapter = mangaChapterRepository.findByChapterId(volChList.get(i).getValue());
                    Chapter currentChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(volChList.get(i).getKey()));

                    if (currentChapter == null) {
                        currentChapter = getNewChapter(manga, volChList.get(i));
                        mangaChapterRepository.save(currentChapter);
                    }

                    if (i != 0 && (currentChapter.getPrevChapter() == null || !currentChapter.getPrevChapter().getChapterId().equals(volChList.get(i - 1).getKey()))) {
                        //mangaChapterRepository.setPrevChapter(null, chapterMap.get(chapterMap.get(String.valueOf(volChList.get(i - 1).getValue())).getNextChapter().getChapterId()).getId());
                        mangaChapterRepository.setPrevChapter(volChList.get(i - 1).getKey(), currentChapter.getId());
                        try {
                            mangaChapterRepository.setNextChapterByChapterId(currentChapter.getChapterId(), volChList.get(i - 1).getKey());
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Next Chapter not found");
                        }
                    }
                    if (i != volChList.size() - 1 && (currentChapter.getNextChapter() == null || !currentChapter.getNextChapter().getChapterId().equals(volChList.get(i + 1).getKey()))) {
                        //Chapter nextChapter = mangaChapterRepository.findByChapterId(volChList.get(i + 1).getValue());
                        Chapter nextChapter = mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(volChList.get(i + 1).getKey()));
                        if (nextChapter == null) {
                            nextChapter = getNewChapter(manga, volChList.get(i + 1));
                        }
                        nextChapter.setChapterId(volChList.get(i + 1).getKey());
                        mangaChapterRepository.save(nextChapter);

                        try {
                            mangaChapterRepository.setNextChapter(mangaUtil.getChapterByDto(mangaChapterRepository.findChapterDtoByChapterId(volChList.get(i + 2).getKey())).getChapterId(), nextChapter.getId());
                        } catch (IndexOutOfBoundsException | NullPointerException e) {
                            mangaChapterRepository.setNextChapter(null, nextChapter.getId());
                        }

                        mangaChapterRepository.setPrevChapter(currentChapter.getChapterId(), nextChapter.getId());
                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());

//                        nextChapter.setPrevChapter(currentChapter);
//                        mangaChapterRepository.save(nextChapter);
//                        mangaChapterRepository.setNextChapter(nextChapter.getChapterId(), currentChapter.getId());

                        try {
                            mangaChapterRepository.setPrevChapterByChapterId(nextChapter.getChapterId(), volChList.get(i + 2).getKey());
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Prev Chapter not found");
                        }
                    }
                }

            }
        }
        List<ChapterDto> chapters = mangaChapterRepository.findAllByMangaIdAndCatalogName(manga.getMangaId(), usagi);
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
        String vol = stringEntry.getValue().split("\n").length > 0 ? stringEntry.getValue().split("\n")[0] : null;
        String ch = stringEntry.getValue().split("\n").length > 1 ? stringEntry.getValue().split("\n")[1] : null;
        String chName = stringEntry.getValue().split("\n").length > 2 ? stringEntry.getValue().split("\n")[2] : null;
        return new Chapter(usagi, manga.getMangaId(), stringEntry.getKey(),
                manga.getName(), vol, ch, new Timestamp(System.currentTimeMillis()), manga.getFormat(), manga.getId(),
                manga.getType(), manga.getLanguageCode(), chName);
    }

    @Override
    public InlineKeyboardMarkup getPrevNextButtons(Chapter chapter, Long userId) {
        return null;
    }

    @Override
    public InlineKeyboardMarkup getMangaButtons(MangaButtonData mangaButtonData) {
        String whiteCheckMark = notificationEntityRepository.findByMangaIdAndUserId(mangaButtonData.getMangaId(), mangaButtonData.getUserId()) != null ? " :white_check_mark:" : "";
        return new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("В закладки")).callbackData(usagi + "\nchangeStatus\n" + mangaButtonData.getMangaDatabaseId() + "\n" + mangaButtonData.getMangaId()).build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Уведомления" + whiteCheckMark)).callbackData(usagi + "\nnotification\n" + mangaButtonData.getMangaDatabaseId()).build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к чтению")).switchInlineQueryCurrentChat(usagi + "\nmangaId:\n" + mangaButtonData.getMangaId()).build())
        )));
    }

    @Override
    public Integer createTelegraphArticleChapter(Long userId, Chapter chapter, EditMessageCaption editMessageCaption) {
        List<MangaPage> urlList = getUrlPageList(chapter.getChapterId());
        if (urlList.isEmpty()) {
            util.sendErrorMessage("Данная глава доступна только на странице создателя, к сожалению это можно проверить только при ее загрузке, ссылки на все главы можно найти тут: \nhttps://mangadex.org/title/" + chapter.getMangaId() + "/", userId);
            return null;
        }
//
//        User user = userRepository.findByUserId(userId);
//        List<AwsUrl> awsUrls = new ArrayList<>();
//        int i = 0;
//        String caption = "";
//        if (editMessageCaption != null) {
//            caption = editMessageCaption.getCaption();
//        }
//        for (String url : urlList) {
//            String fileExtension = url.substring(url.lastIndexOf('.') + 1);
//            String fileName = userId + "_" + UUID.randomUUID() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH_mm_ss_dd_yyyy")) + "." + fileExtension;
//            File file = util.downloadFileWithoutReferrer(util.createStorageFolder("TempImageStorage"), new URL(url), fileName);
//            BufferedImage image = ImageIO.read(file);
//            double aspectRatio = (double) image.getHeight() / image.getWidth();
//            if (user.getMangaFormatParameter() != null && user.getMangaFormatParameter().equals("telegraph") && aspectRatio > 2.5) {
//                cutImagesAndSendToAws(file, userId, chapter.getId(), awsUrls);
//            } else if (user.getMangaFormatParameter() == null && aspectRatio > 2.5) {
//                mangaChapterRepository.setTelegraphStatusDownload(null, chapter.getId());
//                file.delete();
//                return createPdfChapter(userId, chapter);
//            } else {
//                URL urlAws = awsServerService.uploadFileFromUrl(url, "MangaManhwaBot/" + "/" + fileName, file.length());
//                awsUrls.add(new AwsUrl(chapter.getId(), urlAws.toString(), image.getHeight(), image.getWidth(), file.length()));
//            }
//            file.delete();
//            i++;
//
//            if (editMessageCaption != null && i % 3 == 0) {
//                editMessageCaption.setCaption(caption + "\n\nСтатус загрузки страниц: " + i + " из " + urlList.size());
//                telegramSender.sendEditMessageCaption(editMessageCaption);
//            }
//        }
//
//        if (awsUrls.isEmpty()) {
//            util.sendErrorMessage("Возникла ошибка при сохранении изображений, попробуй еще раз или обратись в поддержку", userId);
//            return null;
//        }
//        awsUrlRepository.saveAll(awsUrls);
//        List<Node> content = new ArrayList<>();
//        for (AwsUrl url : awsUrls) {
//            content.add(mangaUtil.createImage(url.getAwsUrl()));
//        }
//
//        if (content == null) {
//            mangaChapterRepository.setTelegraphStatusDownload(null, chapter.getId());
//            return createPdfChapter(userId, chapter);
//        }
//
//        mangaChapterRepository.setTelegraphStatusDownload("process", chapter.getId());
//        Page page = mangaUtil.createTelegraphPage(chapter, content);
//        Integer messageIdChapterInStorage = mangaUtil.sendChapterInStorage(chapter, page);
//        if (messageIdChapterInStorage != null) {
//            mangaChapterRepository.setTelegraphMessageId(messageIdChapterInStorage, chapter.getId());
//            mangaChapterRepository.setTelegraphStatusDownload("finished", chapter.getId());
//            mangaChapterRepository.setTelegraphUrl(page.getUrl(), chapter.getId());
//        }
        return null;
    }

    private List<MangaPage> getUrlPageList(String chapterId) {
        WebDriver webDriver = WebDriverSingleton.getInstance();
        synchronized (webDriver) {
            switchToFirstTab(webDriver);
            checkAuth(webDriver, null);
            ((JavascriptExecutor) webDriver).executeScript("window.open('" + chapterId + "', '_blank');");
            sleep(1000);
            switchToLastTab(webDriver);
        }

//        if (webDriver.)


        //TODO https://1.seimanga.me/

        WebElement fotocontextDiv = webDriver.findElement(By.cssSelector("div#fotocontext.no-user-select"));
        List<WebElement> images = fotocontextDiv.findElements(By.cssSelector("img[data-src], img[src]"));

        List<MangaPage> pageList = new ArrayList<>();
        for (WebElement image : images) {
            String url = image.getAttribute("data-src") != null ? image.getAttribute("data-src") : image.getAttribute("src");
            String rh = image.getAttribute("rh");
            String rw = image.getAttribute("rw");
            String size = image.getAttribute("data-size");
            if (url != null && rh != null && rw != null) {
                pageList.add(new MangaPage(url, rh, rw, size));
            }
        }
        webDriver.close();
        pageList.forEach(mangaPage -> System.out.println(mangaPage.getUrl() + " h " + mangaPage.getHeight() + " w " + mangaPage.height + " ds " + mangaPage.getSize()));

        return pageList;
    }

    private void switchToFirstTab(WebDriver webDriver) {
        Object[] tabs = webDriver.getWindowHandles().toArray();
        webDriver.switchTo().window(tabs[0].toString());
    }

    private void switchToLastTab(WebDriver webDriver) {
        Object[] tabs = webDriver.getWindowHandles().toArray();
        webDriver.switchTo().window(tabs[tabs.length - 1].toString());
    }

    private void checkAuth(WebDriver webDriver, Long userId) {
        webDriver.navigate().refresh();
        sleep(1000);
        if (!Jsoup.parse(webDriver.getPageSource()).getElementsByClass("strong nav-link login-link").isEmpty()) {
            setWebDriver(webDriver, userId);
        }
    }

    private void setWebDriver(WebDriver webDriver, Long userId) {
        try {
            WebElement loginButton = webDriver.findElement(By.xpath("//*[@id=\"wrap\"]/header/div/div[1]/div[2]/div[1]/div[3]/a"));
            loginButton.click();
            WebElement usernameField = webDriver.findElement(By.id("username"));
            WebElement passwordField = webDriver.findElement(By.id("password"));
            usernameField.sendKeys("yellowF310@gmail.com");
            passwordField.sendKeys("yellowF310");
            WebElement submitButton = passwordField.findElement(By.xpath("//*[@id=\"mangaBox\"]/div[1]/div/div[1]/form/div[4]/input"));
            submitButton.click();
            webDriver.get("https://web.usagi.one/internal/user/reader");
            sleep(1000);
            WebElement radioButton = webDriver.findElement(By.cssSelector("#reader-settings-modal > div:nth-child(1) > div > div > label:nth-child(3)"));
            radioButton.click();
        } catch (Exception e) {
            log.error("Ошибка авторизации на сайте", e);
            util.sendErrorMessage("Произошла ошибка при авторизации пользователя на сайте, повтори попытку и, если ошибка повторится, то обратись в поддержку", userId);
            throw new RuntimeException();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer createPdfChapter(Long userId, Chapter chapter) {
        return null;
    }

    @Override
    public Integer createCbzChapter(Long userId, Chapter chapter) {
        return null;
    }

    @Override
    public void preloadMangaChapter(Long userId, Chapter chapter) {

    }

    @Override
    public void preloadManhwaChapter(Long userId, Chapter chapter) {

    }

    @Override
    public void getRandomManga(Long userId) {

    }
}
