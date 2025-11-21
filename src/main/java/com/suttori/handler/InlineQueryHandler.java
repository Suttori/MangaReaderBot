package com.suttori.handler;

import com.suttori.config.ServiceConfig;
import com.suttori.entity.User;
import com.suttori.exception.CatalogNotFoundException;
import com.suttori.service.*;
import com.suttori.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;

import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class InlineQueryHandler implements Handler<InlineQuery> {

    private UserService userService;
    private LocaleService localeService;
    private ProfileService profileService;
    private ServiceConfig serviceConfig;
    private Util util;
    private MangaService mangaService;
    private SortFilterDesuMeService sortFilterDesuMeService;
    private SortFilterMangaDexService sortFilterMangaDexService;


    @Autowired
    public InlineQueryHandler(UserService userService, LocaleService localeService, ProfileService profileService, ServiceConfig serviceConfig, Util util, MangaService mangaService, SortFilterDesuMeService sortFilterDesuMeService, SortFilterMangaDexService sortFilterMangaDexService) {
        this.userService = userService;
        this.localeService = localeService;
        this.profileService = profileService;
        this.serviceConfig = serviceConfig;
        this.util = util;
        this.mangaService = mangaService;
        this.sortFilterDesuMeService = sortFilterDesuMeService;
        this.sortFilterMangaDexService = sortFilterMangaDexService;
    }


    @Override
    public void choose(InlineQuery inlineQuery) {
        User user = userService.getUser(inlineQuery);
        try {
            if (user != null) {
                inlineQuery.getFrom().setLanguageCode(userService.getLocale(inlineQuery.getFrom().getId()));
                localeService.setLocale(Locale.forLanguageTag(inlineQuery.getFrom().getLanguageCode()));
            } else {
                return;
            }

            if (inlineQuery.getQuery().equals("history")) {
                profileService.clickHistory(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().equals("read") || inlineQuery.getQuery().equals("planned") || inlineQuery.getQuery().equals("finished") || inlineQuery.getQuery().equals("postponed")) {
                profileService.getMangaByStatus(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\nsort")) {
                serviceConfig.sortFilterServices().get(util.getSourceName(inlineQuery.getQuery())).getSortParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\nstatus")) {
                serviceConfig.sortFilterServices().get(util.getSourceName(inlineQuery.getQuery())).getStatusParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\ngenre")) {
                serviceConfig.sortFilterServices().get(util.getSourceName(inlineQuery.getQuery())).getGenreParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\ntype")) {
                sortFilterDesuMeService.getTypeParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\ncontentRating")) {
                sortFilterMangaDexService.getContentRatingParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\nmagazineDemographic")) {
                sortFilterMangaDexService.getMagazineDemographicParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\nformat")) {
                sortFilterMangaDexService.getFormatParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\ntheme")) {
                sortFilterMangaDexService.getThemeParams(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("\nmangaId:\n")) {
                serviceConfig.mangaServices().get(util.getSourceName(inlineQuery.getQuery())).getMangaChaptersButton(inlineQuery);
                return;
            }

            if (inlineQuery.getQuery().contains("SetLanguageCode")) {
                mangaService.chooseLanguageCodeMangaDex(inlineQuery);
                return;
            }

            serviceConfig.mangaServices().get(user.getCurrentMangaCatalog()).getSearchResult(inlineQuery, user);
        } catch (CatalogNotFoundException e) {
            log.error("Ошибка при получении каталога", e);
            util.sendErrorMessage("Произошла ошибка при получении каталога, введен неправильный запрос или что-то другое. Попробуй еще раз и, если ошибка повторится, то обратись в поддержку", user.getUserId());
        } catch (Exception e) {
            log.error("Неизвестная ошибка в InlineQueryHandler()", e);
            util.sendErrorMessage("Произошла ошибка при поиске главы, введен неправильный запрос или что-то другое. Перезапусти бот и, если ошибка повторится, то напиши в поддержку.", user.getUserId());
        }
    }

    @Override
    public void choose(List<InlineQuery> t) {

    }
}
