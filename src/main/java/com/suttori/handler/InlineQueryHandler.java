package com.suttori.handler;

import com.suttori.entity.User;
import com.suttori.service.LocaleService;
import com.suttori.service.MangaService;
import com.suttori.service.ProfileService;
import com.suttori.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;

import java.util.List;
import java.util.Locale;

@Component
public class InlineQueryHandler implements Handler<InlineQuery> {

    private MangaService mangaService;
    private UserService userService;
    private LocaleService localeService;
    private ProfileService profileService;

    @Autowired
    public InlineQueryHandler(MangaService mangaService, UserService userService, LocaleService localeService, ProfileService profileService) {
        this.mangaService = mangaService;
        this.userService = userService;
        this.localeService = localeService;
        this.profileService = profileService;
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
        } else {
            mangaService.getSearchResultManga(inlineQuery);
        }

    }

    @Override
    public void choose(List<InlineQuery> t) {

    }
}
