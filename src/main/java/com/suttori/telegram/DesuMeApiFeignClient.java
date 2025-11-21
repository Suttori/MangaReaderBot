package com.suttori.telegram;

import com.suttori.config.FeignConfig;;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "desuMeApiFeignClient", url = "https://x.desu.city/", configuration = FeignConfig.class)
public interface DesuMeApiFeignClient {

    @RequestMapping(method = RequestMethod.GET, value = "/manga/api/")
    Response searchManga(@RequestParam Map<String, String> params);

    @RequestMapping(method = RequestMethod.GET, value = "/manga/api/{id}")
    Response getMangaById(@PathVariable String id);

    @RequestMapping(method = RequestMethod.GET, value = "/manga/api/{mangaId}/chapter/{chapterId}")
    Response getChapter(@PathVariable String mangaId, @PathVariable String chapterId);

}


