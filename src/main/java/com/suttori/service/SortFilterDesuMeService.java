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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SortFilterDesuMeService implements SortFilterInterface {

    @Value("${desuMe}")
    private String desuMe;
    private TelegramSender telegramSender;
    private UserFilterPreferencesRepository userFilterPreferencesRepository;
    private UserSortPreferencesRepository userSortPreferencesRepository;
    private Util util;
    private UserRepository userRepository;

    @Autowired
    public SortFilterDesuMeService(TelegramSender telegramSender, UserFilterPreferencesRepository userFilterPreferencesRepository, UserSortPreferencesRepository userSortPreferencesRepository, Util util, UserRepository userRepository) {
        this.telegramSender = telegramSender;
        this.userFilterPreferencesRepository = userFilterPreferencesRepository;
        this.userSortPreferencesRepository = userSortPreferencesRepository;
        this.util = util;
        this.userRepository = userRepository;
    }

    @Override
    public void clickSetSortFilterParams(Long userId, Integer messageId) {
        StringBuilder stringBuilder = new StringBuilder();
        UserSortPreferences userSortPreferences = userSortPreferencesRepository.findByUserIdAndCatalogName(userId, desuMe);
        if (userSortPreferences != null) {
            stringBuilder.append("<b>Сортировка:</b> ").append(Constants.getRevSortParamDesuMe(userSortPreferences.getSortType())).append("\n");
        }
        List<UserFilterPreference> filterPreferenceList = userFilterPreferencesRepository.findAllByUserIdAndCatalogName(userId, desuMe);
        if (!filterPreferenceList.isEmpty()) {
            Map<String, List<String>> userParams = filterPreferenceList.stream()
                    .collect(Collectors.groupingBy(
                            UserFilterPreference::getFilterType,
                            Collectors.mapping(UserFilterPreference::getFilterName, Collectors.toList())
                    ));
            for (Map.Entry<String, List<String>> param : userParams.entrySet()) {
                stringBuilder.append("<b>").append(Constants.getRevTypeParamDesuMe(param.getKey())).append(":</b> ").append(String.join(", ", param.getValue())).append("\n");
            }
        }

        Message message = telegramSender.sendEditMessageTextAsync(EditMessageText.builder()
                .text("Здесь ты можешь настроить параметры сортировки и фильтрации для каталога desu.me\n\n" + stringBuilder)
                .messageId(messageId)
                .parseMode("HTML")
                .chatId(userId)
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Сбросить все параметры")).callbackData(desuMe + "\nresetAllSortAndFilterParams").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Сортировка")).switchInlineQueryCurrentChat(desuMe + "\nsort").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Тип")).switchInlineQueryCurrentChat(desuMe + "\ntype").build()),
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Статус")).switchInlineQueryCurrentChat(desuMe + "\nstatus").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Жанр")).switchInlineQueryCurrentChat(desuMe + "\ngenre").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backToSearch").build())
                )))).build());
        if (message != null) {
            userRepository.setTemporaryMessageId(String.valueOf(message.getMessageId()), userId);
        }
    }

    public void resetAllSortAndFilterParams(CallbackQuery callbackQuery) {
        if (!userSortPreferencesRepository.existsByUserIdAndCatalogName(callbackQuery.getFrom().getId(), desuMe) && !userFilterPreferencesRepository.existsByUserIdAndCatalogName(callbackQuery.getFrom().getId(), desuMe)) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Параметры сортировки и фильтрации для каталога desu.me успешно сброшены")
                    .showAlert(true).build());
            return;
        }
        userSortPreferencesRepository.deleteAllByUserIdAndCatalogName(callbackQuery.getFrom().getId(), desuMe);
        userFilterPreferencesRepository.deleteAllByUserIdAndCatalogName(callbackQuery.getFrom().getId(), desuMe);
        clickSetSortFilterParams(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
    }

    @Override
    public void getSortParams(InlineQuery inlineQuery) {
        UserSortPreferences sortPreferences = userSortPreferencesRepository.findByUserIdAndCatalogName(inlineQuery.getFrom().getId(), desuMe);
        if (sortPreferences == null) {
            sortPreferences = new UserSortPreferences(inlineQuery.getFrom().getId(), desuMe, "order_by", "popular");
            userSortPreferencesRepository.save(sortPreferences);
        }

        Map<String, String> sortParams = Constants.getAllSortParamDesuMe();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, String> paramEntry : sortParams.entrySet()) {
            String title;
            if (sortPreferences.getSortType().equals(paramEntry.getValue())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetSortParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    @Override
    public void setSortParams(Message message) {
        String param = Constants.getSortParamDesuMe(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку?", message.getFrom().getId());
            return;
        }
        if (userSortPreferencesRepository.existsByUserIdAndCatalogName(message.getFrom().getId(), desuMe)) {
            userSortPreferencesRepository.setSortType(param, message.getFrom().getId(), desuMe);
        } else {
            userSortPreferencesRepository.save(new UserSortPreferences(message.getFrom().getId(), desuMe, "order_by", param));
        }
    }

    @Override
    public void getStatusParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> statusList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "status", desuMe);
        List<String> statusNameList = new ArrayList<>();
        if (!statusList.isEmpty()) {
            for (UserFilterPreference filterPreference : statusList) {
                statusNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> statusParams = Constants.getAllStatusParamDesuMe();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по статусам")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetStatusParam\nnone")).build());
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
                    .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetStatusParam\n" + paramEntry.getKey())).build());
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
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "status", desuMe);
            return;
        }
        String param = Constants.getStatusParamDesuMe(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку?", message.getFrom().getId());
            return;
        }

        UserFilterPreference statusParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "status", param, desuMe);
        if (statusParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), desuMe, "status", null, util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(statusParam);
        }
    }

    @Override
    public void getGenreParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> genreList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "genres", desuMe);
        List<String> genreNameList = new ArrayList<>();
        if (!genreList.isEmpty()) {
            for (UserFilterPreference filterPreference : genreList) {
                genreNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> genreParams = Constants.getAllGenreParamDesuMe();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по жанрам")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetGenreParam\nnone")).build());
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
                    .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetGenreParam\n" + paramEntry.getKey())).build());
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
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "genres", desuMe);
            return;
        }
        String param = Constants.getGenreParamDesuMe(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку?", message.getFrom().getId());
            return;
        }

        UserFilterPreference genreParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "genres", param, desuMe);
        if (genreParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), desuMe, "genres", null, util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(genreParam);
        }
    }


    public void getTypeParams(InlineQuery inlineQuery) {
        List<UserFilterPreference> typeList = userFilterPreferencesRepository.findAllByUserIdAndFilterTypeAndCatalogName(inlineQuery.getFrom().getId(), "kinds", desuMe);
        List<String> typeNameList = new ArrayList<>();
        if (!typeList.isEmpty()) {
            for (UserFilterPreference filterPreference : typeList) {
                typeNameList.add(filterPreference.getFilterName());
            }
        }

        Map<String, String> typeParams = Constants.getAllTypeParamDesuMe();
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int i = 0;
        inlineQueryResultList.add(InlineQueryResultArticle.builder()
                .id(inlineQuery.getFrom().getId() + "" + i++)
                .title(EmojiParser.parseToUnicode("Сбросить все"))
                .description("Нажми чтобы сбросить фильтрацию по типам")
                .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetTypeParam\nnone")).build());
        for (Map.Entry<String, String> paramEntry : typeParams.entrySet()) {
            String title;
            if (typeNameList.contains(paramEntry.getKey())) {
                title = paramEntry.getKey() + " ✅";
            } else {
                title = paramEntry.getKey();
            }
            inlineQueryResultList.add(InlineQueryResultArticle.builder()
                    .id(inlineQuery.getFrom().getId() + "" + i++)
                    .title(EmojiParser.parseToUnicode(title))
                    .thumbnailUrl("https://gorillastorage.s3.eu-north-1.amazonaws.com/MangaReaderBot/Flags/31829297_7750732.jpg")
                    .inputMessageContent(new InputTextMessageContent(desuMe + "\nsetTypeParam\n" + paramEntry.getKey())).build());
        }
        telegramSender.sendAnswerInlineQuery(AnswerInlineQuery.builder()
                .results(inlineQueryResultList)
                .isPersonal(true)
                .cacheTime(1)
                .inlineQueryId(inlineQuery.getId()).build());
    }

    public void setTypeParams(Message message) {
        if (util.parseValue(message.getText())[2].equals("none")) {
            userFilterPreferencesRepository.deleteAllByUserIdAndFilterTypeAndCatalogName(message.getFrom().getId(), "kinds", desuMe);
            return;
        }
        String param = Constants.getTypeParamDesuMe(util.parseValue(message.getText())[2]);
        if (param == null) {
            util.sendErrorMessage("Пришел невалидный запрос, повтори еще раз и, если ошибка повторится, то обратись в поддержку?", message.getFrom().getId());
            return;
        }

        UserFilterPreference typeParam = userFilterPreferencesRepository.findByUserIdAndFilterTypeAndFilterValueAndCatalogName(message.getFrom().getId(), "kinds", param, desuMe);
        if (typeParam == null) {
            userFilterPreferencesRepository.save(new UserFilterPreference(message.getFrom().getId(), desuMe, "kinds", null, util.parseValue(message.getText())[2], param));
        } else {
            userFilterPreferencesRepository.delete(typeParam);
        }
    }

}
