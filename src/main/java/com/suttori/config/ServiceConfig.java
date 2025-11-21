package com.suttori.config;

import com.suttori.service.*;
import com.suttori.service.interfaces.MangaServiceInterface;
import com.suttori.service.interfaces.SortFilterInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource("classpath:application.properties")
public class ServiceConfig {

    private DesuMeService desuMeService;
    private MangaDexService mangaDexService;
    private UsagiService usagiService;
    private SortFilterDesuMeService sortFilterDesuMeService;
    private SortFilterMangaDexService sortFilterMangaDexService;

    @Autowired
    public ServiceConfig(DesuMeService desuMeService, MangaDexService mangaDexService, UsagiService usagiService, SortFilterDesuMeService sortFilterDesuMeService, SortFilterMangaDexService sortFilterMangaDexService) {
        this.desuMeService = desuMeService;
        this.mangaDexService = mangaDexService;
        this.usagiService = usagiService;
        this.sortFilterDesuMeService = sortFilterDesuMeService;
        this.sortFilterMangaDexService = sortFilterMangaDexService;
    }

    @Bean
    public Map<String, MangaServiceInterface> mangaServices() {
        Map<String, MangaServiceInterface> mangaServices = new HashMap<>();
        mangaServices.put("desu.me", this.desuMeService);
        mangaServices.put("mangadex.org", this.mangaDexService);
        mangaServices.put("usagi", this.usagiService);
        return mangaServices;
    }

    @Bean
    public Map<String, SortFilterInterface> sortFilterServices() {
        Map<String, SortFilterInterface> sortFilterServices = new HashMap<>();
        sortFilterServices.put("desu.me", this.sortFilterDesuMeService);
        sortFilterServices.put("mangadex.org", this.sortFilterMangaDexService);
        return sortFilterServices;
    }


}
