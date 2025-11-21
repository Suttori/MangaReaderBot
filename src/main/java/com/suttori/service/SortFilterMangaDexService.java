package com.suttori.service;


import com.suttori.dao.UserFilterPreferencesRepository;
import com.suttori.dao.UserRepository;
import com.suttori.dao.UserSortPreferencesRepository;
import com.suttori.entity.UserFilterPreference;
import com.suttori.entity.UserSortPreferences;
import com.suttori.service.interfaces.SortFilterInterface;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Constants;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SortFilterMangaDexService implements SortFilterInterface {

    @Value("${mangaDex}")
    private String mangaDex;
    private TelegramSender telegramSender;
    private UserFilterPreferencesRepository userFilterPreferencesRepository;
    private UserSortPreferencesRepository userSortPreferencesRepository;
    private Util util;
    private UserRepository userRepository;


    @Autowired
    public SortFilterMangaDexService(TelegramSender telegramSender, UserFilterPreferencesRepository userFilterPreferencesRepository, UserSortPreferencesRepository userSortPreferencesRepository, Util util, UserRepository userRepository) {
        this.telegramSender = telegramSender;
        this.userFilterPreferencesRepository = userFilterPreferencesRepository;
        this.userSortPreferencesRepository = userSortPreferencesRepository;
        this.util = util;
        this.userRepository = userRepository;
    }

    public void clickSetSortFilterParams(Long userId, Integer messageId) {
        StringBuilder stringBuilder = new StringBuilder();
        UserSortPreferences userSortPreferences = userSortPreferencesRepository.findByUserIdAndCatalogName(userId, mangaDex);
        if (userSortPreferences != null) {
            stringBuilder.append("<b>Сортировка:</b> ").append(Constants.getRevSortParamMangaDex(userSortPreferences.getSortName() + "\n" + userSortPreferences.getSortType())).append("\n");
        }
        List<UserFilterPreference> filterPreferenceList = userFilterPreferencesRepository.findAllByUserIdAndCatalogName(userId, mangaDex);
        if (!filterPreferenceList.isEmpty()) {
            Map<String, List<String>> userParams = filterPreferenceList.stream()
                    .collect(Collectors.groupingBy(
                            UserFilterPreference::getFilterType,
                            Collectors.mapping(UserFilterPreference::getFilterName, Collectors.toList())
                    ));
            for (Map.Entry<String, List<String>> param : userParams.entrySet()) {
                stringBuilder.append("<b>").append(Constants.getRevTypeParamMangaDex(param.getKey())).append(":</b> ").append(String.join(", ", param.getValue())).append("\n");
            }
        }

        Message message = telegramSender.sendEditMessageTextAsync(EditMessageText.builder()
                .text("Здесь ты можешь настроить параметры сортировки и фильтрации для каталога mangadex.org\n\n" + stringBuilder)
                .messageId(messageId)
                .parseMode("HTML")
                .chatId(userId)
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Сбросить все параметры")).callbackData(mangaDex + "\nresetAllSortAndFilterParams").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Сортировка")).switchInlineQueryCurrentChat(mangaDex + "\nsort").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Возрастной рейтинг")).switchInlineQueryCurrentChat(mangaDex + "\ncontentRating").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Категория манги")).switchInlineQueryCurrentChat(mangaDex + "\nmagazineDemographic").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Формат")).switchInlineQueryCurrentChat(mangaDex + "\nformat").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Статус")).switchInlineQueryCurrentChat(mangaDex + "\nstatus").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Тематика")).switchInlineQueryCurrentChat(mangaDex + "\ntheme").build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Жанр")).switchInlineQueryCurrentChat(mangaDex + "\ngenre").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backToSearch").build())
                )))).build());

        if (message != null) {
            userRepository.setTemporaryMessageId(String.valueOf(message.getMessageId()), userId);
        }
    }

    public void resetAllSortAndFilterParams(CallbackQuery callbackQuery) {
        if (!userSortPreferencesRepository.existsByUserIdAndCatalogName(callbackQuery.getFrom().getId(), mangaDex) && !userFilterPreferencesRepository.existsByUserIdAndCatalogName(callbackQuery.getFrom().getId(), mangaDex)) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Параметры сортировки и фильтрации для каталога mangaDex.org успешно сброшены")
                    .showAlert(true).build());
            return;
        }
        userSortPreferencesRepository.deleteAllByUserIdAndCatalogName(callbackQuery.getFrom().getId(), mangaDex);
        userFilterPreferencesRepository.deleteAllByUserIdAndCatalogName(callbackQuery.getFrom().getId(), mangaDex);
        clickSetSortFilterParams(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
    }

    @Override
    public void getSortParams(InlineQuery inlineQuery) {
        UserSortPreferences sortPreferences = userSortPreferencesRepository.findByUserIdAndCatalogName(inlineQuery.getFrom().getId(), mangaDex);
        if (sortPreferences == null) {
            sortPreferences = new UserSortPreferences(inlineQuery.getFrom().getId(), mangaDex, "order[rating]", "desc");
            userSortPreferencesRepository.save(sortPreferences);
        }

        Map<String, String> sortParams = Constants.getAllSortParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, String> paramEntry : sortParams.entrySet()) {
            String title;
            if (sortPreferences.getSortName().equals(paramEntry.getValue().split("\n")[0]) && sortPreferences.getSortType().equals(paramEntry.getValue().split("\n")[1])) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetSortParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    @Override
    public void setSortParams(Message message) {
        String param = Constants.getSortParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }
        if (userSortPreferencesRepository.existsByUserIdAndCatalogName(message.getFrom().getId(), mangaDex)) {
            String paramName = util.parseValue(param)[0];
            String paramType = util.parseValue(param)[1];
            userSortPreferencesRepository.setSortNameAndSortType(paramName, paramType, message.getFrom().getId(), mangaDex);
        } else {
            userSortPreferencesRepository.save(new UserSortPreferences(message.getFrom().getId(), mangaDex, "order[rating]", "desc"));
        }
    }

    @Override
    public void getStatusParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> statusList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "status", mangaDex);
        List<String> statusNameList = new ArrayList<>();
        if (!statusList.isEmpty()) {
            for (UserFilterPreference filterPreference : statusList) {
                statusNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> statusParams = Constants.getAllStatusParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по статусам")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetStatusParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : statusParams.entrySet()) {
            String title;
            if (statusNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetStatusParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    @Override
    public void setStatusParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "status", mangaDex);
            return;
        }
        String param = Constants.getStatusParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку?", message.getFrom().getId());
            return;
        }

        UserFilterPreference statusParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "status", param, mangaDex);
        if (statusParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), mangaDex, "status", "status[]", util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(statusParam);
        }
    }

    @Override
    public void getGenreParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> genreList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "genre", mangaDex);
        List<String> genreNameList = new ArrayList<>();
        if (!genreList.isEmpty()) {
            for (UserFilterPreference filterPreference : genreList) {
                genreNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> genreParams = Constants.getAllGenreParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по жанрам")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetGenreParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : genreParams.entrySet()) {
            String title;
            if (genreNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetGenreParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    @Override
    public void setGenreParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "genre", mangaDex);
            return;
        }
        String param = Constants.getGenreParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }

        UserFilterPreference genreParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "genre", param, mangaDex);
        if (genreParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), mangaDex, "genre", "includedTags[]", util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(genreParam);
        }
    }


    public void getContentRatingParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> genreList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "contentRating", mangaDex);
        List<String> genreNameList = new ArrayList<>();
        if (!genreList.isEmpty()) {
            for (UserFilterPreference filterPreference : genreList) {
                genreNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> genreParams = Constants.getAllContentRatingParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по возрастному рейтингу")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetContentRatingParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : genreParams.entrySet()) {
            String title;
            if (genreNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetContentRatingParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }


    public void setContentRatingParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "contentRating", mangaDex);
            return;
        }
        String param = Constants.getContentRatingParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }

        UserFilterPreference genreParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "contentRating", param, mangaDex);
        if (genreParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), mangaDex, "contentRating", "contentRating[]", util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(genreParam);
        }
    }

    public void getMagazineDemographicParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> magazineDemographicList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "magazineDemographic", mangaDex);
        List<String> magazineDemographicListNameList = new ArrayList<>();
        if (!magazineDemographicList.isEmpty()) {
            for (UserFilterPreference filterPreference : magazineDemographicList) {
                magazineDemographicListNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> magazineDemographicParams = Constants.getAllMagazineDemographicParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по категориям")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetMagazineDemographicParamsParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : magazineDemographicParams.entrySet()) {
            String title;
            if (magazineDemographicListNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetMagazineDemographicParamsParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }


    public void setMagazineDemographicParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "magazineDemographic", mangaDex);
            return;
        }
        String param = Constants.getMagazineDemographicParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }

        UserFilterPreference genreParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "magazineDemographic", param, mangaDex);
        if (genreParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), mangaDex, "magazineDemographic", "publicationDemographic[]", util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(genreParam);
        }
    }

    public void getFormatParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> genreList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "format", mangaDex);
        List<String> genreNameList = new ArrayList<>();
        if (!genreList.isEmpty()) {
            for (UserFilterPreference filterPreference : genreList) {
                genreNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> genreParams = Constants.getAllFormatParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по формату манги")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetFormatParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : genreParams.entrySet()) {
            String title;
            if (genreNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetFormatParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }


    public void setFormatParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "format", mangaDex);
            return;
        }
        String param = Constants.getFormatParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }

        UserFilterPreference genreParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "format", param, mangaDex);
        if (genreParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), mangaDex, "format", "includedTags[]", util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(genreParam);
        }
    }

    public void getThemeParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> genreList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "theme", mangaDex);
        List<String> genreNameList = new ArrayList<>();
        if (!genreList.isEmpty()) {
            for (UserFilterPreference filterPreference : genreList) {
                genreNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> genreParams = Constants.getAllThemeParamMangaDex();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по тематике манги")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetThemeParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : genreParams.entrySet()) {
            String title;
            if (genreNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(mangaDex + "\nsetThemeParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }


    public void setThemeParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "theme", mangaDex);
            return;
        }
        String param = Constants.getThemeParamMangaDex(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку", message.getFrom().getId());
            return;
        }

        UserFilterPreference genreParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "theme", param, mangaDex);
        if (genreParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), mangaDex, "theme", "includedTags[]", util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(genreParam);
        }
    }


}
