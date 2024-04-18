package com.suttori.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suttori.config.BotConfig;
import com.suttori.dao.*;
import com.suttori.entity.*;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SenderService {

    private MessageService messageService;
    private TelegramSender telegramSender;
    private Util util;
    private BotConfig botConfig;
    private MangaService mangaService;

    private final Logger logger = LoggerFactory.getLogger(SenderService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepositoryInterface postRepositoryInterface;
    @Autowired
    private MediaGroupRepository mediaGroupRepository;
    @Autowired
    private MessageEntitiesRepository messageEntitiesRepository;
    @Autowired
    private TelegramApiFeignClient telegramApiFeignClient;


    public SenderService(MessageService messageService, TelegramSender telegramSender, Util util, BotConfig botConfig, MangaService mangaService) {
        this.messageService = messageService;
        this.telegramSender = telegramSender;
        this.util = util;
        this.botConfig = botConfig;
        this.mangaService = mangaService;
    }

    public List<User> publish(CallbackQuery callbackQuery) {
        long startTime = System.nanoTime();
        EditMessageText prepare = new EditMessageText("");
        prepare.setText("Подготовка к рассылке...");
        prepare.setMessageId(callbackQuery.getMessage().getMessageId());
        prepare.setChatId(callbackQuery.getMessage().getChatId());
        telegramSender.sendEditMessageText(prepare);

        List<User> users = userRepository.findAll();
        List<User> userList = telegramSender.prepareSendAdsThread(users, ((Message) callbackQuery.getMessage()));


        long endTime = System.nanoTime();
        Duration duration = Duration.ofNanos(endTime - startTime);
        String formattedTime = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.MIDNIGHT.plus(duration));

        EditMessageText inProcess = new EditMessageText("Ушло на подготовку:" + formattedTime + "\nРассылаю...");
        inProcess.setMessageId(callbackQuery.getMessage().getMessageId());
        inProcess.setChatId(callbackQuery.getMessage().getChatId());
        telegramSender.sendEditMessageText(inProcess);
        return userList;

    }

    public void clickCreateSendAds(CallbackQuery callbackQuery) {
        EditMessageText editMessageText = messageService.createEditMessageWithButton("Отправь пост для рассылки", "Отменить",
                callbackQuery.getMessage().getMessageId(), "cancel_create_send_ads", callbackQuery.getMessage().getChatId());
        telegramSender.sendEditMessageText(editMessageText);
        userRepository.setPosition("CREATE_CREATIVE", callbackQuery.getMessage().getChatId());
    }

    public void cancelCreateCreative(CallbackQuery callbackQuery) {
        telegramSender.sendEditMessage(messageService.createEditMessageWithoutButton("Создание рассылки отменено", callbackQuery.getMessage().getMessageId(), callbackQuery.getMessage().getChatId()));
        userRepository.setPosition("DEFAULT", callbackQuery.getMessage().getChatId());
    }

    public void saveCreative(Message message) {
        Post post;
        if (message.hasText()) {
            post = new Post(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), true, new Timestamp(System.currentTimeMillis()),
                    true, false, message.getMessageId(), false);
            Post savedPost = postRepositoryInterface.save(post);
            if (message.getEntities() != null) {
                saveMessageEntities(message.getEntities(), savedPost);
            }
        } else {
            post = new Post(message.getChatId(), message.getFrom().getId(), message.getMessageId(), false, message.getCaption(), new Timestamp(System.currentTimeMillis()),
                    true, false, message.getMessageId(), false);
            post.setMessageFromChatId(message.getMessageId());
            Post savedPost = postRepositoryInterface.save(post);
            if (message.getCaptionEntities() != null) {
                saveMessageEntities(message.getCaptionEntities(), savedPost);
            }
        }
        SendMessage sendMessage = messageService.createSendMessageWithMaxLength("Теперь отправь кнопки, если кнопок нет, то отправь -", message.getFrom().getId());
        telegramSender.send(sendMessage);
        userRepository.setPosition("TAKE_BUTTON", message.getChatId());
    }

    public void saveMediaGroupCreative(List<Update> updates) {
        Message message = updates.get(0).getMessage();

        Post post = new Post(message.getChatId(), message.getFrom().getId(), message.getMessageId(), false, message.getCaption(), new Timestamp(System.currentTimeMillis()),
                true, false, message.getMessageId(), true);
        post = postRepositoryInterface.save(post);

        for (Update update : updates) {
            if (update.getMessage().getPhoto() != null) {
                MediaGroup mediaGroup = new MediaGroup(post.getId(), message.getChatId(), util.getPhotoFieldId(update.getMessage()));
                mediaGroup.setPhoto(true);
                mediaGroupRepository.save(mediaGroup);
            } else if (update.getMessage().getVideo() != null) {
                MediaGroup mediaGroup = new MediaGroup(post.getId(), message.getChatId(), update.getMessage().getVideo().getFileId());
                mediaGroup.setVideo(true);
                mediaGroupRepository.save(mediaGroup);
            } else if (update.getMessage().getAudio() != null) {
                MediaGroup mediaGroup = new MediaGroup(post.getId(), message.getChatId(), update.getMessage().getAudio().getFileId());
                mediaGroup.setVideo(true);
                mediaGroupRepository.save(mediaGroup);
            } else if (update.getMessage().getDocument() != null) {
                MediaGroup mediaGroup = new MediaGroup(post.getId(), message.getChatId(), update.getMessage().getDocument().getFileId());
                mediaGroup.setVideo(true);
                mediaGroupRepository.save(mediaGroup);
            } else {
                System.out.println("Неизвестный файл");
            }
        }
        if (message.getCaptionEntities() != null) {
            saveMessageEntities(message.getCaptionEntities(), post);
        }

        userRepository.setPosition("DEFAULT", message.getChatId());
        getPost(post);
        SendMessage sendMessage = messageService.createSendMessageWithTwoButtons("Что сделать с постом?", "Отмена", "Опубликовать",
                "cancelSendAds", "publish", message.getChatId());
        telegramSender.send(sendMessage);
    }

    public void saveMessageEntities(List<MessageEntity> messageEntities, Post savedPost) {
        for (MessageEntity messageEntity : messageEntities) {
            MessageFormatting messageFormatting = new MessageFormatting(savedPost.getId(), messageEntity.getType(), messageEntity.getOffset(), messageEntity.getLength(),
                    messageEntity.getText(), messageEntity.getUrl(), messageEntity.getLanguage(), messageEntity.getCustomEmojiId(), false);
            messageEntitiesRepository.save(messageFormatting);
        }
    }

    @Transactional
    public void takeButtonCreative(Message message) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Репост :x:")).callbackData("forward").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Отмена").callbackData("cancelSendAds").build(),
                        InlineKeyboardButton.builder().text("Опубликовать").callbackData("publish").build())
        )));

        Post post = postRepositoryInterface.findFirstByChatIdOrderByTimeCreateDesc(message.getChatId());
        post.setButton(message.getText());
        userRepository.setPosition("DEFAULT", message.getChatId());
        getPost(post);

        telegramSender.send(SendMessage.builder()
                .text("Что сделать с постом?")
                .chatId(message.getChatId())
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    @Transactional
    public void forward(CallbackQuery callbackQuery) {
        Post post = postRepositoryInterface.findFirstByChatIdOrderByTimeCreateDesc(callbackQuery.getMessage().getChatId());
        InlineKeyboardMarkup inlineKeyboardMarkup;
        if (post.getForwardMessage() == null) {
            post.setForwardMessage(true);
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Репост :white_check_mark:")).callbackData("forward").build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text("Отмена").callbackData("cancelSendAds").build(),
                            InlineKeyboardButton.builder().text("Опубликовать").callbackData("publish").build())
            )));
        } else {
            post.setForwardMessage(null);
            inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Репост :x:")).callbackData("forward").build()),
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text("Отмена").callbackData("cancelSendAds").build(),
                            InlineKeyboardButton.builder().text("Опубликовать").callbackData("publish").build())
            )));
        }
        getPost(post);
        telegramSender.send(SendMessage.builder()
                .text("Что сделать с постом?")
                .chatId(callbackQuery.getMessage().getChatId())
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public void getPost(Post post) {
        if (post.isTextMessage() && post.getForwardMessage() == null) {
            SendMessage sendMessage = sendMessage(post);
            sendMessage.setReplyMarkup(new InlineKeyboardMarkup(createCustomButton(post.getButton(), new ArrayList<>())));
            telegramSender.send(sendMessage);
        } else if (post.isMediaGroup()) {
            SendMediaGroup sendMediaGroup = sendMediaGroup(post);
            telegramSender.sendMediaGroup(sendMediaGroup);
        } else if (post.getForwardMessage() != null) {
            ForwardMessage forwardMessage = forwardMessage(post);
            telegramSender.sendForwardMessage(forwardMessage);
        } else {
            CopyMessage copyMessage = copyMessage(post);
            copyMessage.setReplyMarkup(new InlineKeyboardMarkup(createCustomButton(post.getButton(), new ArrayList<>())));
            telegramSender.sendCopyMessage(copyMessage);
        }
    }

    public void clickPublish(CallbackQuery callbackQuery) {
        long startTime = System.nanoTime();
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Подготовка к рассылке...")
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());

        List<User> users = userRepository.findAllByPremiumBotUserIsNull();
        List<User> userList = telegramSender.prepareSendAdsThread(users, (Message) callbackQuery.getMessage());

        long endTime = System.nanoTime();
        String prepareText = "Ушло на подготовку:" + DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.MIDNIGHT.plus(Duration.ofNanos(endTime - startTime))) + "\nДолжны получить: " + userList.size() + "\nРассылаю...";
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text(prepareText)
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());


        startTime = System.nanoTime();
        int counter = 0;
        Post post = postRepositoryInterface.findFirstByChatIdOrderByTimeCreateDesc(callbackQuery.getMessage().getChatId());

        ObjectMapper objectMapper = new ObjectMapper();
        String inlineKeyboardMarkup = null;
        String inputMedia = null;
        String entities = null;

        if (post.getButton() == null) {
        } else if (!post.getButton().equals("-")) {
            try {
                inlineKeyboardMarkup = objectMapper.writeValueAsString(new InlineKeyboardMarkup(createCustomButton(post.getButton(), new ArrayList<>())));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        if (post.isMediaGroup()) {
            try {
                inputMedia = objectMapper.writeValueAsString(getInputMedia(post));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        try {
            entities = objectMapper.writeValueAsString(getMessageEntity(post));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        for (User user : userList) {
            Response response = sendPostInChannel(post, user.getUserId(), inlineKeyboardMarkup, entities, inputMedia);
            switch (response.status()) {
                case 200:
                    counter++;
                    logger.info("Message delivered successfully to " + user.getUserId() + " try " + counter);
                    break;
                case 403:
                    logger.error("Message was NOT delivered to " + user.getUserId() + " | " + util.getInfoError(response) + " try " + counter);
                    break;
                case 429:
                    logger.error("Message was NOT delivered to " + user.getUserId() + " | " + util.getInfoError(response) + " try " + counter);
                    try {
                        Thread.sleep(7000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    logger.error("Message was NOT delivered to " + user.getUserId() + " | " + util.getInfoError(response) + " try " + counter);
                    break;

            }
        }

        endTime = System.nanoTime();
        String publishText = "Рассылка завершена\n" + "Длительность: " + DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.MIDNIGHT.plus(Duration.ofNanos(endTime - startTime))) + "\nПолучили: " + counter;
        telegramSender.send(SendMessage.builder()
                .text(prepareText + "\n\n" + publishText)
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public List<InputMedia> getInputMedia(Post post) {
        List<InputMedia> inputMediaList = new LinkedList<>();
        int oneTime = 0;
        for (MediaGroup mediaGroup : mediaGroupRepository.findMediaGroupByPostId(post.getId())) {
            if (mediaGroup.isPhoto()) {
                InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(mediaGroup.getFieldId());
                if (oneTime == 0) {
                    inputMediaPhoto.setCaption(post.getCaption());
                    inputMediaPhoto.setCaptionEntities(getMessageEntity(post));
                    oneTime = 1;
                }
                inputMediaList.add(inputMediaPhoto);
            } else if (mediaGroup.isVideo()) {
                InputMediaVideo inputMediaVideo = new InputMediaVideo(mediaGroup.getFieldId());
                if (oneTime == 0) {
                    inputMediaVideo.setCaption(post.getCaption());
                    inputMediaVideo.setCaptionEntities(getMessageEntity(post));
                    oneTime = 1;
                }
                inputMediaList.add(inputMediaVideo);
            } else if (mediaGroup.isAudio()) {
                InputMediaAudio inputMediaAudio = new InputMediaAudio(mediaGroup.getFieldId());
                inputMediaAudio.setCaption(post.getCaption());
                inputMediaAudio.setCaptionEntities(getMessageEntity(post));
                inputMediaList.add(inputMediaAudio);
            } else if (mediaGroup.isDocument()) {
                InputMediaDocument inputMediaDocument = new InputMediaDocument(mediaGroup.getFieldId());
                inputMediaDocument.setCaption(post.getCaption());
                inputMediaDocument.setCaptionEntities(getMessageEntity(post));
                inputMediaList.add(inputMediaDocument);
            }
        }
        return inputMediaList;
    }

    public Response sendPostInChannel(Post post, Long userId, String inlineKeyboardMarkup, String entities, String inputMedia) {
        if (post.isTextMessage() && post.getForwardMessage() == null) {
            return telegramApiFeignClient.sendMessage(botConfig.getToken(), userId, post.getText(), entities, post.getDisableWebPagePreview(), post.getDisableNotification(), inlineKeyboardMarkup);
        } else if (post.getForwardMessage() != null) {
            return telegramApiFeignClient.forwardMessage(botConfig.getToken(), userId, post.getFromChatId(), post.getMessageId());
        } else if (post.isMediaGroup()) {
            return telegramApiFeignClient.sendMediaGroup(botConfig.getToken(), userId, inputMedia);
        } else {
            return telegramApiFeignClient.copyMessage(botConfig.getToken(), userId, post.getFromChatId(), post.getMessageId(), post.getCaption(), entities, inlineKeyboardMarkup);
        }
    }

    public void sendNotificationToUsers(List<Long> userList, Long mangaId, Long lastChapter) {
        String inlineKeyboardMarkup = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            inlineKeyboardMarkup = objectMapper.writeValueAsString(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                   new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Перейти к манге")).callbackData("sendMangaById\n" + mangaId).build())
            ))));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        int counter = 0;

        Post post = new Post();
        post.setText("В манге \"" + mangaService.getMangaData(mangaId).getRussian() + "\" вышла " + lastChapter + " глава!");
        post.setTextMessage(true);
        post.setDisableWebPagePreview(true);
        post.setDisableNotification(false);

        for (Long userId : userList) {
            Response response = sendPostInChannel(post, userId, inlineKeyboardMarkup, null, null);
            switch (response.status()) {
                case 200:
                    counter++;
                    logger.info("Message delivered successfully to " + userId + " try " + counter);
                    break;
                case 403:
                    logger.error("Message was NOT delivered to " + userId + " | " + util.getInfoError(response) + " try " + counter);
                    break;
                case 429:
                    logger.error("Message was NOT delivered to " + userId + " | " + util.getInfoError(response) + " try " + counter);
                    try {
                        Thread.sleep(7000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    logger.error("Message was NOT delivered to " + userId + " | " + util.getInfoError(response) + " try " + counter);
                    break;

            }
        }
    }

    public List<MessageEntity> getMessageEntity(Post post) {
        List<MessageEntity> messageEntityList = new ArrayList<>();
        List<MessageFormatting> messageFormattingList = messageEntitiesRepository.findMessageFormattingByPostId(post.getId());

        for (MessageFormatting messageFormatting : messageFormattingList) {
            messageEntityList.add(MessageEntity.builder()
                    .type(messageFormatting.getType())
                    .offset(messageFormatting.getStart())
                    .length(messageFormatting.getLength())
                    .url(messageFormatting.getUrl())
                    .language(messageFormatting.getLanguage())
                    .text(messageFormatting.getText())
                    .customEmojiId(messageFormatting.getCustomEmojiId())
                    .build());
        }
        return messageEntityList;
    }

    public List<InlineKeyboardRow> createCustomButton(String buttons, List<InlineKeyboardRow> keyboard) {
        if (buttons.equals("-")) {
            return new ArrayList<>();
        }
        String[] buttonN = buttons.split("\\n");
        InlineKeyboardButton customButton;
        InlineKeyboardRow rowInLine;
        String[] buttonDelimiter;
        String[] buttonDelimiter1;

        for (String s : buttonN) {
            rowInLine = new InlineKeyboardRow();
            buttonDelimiter = s.split("\\s+\\|\\s+");
            for (String value : buttonDelimiter) {
                buttonDelimiter1 = value.split("\\s+-\\s+");
                for (int k = 0; k < buttonDelimiter1.length; k++) {
                    customButton = new InlineKeyboardButton(buttonDelimiter1[k]);
                    customButton.setUrl(buttonDelimiter1[k + 1]);
                    rowInLine.add(customButton);
                    k++;
                }
            }
            keyboard.add(rowInLine);
        }
        return keyboard;
    }

    public CopyMessage copyMessage(Post post) {
        return CopyMessage.builder()
                .chatId(post.getChatId())
                .fromChatId(post.getFromChatId())
                .messageId(post.getMessageId())
                .messageThreadId(post.getMessageThreadId())
                .caption(post.getCaption())
                .disableNotification(post.getDisableNotification())
                .captionEntities(getMessageEntity(post)).build();
//                .parseMode("HTML")
        //  .replyMarkup(post.getReplyMarkup())
    }

    public SendMessage sendMessage(Post post) {
        return SendMessage.builder()
                .chatId(post.getChatId())
                .messageThreadId(post.getMessageThreadId())
                .text(post.getText())
                .disableNotification(post.getDisableNotification())
                .disableWebPagePreview(post.getDisableWebPagePreview())
                .entities(getMessageEntity(post)).build();
        // .protectContent(message.getHasProtectedContent())
    }

    public SendMediaGroup sendMediaGroup(Post post) {
        List<InputMedia> inputMediaList = new LinkedList<>();
        int oneTime = 0;
        for (MediaGroup mediaGroup : mediaGroupRepository.findMediaGroupByPostId(post.getId())) {
            if (mediaGroup.isPhoto()) {
                InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(mediaGroup.getFieldId());
                if (oneTime == 0) {
                    inputMediaPhoto.setCaption(post.getCaption());
                    inputMediaPhoto.setCaptionEntities(getMessageEntity(post));
                    oneTime = 1;
                }
                inputMediaList.add(inputMediaPhoto);
            } else if (mediaGroup.isVideo()) {
                InputMediaVideo inputMediaVideo = new InputMediaVideo(mediaGroup.getFieldId());
                if (oneTime == 0) {
                    inputMediaVideo.setCaption(post.getCaption());
                    inputMediaVideo.setCaptionEntities(getMessageEntity(post));
                    oneTime = 1;
                }
                inputMediaList.add(inputMediaVideo);
            } else if (mediaGroup.isAudio()) {
                InputMediaAudio inputMediaAudio = new InputMediaAudio(mediaGroup.getFieldId());
                inputMediaAudio.setCaption(post.getCaption());
                inputMediaAudio.setCaptionEntities(getMessageEntity(post));
                inputMediaList.add(inputMediaAudio);
            } else if (mediaGroup.isDocument()) {
                InputMediaDocument inputMediaDocument = new InputMediaDocument(mediaGroup.getFieldId());
                inputMediaDocument.setCaption(post.getCaption());
                inputMediaDocument.setCaptionEntities(getMessageEntity(post));
                inputMediaList.add(inputMediaDocument);
            }
        }
        return SendMediaGroup.builder()
                .medias(inputMediaList)
                .chatId(post.getChatId())
                .disableNotification(post.getDisableNotification()).build();
    }

    public ForwardMessage forwardMessage(Post post) {
        return ForwardMessage.builder()
                .chatId(post.getChatId())
                .messageId(post.getMessageId())
                .disableNotification(post.getDisableNotification())
                .fromChatId(post.getFromChatId())
                .messageThreadId(post.getMessageThreadId())
                .build();
    }
}
