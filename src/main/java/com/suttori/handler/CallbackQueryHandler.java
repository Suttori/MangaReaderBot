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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;
import java.util.Locale;


@Component
@Slf4j
public class CallbackQueryHandler implements Handler<CallbackQuery> {

    private UserService userService;
    private SettingService settingService;
    private AdminService adminService;
    private SenderService senderService;
    private Util util;
    private LocaleService localeService;
    private MangaService mangaService;
    private ProfileService profileService;
    private ServiceConfig serviceConfig;
    private TelegramSender telegramSender;
    private SortFilterDesuMeService sortFilterDesuMeService;

    @Autowired
    public CallbackQueryHandler(UserService userService, SettingService settingService,
                                AdminService adminService, SenderService senderService,
                                Util util, LocaleService localeService, MangaService mangaService, ProfileService profileService, ServiceConfig serviceConfig, TelegramSender telegramSender, SortFilterDesuMeService sortFilterDesuMeService) {
        this.userService = userService;
        this.settingService = settingService;
        this.adminService = adminService;
        this.senderService = senderService;
        this.util = util;
        this.localeService = localeService;
        this.mangaService = mangaService;
        this.profileService = profileService;
        this.serviceConfig = serviceConfig;
        this.telegramSender = telegramSender;
        this.sortFilterDesuMeService = sortFilterDesuMeService;
    }

    @Override
    public void choose(CallbackQuery callbackQuery) {
        User user = userService.getUser(callbackQuery);
        try {
            if (user != null) {
                callbackQuery.getFrom().setLanguageCode(userService.getLocale(callbackQuery.getFrom().getId()));
                localeService.setLocale(Locale.forLanguageTag(callbackQuery.getFrom().getLanguageCode()));
            } else {
                return;
            }

            userService.setLastActivity(callbackQuery.getFrom().getId());
            switch (callbackQuery.getData()) {
                case "chooseLanguage":
                    settingService.chooseLanguage(callbackQuery);
                    return;
                case "backToSettings":
                    settingService.clickSettings(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
                    return;
                case "clickTechSupport":
                    settingService.clickTechSupport(callbackQuery);
                    return;
                case "clickMyFavorites":
                case "clickMyFavoritesViaFavorites":
                    profileService.clickMyFavorites(callbackQuery);
                    return;
                case "clickChooseCatalog":
                    mangaService.clickChooseCatalog(callbackQuery);
                    return;
                case "changeMangaFormat":
                    settingService.clickChangeMangaFormat(callbackQuery);
                    return;
                case "clickDonate":
                    settingService.clickDonate(callbackQuery);
                    return;
                case "changeNumberOfChaptersSent":
                    settingService.clickChangeNumberOfChaptersSent(callbackQuery);
                    return;
            }

            if (callbackQuery.getData().contains("prevChapter\n") || callbackQuery.getData().contains("nextChapter\n")) {
                mangaService.getChapterFromCallbackHandler(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("prevChaptersPack\n") || callbackQuery.getData().contains("nextChaptersPack\n")) {
                mangaService.handleChaptersPack(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("\nchangeStatus\n")) {
                mangaService.clickChangeMangaStatus(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("changeMangaStatus")) {
                mangaService.clickMangaStatus(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("clickBackManga\n")) {
                mangaService.clickBackManga(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("notification\n")) {
                serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).clickNotification(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("\nreadStatus\n")) {
                mangaService.clickReadStatus(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("chooseCatalog\nmangadex.org")) {
                mangaService.chooseMangaDexCatalog(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("chooseCatalog\n")) {
                mangaService.catalogWasChosen(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("\nmangaDatabaseId\n")) {
                telegramSender.deleteMessageById(String.valueOf(callbackQuery.getFrom().getId()), callbackQuery.getMessage().getMessageId());
                serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).sendMangaByDatabaseId(callbackQuery.getFrom().getId(), util.parseValue(callbackQuery.getData())[2]);
            }

            if (callbackQuery.getData().contains("clickBackToProfile")) {
                profileService.clickBackToProfile(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("clickMyFriend\n") || callbackQuery.getData().contains("clickMyFriendBack\n") || callbackQuery.getData().contains("click_previous_page_friend_page\n") || callbackQuery.getData().contains("click_next_page_friend_page\n")) {
                profileService.clickMyFriend(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("getInfoAboutFriend\n")) {
                profileService.getInfoAboutFriend(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("chooseMangaFormat\n")) {
                settingService.changeMangaFormat(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("chooseNumberOfChaptersSent\n")) {
                settingService.changeNumberOfChaptersSent(callbackQuery);
                return;
            }

            if (callbackQuery.getData().equals("clickSetSortFilterParams")) {
                serviceConfig.sortFilterServices().get(util.getSourceName(user.getCurrentMangaCatalog())).clickSetSortFilterParams(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
                return;
            }

            if (callbackQuery.getData().contains("resetAllSortAndFilterParams")) {
                serviceConfig.sortFilterServices().get(util.getSourceName(callbackQuery.getData())).resetAllSortAndFilterParams(callbackQuery);
                return;
            }

            if (callbackQuery.getData().equals("backToSearch")) {
                mangaService.clickSearch(callbackQuery);
                return;
            }

            if (callbackQuery.getData().contains("set_language\n")) {
                settingService.setLocale(callbackQuery);
                return;
            }

            if (adminService.isAdmin(callbackQuery.getFrom().getId())) {
                switch (callbackQuery.getData()) {
                    case "addChannel":
                        adminService.addChannel(callbackQuery);
                        return;
                    case "chooseChannel":
                        adminService.chooseChannel(callbackQuery);
                        return;
                    case "createLink":
                        adminService.setLink(callbackQuery);
                        return;
                    case "backAdminPanel":
                        adminService.createAdminPanelCallbackQuery(callbackQuery);
                        return;
                    case "statistics":
                    case "backToStatistic":
                        adminService.getAllLinks(callbackQuery);
                        return;
                    case "createReferralLink":
                        adminService.clickCreateReferralLink(callbackQuery);
                        return;
                    case "cancel":
                        adminService.clickCancel(callbackQuery);
                        return;
                    case "createSendAds":
                        senderService.clickCreateSendAds(callbackQuery);
                        return;
                    case "publish":
                        senderService.clickPublish(callbackQuery);
                        return;
                    case "cancel_create_send_ads":
                        senderService.cancelCreateCreative(callbackQuery);
                        return;
                    case "cancelSendAds":
                        adminService.cancelSendAds(callbackQuery);
                        return;
                    case "clickInfoAndBans":
                        adminService.clickInfoAndBans(callbackQuery);
                        return;
                    case "clickGetInfoAboutUser":
                        adminService.clickGetInfoAboutUser(callbackQuery);
                        return;
                }

                if (callbackQuery.getData().contains("deleteAds - ")) {
                    adminService.deleteAds(callbackQuery);
                    return;
                }

                if (callbackQuery.getData().contains("click_link - ")) {
                    adminService.clickLink(callbackQuery);
                    return;
                }

                if (callbackQuery.getData().contains("click_choose_channel - ")) {
                    adminService.clickChooseChannel(callbackQuery);
                    return;
                }

                if (callbackQuery.getData().contains("click_next_page - ") || callbackQuery.getData().contains("click_previous_page - ")) {
                    adminService.nextOrPrevButton(callbackQuery);
                    return;
                }

                if (callbackQuery.getData().equals("forward")) {
                    util.deleteMessageByMessageId(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
                    senderService.forward(callbackQuery);
                    return;
                }

                if (callbackQuery.getData().contains("set_channel_link - ")) {
                    adminService.setChannelLink(callbackQuery);
                    return;
                }

            }
        } catch (CatalogNotFoundException e) {
            log.error("Ошибка при получении каталога", e);
            util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", user.getUserId());
        } catch (Exception e) {
            log.error("Неизвестная ошибка", e);
            util.sendErrorMessage("Произошла ошибка, введен неправильный запрос или есть другая причина. Лучше написать в поддержку.", user.getUserId());
        }
    }

    @Override
    public void choose(List<CallbackQuery> t) {
    }

}
