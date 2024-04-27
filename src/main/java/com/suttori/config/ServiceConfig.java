package com.suttori.config;

import com.suttori.service.DesuMeService;
import com.suttori.service.MangaDexService;
import com.suttori.service.interfaces.MangaServiceInterface;
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

    @Autowired
    public ServiceConfig(DesuMeService desuMeService, MangaDexService mangaDexService) {
        this.desuMeService = desuMeService;
        this.mangaDexService = mangaDexService;
    }

    @Bean
    public Map<String, MangaServiceInterface> mangaServices() {
        Map<String, MangaServiceInterface> mangaServices = new HashMap<>();
        mangaServices.put("desu.me", this.desuMeService);
        mangaServices.put("mangadex.org", this.mangaDexService);
        return mangaServices;
    }
}
