package com.suttori.handler;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class MediaGroupHandler implements Handler<Update> {

    @Override
    public void choose(List<Update> updates) {
    }

    @Override
    public void choose(Update update) {
    }

}
