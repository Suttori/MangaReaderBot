package com.suttori.service;


import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
public class LocaleService {
    private ResourceBundle bundle;
    public LocaleService() {
        // Задаем локаль по умолчанию (английский язык)
        Locale locale = Locale.getDefault();
        // Загружаем файл с локализованными строками
        bundle = ResourceBundle.getBundle("resources", locale);
    }

    public String getBundle(String key) {
        return bundle.getString(key);
    }

    public void setLocale(Locale locale) {
        // Обновляем локализованные строки для выбранной локали
        bundle = ResourceBundle.getBundle("resources", locale);
    }
}
