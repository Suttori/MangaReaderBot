package com.suttori.telegram;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "uploadsMangaDexApiFeignClient", url = "https://uploads.mangadex.org/")
@Headers("User-Agent: Manga Reader Bot")
public interface UploadMangaDexFeignClient {

    @RequestMapping(method = RequestMethod.GET, value = "/data/{hash}/{pageData}")
    Response getPage(@PathVariable String hash, @PathVariable String pageData);


}


