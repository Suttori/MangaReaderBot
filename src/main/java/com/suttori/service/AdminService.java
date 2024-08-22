package com.suttori.service;

import com.suttori.dao.*;
import com.suttori.entity.*;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AdminService {

    private TelegramSender telegramSender;
    private MessageService messageService;
    private Util util;
    private StatisticEntityRepository statisticEntityRepository;
    private MangaChapterRepository mangaChapterRepository;
    private ReferralChannelRepository referralChannelRepository;
    private UserRepository userRepository;
    private ReferralLinkRepository referralLinkRepository;
    private AdvertiserRepository advertiserRepository;
    private PostToDeleteRepository postToDeleteRepository;
    private PostRepositoryInterface postRepositoryInterface;
    private MediaGroupRepository mediaGroupRepository;
    private MessageEntitiesRepository messageEntitiesRepository;
    private LastActivityRepository lastActivityRepository;

    @Autowired
    public AdminService(TelegramSender telegramSender, MessageService messageService, Util util,
                        StatisticEntityRepository statisticEntityRepository, MangaChapterRepository mangaChapterRepository,
                        ReferralChannelRepository referralChannelRepository, UserRepository userRepository,
                        ReferralLinkRepository referralLinkRepository, AdvertiserRepository advertiserRepository,
                        PostToDeleteRepository postToDeleteRepository, PostRepositoryInterface postRepositoryInterface,
                        MediaGroupRepository mediaGroupRepository, MessageEntitiesRepository messageEntitiesRepository,
                        LastActivityRepository lastActivityRepository) {
        this.telegramSender = telegramSender;
        this.messageService = messageService;
        this.util = util;
        this.statisticEntityRepository = statisticEntityRepository;
        this.mangaChapterRepository = mangaChapterRepository;
        this.referralChannelRepository = referralChannelRepository;
        this.userRepository = userRepository;
        this.referralLinkRepository = referralLinkRepository;
        this.advertiserRepository = advertiserRepository;
        this.postToDeleteRepository = postToDeleteRepository;
        this.postRepositoryInterface = postRepositoryInterface;
        this.mediaGroupRepository = mediaGroupRepository;
        this.messageEntitiesRepository = messageEntitiesRepository;
        this.lastActivityRepository = lastActivityRepository;
    }

    public boolean isAdvertiser(Long userId) {
        return advertiserRepository.existsByUserid(userId);
    }

    public void addRemoveAdvertiser(Message message) {
        String text;
        String userName = util.parseValue(message.getText())[1];
        Long userId = userRepository.findByUserName(userName).getUserId();
        if (userId == null) {
            telegramSender.send(SendMessage.builder()
                    .text("Не запустил бот или поменял юзер")
                    .chatId(message.getFrom().getId()).build());
            return;
        }

        if (!advertiserRepository.existsByUserid(userId)) {
            advertiserRepository.save(new Advertiser(userRepository.findByUserName(userName).getUserId(), userName));
            text = userName + " успешно добавлен";
        } else {
            advertiserRepository.deleteByUserid(userId);
            text = userName + " успешно удален";
        }
        telegramSender.send(SendMessage.builder()
                .text(text)
                .chatId(message.getFrom().getId()).build());
    }

    public void getStatForAdvertiser(Message message) {
        DateTimeFormatter formatterForUser = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        List<ReferralLink> referralLinkList = referralLinkRepository.findAllContainsName(message.getFrom().getUserName());

        for (ReferralLink referralLink : referralLinkList) {

            List<User> users = userRepository.findAllByReferral(referralLink.getName());
            List<User> usersSubscribe = Optional.ofNullable(users).stream().flatMap(Collection::stream)
                    .filter(user -> {
                        try {
                            return user.getAccessStatus();
                        } catch (NullPointerException e) {
                            return false;
                        }
                    })
                    .toList();

            telegramSender.send(SendMessage.builder()
                    .text("Ссылка: " + referralLink.getLink() + "\n" + "Нажали старт: " + users.size() + "\n" + "Прошли оп: " + usersSubscribe.size() + "\n" + "Создана: " + referralLink.getCreateTime().toLocalDateTime().format(formatterForUser))
                    .chatId(message.getFrom().getId()).build());
        }
    }

    public void createAdminPanel(Message message) {
        userRepository.setPosition("DEFAULT_POSITION", message.getChatId());
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Добавить канал")).callbackData("addChannel").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать каналы")).callbackData("chooseChannel").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Заменить ссылку")).callbackData("createLink").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Статистика")).callbackData("statistics").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Создать рассылку")).callbackData("createSendAds").build(),
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Удалить стикерпак")).callbackData("clickDeleteStickerSetFromCatalog").build())
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Инфо и баны")).callbackData("clickInfoAndBans").build())
        )));

        telegramSender.send(SendMessage.builder()
                .text("Админ панель")
                .chatId(message.getChatId())
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public void createAdminPanelCallbackQuery(CallbackQuery callbackQuery) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Добавить канал")).callbackData("addChannel").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Выбрать каналы")).callbackData("chooseChannel").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Заменить ссылку")).callbackData("createLink").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Статистика")).callbackData("statistics").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Создать рассылку")).callbackData("createSendAds").build(),
                        //InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Удалить стикерпак")).callbackData("clickDeleteStickerSetFromCatalog").build())
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Инфо и баны")).callbackData("clickInfoAndBans").build())
        )));

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Админ панель")
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId())
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public void addChannel(Update update) {
        String errorMessage;
        Message message = update.getMessage();

        if (message.getForwardFrom() != null && message.getForwardFrom().getIsBot()) {
            ReferralChannel channel = new ReferralChannel(message.getForwardFrom().getId(), message.getForwardFrom().getFirstName(), message.getForwardFrom().getUserName(),
                    message.getChatId(), message.getFrom().getUserName(), null, false, new Timestamp(System.currentTimeMillis()), true);
            referralChannelRepository.save(channel);
            SendMessage sendMessage = SendMessage.builder()
                    .text("Бот успешно добавлен. Теперь пришли токен, вот id бота: <code>" + message.getForwardFrom().getId() + "</code>\nСообщение должно выглядеть так: id:токен")
                    .chatId(message.getChatId())
                    .build();
            sendMessage.setParseMode("HTML");
            telegramSender.send(sendMessage);
            userRepository.setPosition("GET_TOKEN", message.getChatId());
            return;
        }

        if (!telegramSender.isBotAdmin(message)) {
            errorMessage = "Бот не является админом в канале";
            SendMessage sendMessage = SendMessage.builder()
                    .text(errorMessage)
                    .chatId(message.getChatId())
                    .build();
            telegramSender.send(sendMessage);
            userRepository.setPosition("DEFAULT", message.getChatId());
            return;
        }

        List<ReferralChannel> channels = referralChannelRepository.findAll();
        for (ReferralChannel channel : channels) {
            if (channel.getChannelId().equals(message.getForwardFromChat().getId()) && channel.getUserId().equals(message.getChatId())) {
                errorMessage = "Канал уже был добавлен в каталог";
                SendMessage sendMessage = SendMessage.builder()
                        .text(errorMessage)
                        .chatId(message.getChatId())
                        .build();
                telegramSender.send(sendMessage);
                userRepository.setPosition("DEFAULT", message.getChatId());
                return;
            }
        }

        ReferralChannel channel = new ReferralChannel(message.getForwardFromChat().getId(), message.getForwardFromChat().getTitle(), message.getForwardFromChat().getUserName(),
                message.getChatId(), message.getFrom().getUserName(), null, false, new Timestamp(System.currentTimeMillis()), false);
        referralChannelRepository.save(channel);
        SendMessage sendMessage = SendMessage.builder()
                .text("Канал успешно добавлен")
                .chatId(message.getChatId())
                .build();
        telegramSender.send(sendMessage);
        userRepository.setPosition("DEFAULT", message.getChatId());
    }

    public void chooseChannel(CallbackQuery callbackQuery) {
        List<ReferralChannel> channelList = referralChannelRepository.findAll();

        channelList.sort(Comparator.comparing(ReferralChannel::getAddChannel));

        EditMessageText sendMessage = new EditMessageText("Выбери каналы, которые нужно добавить в список на обязательную подписку");
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        sendMessage.setMessageId(callbackQuery.getMessage().getMessageId());

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();

        for (int i = 0; i < channelList.size(); i++) {
            var button = new InlineKeyboardButton("...");

            if (channelList.get(i).getEnableChannel()) {
                button.setText(EmojiParser.parseToUnicode(":white_check_mark: " + channelList.get(i).getChannelName()));
            } else {
                button.setText(channelList.get(i).getChannelName());
            }

            button.setCallbackData("click_choose_channel - " + channelList.get(i).getChannelId());

            if (row.size() == 2) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
            row.add(button);
            if (i == channelList.size() - 1) {
                keyboard.add(row);
            }
        }

        var selectAllTrue = new InlineKeyboardButton("Выбрать все");
        var selectAllFalse = new InlineKeyboardButton("Снять все");
        selectAllTrue.setCallbackData("click_choose_channel - allTrue");
        selectAllFalse.setCallbackData("click_choose_channel - allFalse");
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(selectAllTrue);
        row1.add(selectAllFalse);

        var buttonOne = new InlineKeyboardButton("Назад");
        buttonOne.setCallbackData("backAdminPanel");
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(buttonOne);

        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        telegramSender.sendEditMessage(sendMessage);
    }

    public void clickChooseChannel(CallbackQuery callbackQuery) {

        List<ReferralChannel> channelList = referralChannelRepository.findAll();
        String channelId = getParseValue(callbackQuery);
        channelList.sort(Comparator.comparing(ReferralChannel::getAddChannel));

        var editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(String.valueOf(callbackQuery.getMessage().getChatId()));
        editMessageReplyMarkup.setMessageId(callbackQuery.getMessage().getMessageId());

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row = new InlineKeyboardRow();

        if (channelId.equals("allTrue")) {
            for (int i = 0; i < channelList.size(); i++) {
                var button = new InlineKeyboardButton(EmojiParser.parseToUnicode(":white_check_mark: " + channelList.get(i).getChannelName()));
                referralChannelRepository.setEnableChannel(true, channelList.get(i).getChannelId());
                button.setCallbackData("click_choose_channel - " + channelList.get(i).getChannelId());
                referralChannelRepository.setAddInReferralTime(new Timestamp(System.currentTimeMillis()), channelList.get(i).getChannelId());
                if (row.size() == 2) {
                    keyboard.add(row);
                    row = new InlineKeyboardRow();
                }
                row.add(button);
                if (i == channelList.size() - 1) {
                    keyboard.add(row);
                }
            }
        } else if (channelId.equals("allFalse")) {
            for (int i = 0; i < channelList.size(); i++) {
                var button = new InlineKeyboardButton(channelList.get(i).getChannelName());
                referralChannelRepository.setEnableChannel(false, channelList.get(i).getChannelId());
                button.setCallbackData("click_choose_channel - " + channelList.get(i).getChannelId());
                if (row.size() == 2) {
                    keyboard.add(row);
                    row = new InlineKeyboardRow();
                }
                row.add(button);
                if (i == channelList.size() - 1) {
                    keyboard.add(row);
                }
            }
        } else {
            for (int i = 0; i < channelList.size(); i++) {
                InlineKeyboardButton button = null;
                if (channelList.get(i).getChannelId().equals(Long.valueOf(channelId)) && referralChannelRepository.findByChannelId(Long.valueOf(channelId)).getEnableChannel()) {
                    button = new InlineKeyboardButton(channelList.get(i).getChannelName());
                    referralChannelRepository.setEnableChannel(false, channelList.get(i).getChannelId());
                } else if (channelList.get(i).getChannelId().equals(Long.valueOf(channelId))) {
                    button = new InlineKeyboardButton(EmojiParser.parseToUnicode(":white_check_mark: " + channelList.get(i).getChannelName()));
                    referralChannelRepository.setEnableChannel(true, channelList.get(i).getChannelId());
                    referralChannelRepository.setAddInReferralTime(new Timestamp(System.currentTimeMillis()), channelList.get(i).getChannelId());
                } else if (channelList.get(i).getEnableChannel()) {
                    button = new InlineKeyboardButton(EmojiParser.parseToUnicode(":white_check_mark: " + channelList.get(i).getChannelName()));
                } else {
                    button = new InlineKeyboardButton(channelList.get(i).getChannelName());
                }

                button.setCallbackData("click_choose_channel - " + channelList.get(i).getChannelId());

                if (row.size() == 2) {
                    keyboard.add(row);
                    row = new InlineKeyboardRow();
                }
                row.add(button);
                if (i == channelList.size() - 1) {
                    keyboard.add(row);
                }
            }
        }

        var selectAllTrue = new InlineKeyboardButton("Выбрать все");
        var selectAllFalse = new InlineKeyboardButton("Снять все");
        selectAllTrue.setCallbackData("click_choose_channel - allTrue");
        selectAllFalse.setCallbackData("click_choose_channel - allFalse");
        InlineKeyboardRow rowInLine6 = new InlineKeyboardRow();
        rowInLine6.add(selectAllTrue);
        rowInLine6.add(selectAllFalse);


        var back = new InlineKeyboardButton("Назад");
        back.setCallbackData("backAdminPanel");
        InlineKeyboardRow rowInLine7 = new InlineKeyboardRow();
        rowInLine7.add(back);

        keyboard.add(rowInLine6);
        keyboard.add(rowInLine7);

        editMessageReplyMarkup.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramSender.sendEditMessageReplyMarkup(editMessageReplyMarkup);
    }


    public void setLink(CallbackQuery callbackQuery) {
        EditMessageText sendMessage = new EditMessageText("Выбери канал, в котором хочешь поменять ссылку");
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        sendMessage.setMessageId(callbackQuery.getMessage().getMessageId());
        sendMessage.setReplyMarkup(createButtonWithChannel(referralChannelRepository.findAll()));
        telegramSender.sendEditMessage(sendMessage);
    }


    public InlineKeyboardMarkup createButtonWithChannel(List<ReferralChannel> channelList) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int i = 0; i < channelList.size(); i++) {
            var button = new InlineKeyboardButton(channelList.get(i).getChannelName());
            button.setCallbackData("set_channel_link - " + channelList.get(i).getChannelId());
            if (row.size() == 2) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
            row.add(button);
            if (i == channelList.size() - 1) {
                keyboard.add(row);
            }
        }
        return new InlineKeyboardMarkup(keyboard);
    }

    public void getAllLinks(CallbackQuery callbackQuery) {
        EditMessageText editMessageText = new EditMessageText("Ниже показаны все созданные ссылки");
        editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageText.setChatId(callbackQuery.getMessage().getChatId());
        editMessageText.setReplyMarkup(createButtonWithLinks(new ArrayList<>(), referralLinkRepository.findAll(), 1));
        telegramSender.sendEditMessageText(editMessageText);
    }

    public InlineKeyboardMarkup createButtonWithLinks(List<InlineKeyboardRow> keyboard, List<ReferralLink> links, int currentPage) {
        links.sort(Comparator.comparing(ReferralLink::getCreateTime).reversed());
        InlineKeyboardRow calendarRow = new InlineKeyboardRow();
        var createLink = new InlineKeyboardButton(EmojiParser.parseToUnicode("Создать ссылку"));
        createLink.setCallbackData("createReferralLink");
        calendarRow.add(createLink);
        keyboard.add(calendarRow);

        int postsPerPage = 10;
        int startIndex = currentPage * postsPerPage - postsPerPage;
        int endIndex = Math.min(startIndex + postsPerPage, links.size());
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int i = startIndex; i < endIndex; i++) {
            ReferralLink link = links.get(i);
            var button = new InlineKeyboardButton(link.getName());
            button.setCallbackData("click_link - " + link.getId());

            if (row.size() == 2) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
            row.add(button);
            if (i == endIndex - 1) {
                keyboard.add(row);
            }
        }

        InlineKeyboardRow paginationRow = new InlineKeyboardRow();
        if (currentPage > 1) {
            var previousButton = new InlineKeyboardButton("<< Назад");
            previousButton.setCallbackData("click_previous_page - " + (currentPage - 1));
            paginationRow.add(previousButton);
        }
        if (endIndex < links.size()) {
            var nextButton = new InlineKeyboardButton("Дальше >>");
            nextButton.setCallbackData("click_next_page - " + (currentPage + 1));
            paginationRow.add(nextButton);
        }
        if (!paginationRow.isEmpty()) {
            keyboard.add(paginationRow);
        }

        return new InlineKeyboardMarkup(keyboard);
    }

    public void nextOrPrevButton(CallbackQuery callbackQuery) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(callbackQuery.getMessage().getChatId());
        editMessageReplyMarkup.setMessageId(callbackQuery.getMessage().getMessageId());
        int currentPage = Integer.parseInt(getParseValue(callbackQuery));
        List<ReferralLink> postList = referralLinkRepository.findAll();
        editMessageReplyMarkup.setReplyMarkup(createButtonWithLinks(new ArrayList<>(), postList, currentPage));
        telegramSender.sendEditMessageReplyMarkup(editMessageReplyMarkup);
    }

    public void clickLink(CallbackQuery callbackQuery) {
        DateTimeFormatter formatterForUser = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        Optional<ReferralLink> referralLink = referralLinkRepository.findById(Long.valueOf(getParseValue(callbackQuery)));
        List<User> users = userRepository.findAllByReferral(referralLink.get().getName());
        List<User> usersSubscribe = Optional.ofNullable(users).stream().flatMap(Collection::stream)
                .filter(user -> {
                    try {
                        return user.getAccessStatus();
                    } catch (NullPointerException e) {
                        return false;
                    }
                })
                .toList();
        referralLink.get().setStart(users.size());
        referralLink.get().setSubscribe(usersSubscribe.size());
        EditMessageText editMessageText = new EditMessageText("Ссылка: " + referralLink.get().getLink() + "\n" + "Нажали старт: " + referralLink.get().getStart() + "\n" + "Прошли оп: " + referralLink.get().getSubscribe() + "\n" + "Создана: " + referralLink.get().getCreateTime().toLocalDateTime().format(formatterForUser));
        editMessageText.setChatId(callbackQuery.getMessage().getChatId());
        editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageText.setReplyMarkup(messageService.createButton(Collections.singletonList(InlineKeyboardButton.builder().text("Назад").callbackData("backToStatistic").build())));
        telegramSender.sendEditMessageText(editMessageText);
        referralLinkRepository.save(referralLink.get());
    }

    public void linkStat(Message message) {

        List<ReferralLink> referralLinks = referralLinkRepository.findAll();
        referralLinks.sort(Comparator.comparing(ReferralLink::getCreateTime).reversed());
        DateTimeFormatter formatterForUser = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        StringBuilder stringBuilder = new StringBuilder();

        for (ReferralLink referralLink : referralLinks) {
            List<User> users = userRepository.findAllByReferral(referralLink.getName());
            List<User> usersSubscribe = Optional.ofNullable(users).stream().flatMap(Collection::stream)
                    .filter(user -> {
                        try {
                            return user.getAccessStatus();
                        } catch (NullPointerException e) {
                            return false;
                        }
                    })
                    .toList();

            if (!users.isEmpty()) {
                List<User> userListAlive = telegramSender.getStatLink(users);

                stringBuilder.append("Имя: " + referralLink.getName() + "\n")
                        .append("Создана: " + referralLink.getCreateTime().toLocalDateTime().format(formatterForUser) + "\n")
                        .append("Всего юзеров: " + users.size() + "\n")
                        .append("Прошли оп " + usersSubscribe.size() + "\n")
                        .append("Осталось живых " + userListAlive.size() + "\n")
                        .append("Умерли " + (users.size() - userListAlive.size()) + "\n")
                        .append("Процент живых " + ((double) userListAlive.size() / (double) users.size()) * 100 + "%" + "\n\n");
            }
        }

        String text = String.valueOf(stringBuilder);

        if (text.length() <= 1024) {
            telegramSender.send(SendMessage.builder()
                    .text(text)
                    .parseMode("HTML")
                    .chatId(message.getChatId()).build());
        } else {
            boolean firstSend = true;
            // Разделяем сообщение на части по знаку переноса строки
            String[] messageParts = text.split("\n");
            StringBuilder currentPart = new StringBuilder();

            for (String part : messageParts) {
                if (currentPart.length() + part.length() + 1 > 1024) {
                    if (firstSend) {
                        telegramSender.send(SendMessage.builder()
                                .text(currentPart.toString())
                                .parseMode("HTML")
                                .chatId(message.getChatId()).build());
                        firstSend = false;
                    } else {
                        telegramSender.send(SendMessage.builder()
                                .text(currentPart.toString())
                                .parseMode("HTML")
                                .chatId(message.getChatId()).build());
                    }
                    currentPart = new StringBuilder();
                }
                currentPart.append(part).append("\n");
            }
            // Отправляем оставшуюся часть сообщения, если есть
            if (currentPart.length() > 0) {
                telegramSender.send(SendMessage.builder()
                        .text(currentPart.toString())
                        .parseMode("HTML")
                        .chatId(message.getChatId()).build());
            }
        }
    }

    public void changeLink(Message message) {
        ReferralChannel channel = referralChannelRepository.findBySetLink(true);
        referralChannelRepository.setLink(message.getText(), true);
        referralChannelRepository.setSetLink(false, channel.getChannelId());
        ReferralChannel newChannel = referralChannelRepository.findByChannelId(channel.getChannelId());
        telegramSender.send(SendMessage.builder()
                .text("Ссылка успешно заменена. Текущая ссылка: " + newChannel.getLink())
                .chatId(message.getChatId()).build());
        userRepository.setPosition("DEFAULT", message.getChatId());
    }

    public void clickCreateReferralLink(CallbackQuery callbackQuery) {
        EditMessageText editMessageText = new EditMessageText("Отправь мне юзер или имя для названия ссылки, учти что оно должно быть уникальным и напечатано на английском.");
        editMessageText.setChatId(callbackQuery.getMessage().getChatId());
        editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
        telegramSender.sendEditMessageText(editMessageText);
        userRepository.setPosition("CREATE_REFERRAL_LINK", callbackQuery.getMessage().getChatId());
    }

    public void createReferralLink(Message message) {
        String name;
        if (message.getText().startsWith("@")) {
            name = message.getText().substring(1);
        } else {
            name = message.getText();
        }

        ReferralLink referralLink = new ReferralLink();
        referralLink.setName(name);
        referralLink.setLink("https://t.me/MangaManhwa_bot?start=" + name);
        referralLink.setCreateTime(new Timestamp(System.currentTimeMillis()));
        referralLinkRepository.save(referralLink);

        telegramSender.send(SendMessage.builder()
                .text("Ссылка успешно создана\n" + referralLink.getLink())
                .chatId(message.getChatId()).build());
    }

    public List<String> parser(Message message) {
        int delimiter = message.getText().lastIndexOf(" & ");
        String s = message.getText().substring(delimiter + 3);
        return Arrays.asList(s.split("\\s"));
    }

    public void setPremium(Message message) {
        String userName = util.parseValue(message.getText())[1];
        User user = userRepository.findByUserName(userName);

        if (user != null) {
            userRepository.setPremium(true, userName);
            telegramSender.send(SendMessage.builder()
                    .text("Пользователю с юзернеймом '" + userName + "' успешно присвоен премиум статус")
                    .chatId(message.getChatId()).build());
        } else {
            telegramSender.send(SendMessage.builder()
                    .text("Пользователь с юзернеймом '" + userName + "' не найден")
                    .chatId(message.getChatId()).build());
        }


    }

    public void createFile(Message message) {
        telegramSender.send(SendMessage.builder()
                .text("Анализирую...")
                .chatId(message.getChatId()).build());
        String fileName = "../bot4/user.txt"; // Имя файла
        StringBuilder content = new StringBuilder();
        for (User user : userRepository.findAll()) {
            content.append(user.getUserId()).append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(String.valueOf(content));
            System.out.println("Файл успешно создан и записан.");
        } catch (IOException e) {
            System.err.println("Ошибка при создании или записи файла: " + e.getMessage());
        }

        String filePath = "/root/bot4/user.txt";
        File file = new File(filePath);
        if (file.exists()) {
            telegramSender.sendDocument(new SendDocument(String.valueOf(message.getChatId()), new InputFile(file)));
        } else {
            System.err.println("Файл не существует: " + filePath);
        }
    }

    public void getStatistics(Message message) {
        long startTime = System.nanoTime();
        List<User> users = userRepository.findAll();
        List<User> userList = telegramSender.prepareSendAdsThread(users, telegramSender.send(new SendMessage(String.valueOf(message.getChatId()), "Анализирую...")));
        long endTime = System.nanoTime();
        Duration duration = Duration.ofNanos(endTime - startTime);
        String formattedTime = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.MIDNIGHT.plus(duration));
        telegramSender.send(SendMessage.builder()
                .text("Время выполнения: " + formattedTime + "\nЖивые: " + userList.size() + "\nМертвы: " + (users.size() - userList.size()) + "\nВсего: " + users.size())
                .chatId(message.getChatId()).build());
    }

    public void deleteDead(Message message) {
        List<User> users = userRepository.findAll();
        List<User> userList = telegramSender.prepareDeleteDeadThread(users, telegramSender.send(new SendMessage(String.valueOf(message.getChatId()), "В процессе...")));
        userRepository.deleteAll(userList);
        telegramSender.send(new SendMessage(String.valueOf(message.getChatId()), "\nУдалил: " + userList.size() + " из " + users.size()));
    }

    public void deleteArab(Message message) {
        List<User> users = userRepository.findAll();
        List<User> userList = new ArrayList<>();
        String noArab = "uk ru uz pl az ko cs lv hr sv be kk";
        for (User user : users) {
            if (user.getLanguageCode() == null || !noArab.contains(user.getLanguageCode())) {
                userList.add(user);
            }
        }
        userRepository.deleteAll(userList);
        telegramSender.send(SendMessage.builder()
                .text("\nУдалил: " + userList.size() + " из " + users.size())
                .chatId(message.getChatId()).build());
    }

    public void getToken(Message message) {
        String botToken = message.getText();
        String[] botParts = botToken.split(":");
        referralChannelRepository.setBotToken(botToken, Long.valueOf(botParts[0]));
        telegramSender.send(SendMessage.builder().text("Вроде все ок").chatId(message.getChatId()).build());
        userRepository.setPosition("DEFAULT", message.getChatId());
    }


    public String getParseValue(CallbackQuery callbackQuery) {
        int delimiter = callbackQuery.getData().lastIndexOf(" - ");
        return callbackQuery.getData().substring(delimiter + 3);
    }

    public void addChannel(CallbackQuery callbackQuery) {
        EditMessageText editMessageText = new EditMessageText("Перешли сюда любое сообщение из канала, но не альбом.");
        editMessageText.setChatId(callbackQuery.getMessage().getChatId());
        editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
        telegramSender.sendEditMessage(editMessageText);
        userRepository.setPosition("ADD_CHANNEL", callbackQuery.getMessage().getChatId());
    }

    public void clickCancel(CallbackQuery callbackQuery) {
        referralChannelRepository.setSetAllLinkFalse();
        userRepository.setPosition("DEFAULT", callbackQuery.getMessage().getChatId());
        util.deleteMessageByMessageId(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
    }

    public void cancelSendAds(CallbackQuery callbackQuery) {
        postRepositoryInterface.deleteAll();
        mediaGroupRepository.deleteAll();
        messageEntitiesRepository.deleteAll();
        telegramSender.sendEditMessageText(messageService.createEditMessageWithoutButton("Рассылка отменена", callbackQuery.getMessage().getMessageId(), callbackQuery.getMessage().getChatId()));
    }

    public void deleteAds(CallbackQuery callbackQuery) {
        Long id = Long.valueOf(getParseValue(callbackQuery));
        List<PostToDelete> postToDeleteList = postToDeleteRepository.findAllByPostId(id);
        telegramSender.delete(postToDeleteList);
        postToDeleteRepository.deleteAllByPostId(id);
        telegramSender.sendEditMessageText(messageService.createEditMessageWithoutButton("Посты удалены", callbackQuery.getMessage().getMessageId(), callbackQuery.getMessage().getChatId()));
    }

    public void setChannelLink(CallbackQuery callbackQuery) {
        referralChannelRepository.setSetLink(true, Long.valueOf(getParseValue(callbackQuery)));
        EditMessageText sendMessage = new EditMessageText("В следующем сообщении отправь ссылку на канал.");
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        sendMessage.setMessageId(callbackQuery.getMessage().getMessageId());
        sendMessage.setReplyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Отменить")).callbackData("cancel").build()
                )))));
        telegramSender.sendEditMessage(sendMessage);
        userRepository.setPosition("SET_LINK", callbackQuery.getMessage().getChatId());
    }

    public void clickInfoAndBans(CallbackQuery callbackQuery) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Инфо о юзере")).callbackData("clickGetInfoAboutUser").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Инфо о паке")).callbackData("clickGetInfoAboutStickerSet").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Банлист")).callbackData("clickBlacklist").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Забанить юзера")).callbackData("clickBanUser").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Забанить стикерпак")).callbackData("clickDeleteStickerSetFromCatalog").build(),
                        InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backAdminPanel").build())
        )));

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Инфо и баны")
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId())
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public void clickGetInfoAboutUser(CallbackQuery callbackQuery) {
        userRepository.setPosition("TAKE_USER_OR_ID_FOR_ADMIN", callbackQuery.getFrom().getId());
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Отправь юзернейм или id пользователя, чтобы увидеть информацию о нем")
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public void getLastActivity(Message message) {
        ArrayList<LastActivity> lastActivities = lastActivityRepository.findAllByDateAfter(new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)));
        int week = userRepository.findAllByLastActivityAfter(new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))).size();
        lastActivities.sort(Comparator.comparing(LastActivity::getDate));
        telegramSender.send(SendMessage.builder()
                .text(lastActivities + "\n\nLast Week: " + week)
                .chatId(message.getFrom().getId()).build());
    }

    public void getStatAboutDownloadChapters(Message message) {
        List<Object[]> results = statisticEntityRepository.findUserChapterStatistics();
        int i = 0;
        StringBuilder stringBuilder = new StringBuilder("Стата по загрузкам:\n\n");
        results.sort((a, b) -> ((Number) b[1]).intValue() - ((Number) a[1]).intValue());
        for (Object[] result : results) {
            i++;
            Long userId = ((Number) result[0]).longValue();
            long totalChapters = ((Number) result[1]).longValue();
            User user = userRepository.findByUserId(userId);
            stringBuilder.append(i).append(". ").append(user.getFirstName()).append("\n\n@").append(user.getUserName()).append(" - ").append(totalChapters).append("\n");
            if (i >= 100) {
                break;
            }
        }

        telegramSender.send(SendMessage.builder()
                .text(stringBuilder.toString())
                .chatId(message.getFrom().getId())
                .build());
    }

    public void writeDownloadChaptersStat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        StringBuilder stringBuilder = new StringBuilder("Всего глав загружено (уникальных): ");
        Long allDownloadChapters = mangaChapterRepository.countAllByPdfStatusDownloadOrTelegraphStatusDownload("finished", "finished");
        stringBuilder.append(allDownloadChapters).append("\n\nВсего глав загружено пользователями: ").append(statisticEntityRepository.count());
        stringBuilder.append("\n\nЗа последние сутки: ").append(statisticEntityRepository.findAllByAddedAt(new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)), new Timestamp(System.currentTimeMillis())));
        stringBuilder.append("\n\nТоп 20 пользователей по загрузке:\n");
        List<Object[]> results = statisticEntityRepository.findUserChapterStatistics();
        int i = 0;
        results.sort((a, b) -> ((Number) b[1]).intValue() - ((Number) a[1]).intValue());
        for (Object[] result : results) {
            i++;
            Long userId = ((Number) result[0]).longValue();
            long totalChapters = ((Number) result[1]).longValue();
            User user = userRepository.findByUserId(userId);
            stringBuilder.append(i).append(". ").append(user.getFirstName()).append(" - ").append(totalChapters).append("\n");
            if (i >= 20) {
                break;
            }
        }

        stringBuilder.append("\n\nОбновлено: ").append(dateFormat.format(new Timestamp(System.currentTimeMillis())));

        Post postWithStat = postRepositoryInterface.findFirstByIsCreative(true);
        if (postWithStat == null) {
            Post post = new Post();
            post.setCreative(true);
            post.setMessageId(telegramSender.sendDocument(SendDocument.builder()
                    .document(new InputFile("CgACAgQAAxkBAAEBBjFmFr3Q2PuaPQNW8C_1v2Pqe7yzKwACWgMAApa_NVJatQ1Gi_sfhjQE"))
                    .chatId("-1002051140659")
                    .caption(stringBuilder.toString())
                    .parseMode("HTML").build()).getMessageId());
            postRepositoryInterface.save(post);
            telegramSender.sendPinChatMessage(PinChatMessage.builder()
                    .chatId("-1002051140659")
                    .messageId(post.getMessageId()).build());
        } else {
            try {
                telegramSender.sendEditMessageCaptionInChat(EditMessageCaption.builder()
                        .caption(stringBuilder.toString())
                        .messageId(postWithStat.getMessageId())
                        .chatId("-1002051140659")
                        .parseMode("HTML").build());
            } catch (ExecutionException | InterruptedException | TelegramApiException e) {
                Post post = postRepositoryInterface.findFirstByIsCreative(true);
                post.setMessageId(telegramSender.sendDocument(SendDocument.builder()
                        .document(new InputFile("CgACAgQAAxkBAAEBBjFmFr3Q2PuaPQNW8C_1v2Pqe7yzKwACWgMAApa_NVJatQ1Gi_sfhjQE"))
                        .chatId("-1002051140659")
                        .caption(stringBuilder.toString())
                        .parseMode("HTML").build()).getMessageId());
                postRepositoryInterface.save(post);
                telegramSender.sendPinChatMessage(PinChatMessage.builder()
                        .chatId("-1002051140659")
                        .messageId(post.getMessageId()).build());
            }
        }
    }

    public boolean isAdmin(Long userId) {
        return userId.equals(5672999915L) || userId.equals(6298804214L);
    }


}
