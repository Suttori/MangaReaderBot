package com.suttori.telegram;


import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegraph.api.methods.CreatePage;
import org.telegram.telegraph.api.methods.EditPage;
import org.telegram.telegraph.api.methods.GetPage;

import java.util.Map;

@FeignClient(name = "telegraphApiFeignClient", url = "https://api.telegra.ph")
public interface TelegraphApiFeignClient {

    @RequestMapping(method = RequestMethod.POST, value = "/createPage")
    Response createPage(@RequestBody CreatePage createPage);

    @RequestMapping(method = RequestMethod.POST, value = "/getPage")
    Response getPage(@RequestBody GetPage getPage);

    @RequestMapping(method = RequestMethod.POST, value = "/editPage")
    Response editPage(@RequestBody EditPage editPage);

}


