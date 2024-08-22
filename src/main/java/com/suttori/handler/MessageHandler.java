package com.suttori.handler;

import com.suttori.config.ServiceConfig;
import com.suttori.entity.User;
import com.suttori.exception.CatalogNotFoundException;
import com.suttori.service.*;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class MessageHandler implements Handler<Update> {

    private ButtonService buttonService;
    private UserService userService;
    private SettingService settingService;
    private AdminService adminService;
    private SenderService senderService;
    private ReferralService referralService;
    private LocaleService localeService;
    private MangaService mangaService;
    private TelegramSender telegramSender;
    private ProfileService profileService;
    private Util util;
    private ServiceConfig serviceConfig;
    private SortFilterDesuMeService sortFilterDesuMeService;
    private SortFilterMangaDexService sortFilterMangaDexService;

    @Autowired
    public MessageHandler(ButtonService buttonService, UserService userService, SettingService settingService,
                          AdminService adminService, SenderService senderService, ReferralService referralService,
                          LocaleService localeService, MangaService mangaService, TelegramSender telegramSender, Util util,
                          ProfileService profileService, ServiceConfig serviceConfig, SortFilterDesuMeService sortFilterDesuMeService, SortFilterMangaDexService sortFilterMangaDexService) {
        this.buttonService = buttonService;
        this.userService = userService;
        this.settingService = settingService;
        this.adminService = adminService;
        this.senderService = senderService;
        this.referralService = referralService;
        this.localeService = localeService;
        this.mangaService = mangaService;
        this.telegramSender = telegramSender;
        this.util = util;
        this.profileService = profileService;
        this.serviceConfig = serviceConfig;
        this.sortFilterDesuMeService = sortFilterDesuMeService;
        this.sortFilterMangaDexService = sortFilterMangaDexService;
    }

    @Override
    public void choose(Update update) {
        Message message = update.getMessage();
        User user = userService.getUser(message);
        if (message.getFrom().getIsBot()) {
            return;
        }
        if (user == null || (message.hasText() && message.getText().contains("/start"))) {
            userService.save(message);
            message.getFrom().setLanguageCode(userService.getLocale(message.getFrom().getId()));
            localeService.setLocale(Locale.forLanguageTag(message.getFrom().getLanguageCode()));
            buttonService.generateMainButtonsWithGreetings(message.getFrom().getId(), message.getText());
            return;
        } else {
            user.setLanguageCode(userService.getLocale(user.getUserId()));
            message.getFrom().setLanguageCode(user.getLanguageCode());
            localeService.setLocale(Locale.forLanguageTag(user.getLanguageCode()));
        }

        if (user.getPosition() == null) {
            userService.setPositionAndMessageId(user.getUserId(), "DEFAULT_POSITION");
        }

        if (message.hasText()) {
            userService.setLastActivity(user.getUserId());
        }

        if (message.hasText()) {
            try {
                switch (message.getText()) {
                    case "Поиск":
                        if (referralService.checkAccess(message)) {
                            mangaService.clickSearch(message);
                        }
                        return;
                    case "Случайная манга":
                        if (referralService.checkAccess(message)) {
                            serviceConfig.mangaServices().get(user.getCurrentMangaCatalog()).getRandomManga(message.getFrom().getId());
                        }
                        return;
                    case "Профиль":
                        if (referralService.checkAccess(message)) {
                            profileService.clickProfile(message);
                        }
                        return;
                    case "Настройки ⚙":
                        if (referralService.checkAccess(message)) {
                            settingService.clickSettings(message.getFrom().getId(), null);
                        }
                        return;
                    case "Главное меню":
                        if (referralService.checkAccess(message)) {
                            userService.setPositionAndMessageId(user.getUserId(), "DEFAULT_POSITION");
                            buttonService.generateMainButtonsWithGreetings(message.getFrom().getId(), message.getText());
                        }
                        return;
                }

                if (message.getText().contains("\nsetSortParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).setSortParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetStatusParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).setStatusParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetGenreParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).setGenreParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetTypeParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    sortFilterDesuMeService.setTypeParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetContentRatingParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    sortFilterMangaDexService.setContentRatingParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetMagazineDemographicParamsParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    sortFilterMangaDexService.setMagazineDemographicParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetFormatParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    sortFilterMangaDexService.setFormatParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nsetThemeParam\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    sortFilterMangaDexService.setThemeParams(message);
                    serviceConfig.sortFilterServices().get(util.getSourceName(message.getText())).clickSetSortFilterParams(message.getFrom().getId(), Integer.valueOf(user.getTemporaryMessageId()));
                    return;
                }

                if (message.getText().contains("\nchapterId\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    mangaService.getChapterFromMessageHandler(message);
                    return;
                }

                if (message.getText().contains("\nmangaId\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    serviceConfig.mangaServices().get(util.getSourceName(message.getText())).sendMangaById(message.getFrom().getId(), util.parseValue(message.getText())[2]);
                    return;
                }

                if (message.getText().contains("\nmangaDatabaseId\n")) {
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    serviceConfig.mangaServices().get(util.getSourceName(message.getText())).sendMangaByDatabaseId(message.getFrom().getId(), util.parseValue(message.getText())[2]);
                    return;
                }

                if (message.getText().contains("chooseLanguageCodeMangaDex\n")) {
                    mangaService.setLanguageCodeMangaDexAndCatalog(message);
                    return;
                }

                if (message.getText().equals("sortDESC")) {
                    userService.setSortParam("sortDESC", user.getUserId());
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    return;
                }

                if (message.getText().equals("sortASC")) {
                    userService.setSortParam("sortASC", user.getUserId());
                    telegramSender.deleteMessageById(String.valueOf(message.getFrom().getId()), message.getMessageId());
                    return;
                }

            } catch (CatalogNotFoundException e) {
                log.error("Ошибка при получении каталога", e);
                util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", user.getUserId());
            } catch (Exception e) {
                log.error("Неизвестная ошибка", e);
                util.sendErrorMessage("Произошла ошибка при поиске манги, введен неправильный запрос или что-то другое. Перезапусти бот и, если ошибка повторится, то напиши в поддержку.", user.getUserId());
            }
        }

        if (adminService.isAdmin(message.getFrom().getId())) {
            switch (user.getPosition()) {
                case "SET_LINK":
                    adminService.changeLink(message);
                    return;
                case "CREATE_REFERRAL_LINK":
                    adminService.createReferralLink(message);
                    return;
                case "CREATE_CREATIVE":
                    senderService.saveCreative(message);
                    return;
                case "TAKE_BUTTON":
                    senderService.takeButtonCreative(message);
                    return;
                case "GET_TOKEN":
                    adminService.getToken(message);
                    return;

            }

            if ((message.getForwardFrom() != null || message.getForwardFromChat() != null) && user.getPosition().equals("ADD_CHANNEL")) {
                adminService.addChannel(update);
                return;
            }

            if (message.hasText() && adminService.isAdmin(message.getFrom().getId())) {
                if (message.getText().equals("Файл")) {
                    adminService.createFile(message);
                    return;
                } else if (message.getText().equals("Стата")) {
                    adminService.getStatistics(message);
                    return;
                } else if (message.getText().equals("Удалить мертвых юзеров 1488")) {
                    adminService.deleteDead(message);
                    return;
                } else if (message.getText().contains("Подписка\n")) {
                    adminService.setPremium(message);
                    return;
                } else if (message.getText().contains("Стата по ссылкам")) {
                    adminService.linkStat(message);
                    return;
                } else if (message.getText().contains("Удалить арабов")) {
                    adminService.deleteArab(message);
                    return;
                } else if (message.getText().contains("Рекламораспространитель")) {
                    adminService.addRemoveAdvertiser(message);
                    return;
                } else if (message.getText().equals("Стата по активности")) {
                    adminService.getLastActivity(message);
                    return;
                } else if (message.getText().equals("Стата по загрузкам")) {
                    adminService.getStatAboutDownloadChapters(message);
                    return;
                } else if ((message.getText().equals("Админ панель") || message.getText().equals("Адмін панель") || message.getText().equals("Admin Panel"))) {
                    adminService.createAdminPanel(message);
                    return;
                }
//                else if (message.getText().equals("Поехали нахой")) {
//                    desuMeService.setChapterId();
//                    return;
//                }
            }


        }

        if (message.hasText() && message.getText().contains("Приход") && adminService.isAdvertiser(message.getFrom().getId())) {
            adminService.getStatForAdvertiser(message);
            return;
        }


    }

    @Override
    public void choose(List<Update> t) {
    }

}
