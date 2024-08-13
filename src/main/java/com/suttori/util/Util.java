package com.suttori.util;


import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.suttori.config.ServiceConfig;
import com.suttori.entity.MangaDesu.MangaGenre;
import com.suttori.exception.CatalogNotFoundException;
import com.suttori.service.LocaleService;
import com.suttori.telegram.TelegramSender;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.*;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class Util {

    private TelegramSender telegramSender;


    @Autowired
    public Util(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    public String getPhotoFieldId(Message message) {
        List<PhotoSize> photos = message.getPhoto();
        return photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElseThrow().getFileId();
    }

    public String[] parseValue(String string) {
        return string.split("\n");
    }

    public String getSourceName(String string) throws CatalogNotFoundException {
        String[] parts = string.split("\n");
        if (parts.length == 0) {
            throw new CatalogNotFoundException("Пустая строка без разделителя.");
        }
        String sourceName = parts[0];
        if (sourceName.equals("desu.me") || sourceName.equals("mangadex.org")) {
            return sourceName;
        } else {
            throw new CatalogNotFoundException("Каталог " + sourceName + " не найден");
        }
    }

    public void sendErrorMessage(String error, Long userId) {
        telegramSender.send(SendMessage.builder()
                .chatId(userId)
                .text(error)
                .build());
    }

    public void sendInfoMessage(String error, Long userId) {
        telegramSender.send(SendMessage.builder()
                .chatId(userId)
                .text(error)
                .build());
    }

    public File createStorageFolder(String nameFolder) {
        java.io.File folder = new java.io.File(System.getProperty("user.dir"), nameFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public java.io.File downloadFile(java.io.File folder, URL url, String filePath) {
        java.io.File file = new java.io.File(folder, filePath);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Referer", "https://desu.win/");
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    public java.io.File downloadFileWithoutReferrer(java.io.File folder, URL url, String filePath) {
        java.io.File file = new java.io.File(folder, filePath);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    public ImageData downloadImageWithReferer(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Referer", "https://desu.win/");
            try (InputStream in = connection.getInputStream()) {
                byte[] imageBytes = in.readAllBytes();
                return ImageDataFactory.create(imageBytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ImageData downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try (InputStream in = connection.getInputStream()) {
                byte[] imageBytes = in.readAllBytes();
                return ImageDataFactory.create(imageBytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMessageByMessageId(Long userId, Integer messageId) {
        telegramSender.deleteMessageById(String.valueOf(userId), messageId);
    }

    public org.telegram.telegrambots.meta.api.objects.File getFileById(String fileId) {
        GetFile getFile = new GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File photoFile = telegramSender.sendGetFile(getFile);
        if (photoFile.getFileSize() < 20 * 1024 * 1024) {
            return photoFile;
        } else {
            return null;
        }
    }

    public String getInfoError(Response response) {
        try {
            byte[] responseBodyBytes = response.body().asInputStream().readAllBytes();
            String responseBodyString = new String(responseBodyBytes, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(responseBodyString);
            return jsonObject.get("error_code").toString() + " | " + jsonObject.get("description").toString();
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }


}
