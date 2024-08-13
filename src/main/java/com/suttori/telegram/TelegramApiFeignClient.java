package com.suttori.telegram;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;

import org.springframework.web.bind.annotation.*;

@FeignClient(name = "telegramApiFeignClient", url = "https://api.telegram.org")
public interface TelegramApiFeignClient {

    @RequestMapping(method = RequestMethod.GET, value = "/bot{botToken}/sendChatAction?chat_id={chatId}&action={action}")
    Response sendChatAction(@PathVariable("botToken") String botToken, @RequestParam("chatId") Long chatId, @RequestParam("action") String action);

    @RequestMapping(method = RequestMethod.GET, value = "/bot{botToken}/deleteMessage?chat_id={chatId}&message_id={messageId}")
    Response delete(@PathVariable("botToken") String botToken, @PathVariable("chatId") Long chatId, @PathVariable("messageId") Integer messageId);


    @RequestMapping(method = RequestMethod.GET, value = "/bot{botToken}/sendMessage?chat_id={chatId}&text={text}&entities={entities}&disable_web_page_preview={disable_web_page_preview}&disable_notification={disable_notification}&inline_keyboard={inline_keyboard}")
    Response sendMessage(@PathVariable("botToken") String botToken, @RequestParam("chatId") Long chatId, @RequestParam("text") String text, @RequestParam("entities") String entities, @RequestParam("disable_web_page_preview") Boolean disable_web_page_preview, @RequestParam("disable_notification") Boolean disable_notification, @RequestParam("reply_markup") String inline_keyboard);

    @RequestMapping(method = RequestMethod.GET, value = "/bot{botToken}/forwardMessage?chat_id={chatId}&from_chat_id={from_chat_id}&message_id={message_id}")
    Response forwardMessage(@PathVariable("botToken") String botToken, @RequestParam("chatId") Long chatId, @RequestParam("from_chat_id") Long from_chat_id, @RequestParam("message_id") Integer message_id);

    @RequestMapping(method = RequestMethod.GET, value = "/bot{botToken}/sendMediaGroup?chat_id={chatId}&media={media}")
    Response sendMediaGroup(@PathVariable("botToken") String botToken, @RequestParam("chatId") Long chatId, @RequestParam("media") String media);

    @RequestMapping(method = RequestMethod.GET, value = "/bot{botToken}/copyMessage?chat_id={chatId}&from_chat_id={from_chat_id}&message_id={message_id}&caption={caption}&caption_entities={caption_entities}&inline_keyboard={inline_keyboard}")
    Response copyMessage(@PathVariable("botToken") String botToken, @RequestParam("chatId") Long chatId, @RequestParam("from_chat_id") Long from_chat_id, @RequestParam("message_id") Integer message_id, @RequestParam("caption") String caption, @RequestParam("caption_entities") String caption_entities, @RequestParam("reply_markup") String inline_keyboard);


}



