package com.suttori.telegram;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "mangaDexApiFeignClient", url = "https://api.mangadex.org/")
@Headers("User-Agent: Manga Reader Bot")
public interface MangaDexApiFeignClient {

    @RequestMapping(method = RequestMethod.GET, value = "/manga/?includes[]=cover_art")
    Response searchMangaIncludesCoverArt(@RequestParam Map<String, List<String>> params);

    @RequestMapping(method = RequestMethod.GET, value = "/manga/{mangaId}?includes[]=cover_art")
    Response getMangaById(@PathVariable String mangaId);

    @RequestMapping(method = RequestMethod.GET, value = "/manga/{mangaId}/aggregate")
    Response getChapterListAggregate(@PathVariable String mangaId, @RequestParam Map<String, List<String>> params);

    @RequestMapping(method = RequestMethod.GET, value = "/at-home/server/{chapterId}")
    Response getChapterPageIds(@PathVariable String chapterId);

    @RequestMapping(method = RequestMethod.GET, value = "/manga/tag/")
    Response getTagsId();


}


