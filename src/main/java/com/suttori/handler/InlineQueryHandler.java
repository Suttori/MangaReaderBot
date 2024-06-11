package com.suttori.handler;

import com.suttori.config.ServiceConfig;
import com.suttori.entity.User;
import com.suttori.service.*;
import com.suttori.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;

import java.util.List;
import java.util.Locale;

@Component
public class InlineQueryHandler implements Handler<InlineQuery> {

    private UserService userService;
    private LocaleService localeService;
    private ProfileService profileService;
    private ServiceConfig serviceConfig;
    private Util util;
    private MangaService mangaService;


    @Autowired
    public InlineQueryHandler(UserService userService, LocaleService localeService, ProfileService profileService, ServiceConfig serviceConfig, Util util, MangaService mangaService) {
        this.userService = userService;
        this.localeService = localeService;
        this.profileService = profileService;
        this.serviceConfig = serviceConfig;
        this.util = util;
        this.mangaService = mangaService;
    }


    @Override
    public void choose(InlineQuery inlineQuery) {
        User user = userService.getUser(inlineQuery);

        if (user != null) {
            inlineQuery.getFrom().setLanguageCode(userService.getLocale(inlineQuery.getFrom().getId()));
            localeService.setLocale(Locale.forLanguageTag(inlineQuery.getFrom().getLanguageCode()));
        } else {
            return;
        }

        if (inlineQuery.getQuery().equals("history")) {
            profileService.clickHistory(inlineQuery);
        } else if (inlineQuery.getQuery().equals("read") || inlineQuery.getQuery().equals("planned") || inlineQuery.getQuery().equals("finished") || inlineQuery.getQuery().equals("postponed")) {
            profileService.getMangaByStatus(inlineQuery);
        } else if (inlineQuery.getQuery().contains("\nmangaId:\n")) {
            try {
                serviceConfig.mangaServices().get(util.getSourceName(inlineQuery.getQuery())).getMangaChaptersButton(inlineQuery);
            } catch (NullPointerException e) {
                util.sendErrorMessage("Произошла ошибка при поиске главы, введен неправильный запрос или что-то другое. Перезапусти бот и, если ошибка повторится, то напиши в поддержку.", user.getUserId());
            }
        } else if (inlineQuery.getQuery().contains("SetLanguageCode")) {
            mangaService.chooseLanguageCodeMangaDex(inlineQuery);
        } else {
            serviceConfig.mangaServices().get(user.getCurrentMangaCatalog()).getSearchResult(inlineQuery);
        }

    }

    @Override
    public void choose(List<InlineQuery> t) {

    }
}
