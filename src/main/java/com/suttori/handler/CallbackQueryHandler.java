package com.suttori.handler;

import com.suttori.config.ServiceConfig;
import com.suttori.entity.User;
import com.suttori.service.*;
import com.suttori.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;
import java.util.Locale;


@Component
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

    @Autowired
    public CallbackQueryHandler(UserService userService, SettingService settingService,
                                AdminService adminService, SenderService senderService,
                                Util util, LocaleService localeService, MangaService mangaService, ProfileService profileService, ServiceConfig serviceConfig) {
        this.userService = userService;
        this.settingService = settingService;
        this.adminService = adminService;
        this.senderService = senderService;
        this.util = util;
        this.localeService = localeService;
        this.mangaService = mangaService;
        this.profileService = profileService;
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void choose(CallbackQuery callbackQuery) {
        User user = userService.getUser(callbackQuery);
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
            case "clickDonate":
                settingService.clickDonate(callbackQuery);
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
        }

        if (callbackQuery.getData().contains("prevChapter\n") || callbackQuery.getData().contains("nextChapter\n")) {
            serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).getChapterFromCallbackHandler(callbackQuery);
            return;
        }

        if (callbackQuery.getData().contains("\nchangeStatus\n")) {
            serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).clickChangeMangaStatus(callbackQuery);
            return;
        }

        if (callbackQuery.getData().contains("changeMangaStatus")) {
            serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).clickMangaStatus(callbackQuery);
            return;
        }

        if (callbackQuery.getData().contains("clickBackManga\n")) {
            serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).clickBackManga(callbackQuery);
            return;
        }

        if (callbackQuery.getData().contains("sendMangaById\n")) {
            mangaService.sendMangaById(callbackQuery.getFrom().getId(), Long.valueOf(util.parseValue(callbackQuery.getData())[1]));
            return;
        }

        if (callbackQuery.getData().contains("notification\n")) {
            serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).clickNotification(callbackQuery);
            return;
        }

        if (callbackQuery.getData().contains("\nreadStatus\n")) {
            serviceConfig.mangaServices().get(util.getSourceName(callbackQuery.getData())).clickReadStatus(callbackQuery);
            return;
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
                case "clickChooseCatalog":
                    mangaService.clickChooseCatalog(callbackQuery);
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
    }

    @Override
    public void choose(List<CallbackQuery> t) {
    }

}
