package com.suttori.telegram;

import com.suttori.config.BotConfig;
import com.suttori.entity.PostToDelete;
import com.suttori.entity.User;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.*;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.MessageId;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class TelegramSender {

    @Value("${bot.token}")
    private String token;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Logger logger = LoggerFactory.getLogger(TelegramSender.class);
    private TelegramClient telegramClient;
    private TelegramApiFeignClient telegramApiFeignClient;

    @Autowired
    public TelegramSender(TelegramClient telegramClient, TelegramApiFeignClient telegramApiFeignClient) {
        this.telegramClient = telegramClient;
        this.telegramApiFeignClient = telegramApiFeignClient;
    }

    public Message send(SendMessage sendMessage) {
        logger.info("send " + sendMessage.getChatId());
        try {
            return telegramClient.executeAsync(sendMessage).get();
        } catch (InterruptedException | ExecutionException | TelegramApiException e) {
            logger.error("Failed send " + sendMessage.getChatId(), e);
            throw new RuntimeException(e);
        }
    }

    public void sendEditMessageText(EditMessageText editMessageText) {
        logger.info("sendEditMessageText " + editMessageText.getChatId());
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            logger.error("Failed sendEditMessageText " + editMessageText.getChatId(), e);
        }
    }

    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        logger.info("answerCallbackQuery " + answerCallbackQuery.getCallbackQueryId());
        executorService.submit(() -> {
            try {
                telegramClient.executeAsync(answerCallbackQuery);
            } catch (TelegramApiException e) {
                logger.error("Failed sendEditMessageText " + answerCallbackQuery.getCallbackQueryId(), e);
            }
        });
    }

    public void sendPhoto(SendPhoto sendPhoto) {
        logger.info("sendPhoto " + sendPhoto.getChatId());
        executorService.submit(() -> {
            telegramClient.executeAsync(sendPhoto);
        });
    }

    public Message sendEditMessageMedia(EditMessageMedia editMessageMedia) {
        logger.info("editMessageMedia " + editMessageMedia.getChatId());
        try {
            return (Message) telegramClient.executeAsync(editMessageMedia).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("sendForwardMessage " + editMessageMedia.getChatId(), e);
            throw new RuntimeException(e);
        }
    }

    public void sendEditMessageCaption(EditMessageCaption editMessageCaption) {
        logger.info("sendEditMessageCaption " + editMessageCaption.getChatId());
        try {
            telegramClient.executeAsync(editMessageCaption);
        } catch (TelegramApiException e) {
            logger.error("Failed sendEditMessageCaption " + editMessageCaption.getChatId(), e);
        }
    }

    public void sendEditMessageCaptionInChat(EditMessageCaption editMessageCaption) throws TelegramApiException, ExecutionException, InterruptedException {
        logger.info("sendEditMessageCaptionInChat " + editMessageCaption.getChatId());
        telegramClient.executeAsync(editMessageCaption);
    }

    public void sendPinChatMessage(PinChatMessage pinChatMessage) {
        logger.info("sendEditMessageCaptionInChat");
        try {
            telegramClient.executeAsync(pinChatMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendEditMessageReplyMarkup(EditMessageReplyMarkup editMessageReplyMarkup) {
        logger.info("sendEditMessageReplyMarkup " + editMessageReplyMarkup.getChatId());
        try {
            telegramClient.executeAsync(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            logger.error("sendEditMessageReplyMarkup " + editMessageReplyMarkup.getChatId(), e);
        }

    }

    public void sendForwardMessage(ForwardMessage forwardMessage) {
        logger.info("SendForwardMessage " + forwardMessage.getChatId());
        try {
            telegramClient.executeAsync(forwardMessage);
        } catch (TelegramApiException e) {
            logger.error("sendForwardMessage " + forwardMessage.getChatId(), e);
        }
    }

    public MessageId sendCopyMessage(CopyMessage copyMessage) {
        logger.info("sendCopyMessage " + copyMessage.getChatId());
        try {
            return telegramClient.executeAsync(copyMessage).get();
        } catch (ExecutionException | InterruptedException | TelegramApiException e) {
            logger.error("Failed sendCopyMessage " + copyMessage.getChatId(), e);
            throw new RuntimeException(e);
        }
    }

    public void resendCopyMessageFromStorage(CopyMessage copyMessage) throws ExecutionException, InterruptedException {
        logger.info("sendCopyMessageFromStorage " + copyMessage.getChatId());
        try {
            telegramClient.executeAsync(copyMessage);
            checkChannelSubscribe(Long.valueOf(copyMessage.getChatId()));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendCopyMessageFromStorage(CopyMessage copyMessage) {
        logger.info("sendCopyMessageFromStorage " + copyMessage.getChatId());
        try {
            telegramClient.executeAsync(copyMessage);
            checkChannelSubscribe(Long.valueOf(copyMessage.getChatId()));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMediaGroup(SendMediaGroup sendMediaGroup) {
        logger.info("sendMediaGroup " + sendMediaGroup.getChatId());
        telegramClient.executeAsync(sendMediaGroup);
    }

    public void sendEditMessage(EditMessageText editMessageText) {
        logger.info("sendEditMessage " + editMessageText.getChatId());
        try {
            telegramClient.executeAsync(editMessageText);
        } catch (TelegramApiException e) {
            logger.error("sendEditMessage " + editMessageText.getChatId(), e);
        }
    }

    public UserProfilePhotos getUserProfilePhotos(GetUserProfilePhotos getUserProfilePhotos) {
        logger.info("getUserProfilePhotos " + getUserProfilePhotos.getUserId());
        try {
            return telegramClient.executeAsync(getUserProfilePhotos).get();
        } catch (ExecutionException | InterruptedException | TelegramApiException e) {
            logger.error("Failed getUserProfilePhotos " + getUserProfilePhotos.getUserId());
            throw new RuntimeException(e);
        }
    }

    public Message sendDocument(SendDocument sendDocument) {
        logger.info("sendDocument ");
        try {
            return telegramClient.executeAsync(sendDocument).get();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed sendDocument ");
            throw new RuntimeException(e);
        }
    }

    public File sendGetFile(GetFile getFile) {
        logger.info("getFile " + getFile.getFileId());
        try {
            return telegramClient.executeAsync(getFile).get();
        } catch (ExecutionException | InterruptedException | TelegramApiException e) {
            logger.error("Failed sendCopyMessage " + getFile.getFileId(), e);
            throw new RuntimeException(e);
        }
    }

    public void deleteMessageById(String chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        try {
            telegramClient.executeAsync(deleteMessage);
        } catch (TelegramApiException tae) {
            throw new RuntimeException(tae);
        }
    }

    public boolean isMember(Long userId, Long channelId) {
        try {
            ChatMember chatMember = telegramClient.executeAsync(new GetChatMember(String.valueOf(channelId), userId)).get();
            if (!chatMember.getStatus().equals("left")) {
                return true;
            }
        } catch (ExecutionException | InterruptedException | TelegramApiException e) {
            logger.error("Пользователь не подписан на канал", e);
            throw new RuntimeException(e);
        }
        return false;
    }

    public boolean isBotUser(String botToken, Message message) {
        Response response = telegramApiFeignClient.sendChatAction(botToken, message.getChatId(), "typing");
        return response.reason().equals("OK");
    }

    public void sendChatAction(Long userId, String action) {
        logger.info("action " + userId);
        executorService.submit(() -> {
            telegramApiFeignClient.sendChatAction(token, userId, action);
        });

    }

    public List<User> prepareSendAdsThread(List<User> users, Message message) {
        AtomicInteger counter = new AtomicInteger(0);
        EditMessageText editMessageText = new EditMessageText("");
        editMessageText.setChatId(message.getChatId());
        editMessageText.setMessageId(message.getMessageId());
        final double[] percent = {0.01};
        int totalCount = users.size();
        int fivePercent = (int) Math.round(totalCount * percent[0]);

        logger.info(String.valueOf(ForkJoinPool.commonPool()));

        return users.parallelStream()
                .peek(user -> {
                    int currentCounter = counter.incrementAndGet();
                    if (fivePercent != 0 && currentCounter % fivePercent == 0) {
                        editMessageText.setText("Пройдено " + Math.round(percent[0] * 100) + "%");
                        sendEditMessageText(editMessageText);
                        percent[0] += 0.01;
                    }
                })
                .filter(user -> telegramApiFeignClient.sendChatAction(token, user.getChatId(), "typing").reason().equals("OK"))
                .collect(Collectors.toList());
    }

    public List<User> prepareDeleteDeadThread(List<User> users, Message message) {
        AtomicInteger counter = new AtomicInteger(0);
        EditMessageText editMessageText = new EditMessageText("");
        editMessageText.setChatId(message.getChatId());
        editMessageText.setMessageId(message.getMessageId());
        final double[] percent = {0.01};
        int totalCount = users.size();
        int fivePercent = (int) Math.round(totalCount * percent[0]);

        logger.info(String.valueOf(ForkJoinPool.commonPool()));

        return users.parallelStream()
                .peek(user -> {
                    int currentCounter = counter.incrementAndGet();
                    if (fivePercent != 0 && currentCounter % fivePercent == 0) {
                        editMessageText.setText("Пройдено " + Math.round(percent[0] * 100) + "%");
                        sendEditMessageText(editMessageText);
                        percent[0] += 0.01;
                    }
                })
                .filter(user -> !Objects.equals(telegramApiFeignClient.sendChatAction(token, user.getChatId(), "typing").reason(), "OK"))
                .collect(Collectors.toList());
    }

    public List<User> getStatLink(List<User> users) {
        return users.parallelStream()
                .filter(user -> telegramApiFeignClient.sendChatAction(token, user.getChatId(), "typing").reason().equals("OK"))
                .collect(Collectors.toList());
    }

    public void delete(List<PostToDelete> postToDeleteList) {
        for (PostToDelete post : postToDeleteList) {
            telegramApiFeignClient.delete(token, post.getChannelId(), post.getMessageId());
        }
    }

    public String getInfoLogger(Message message) {
        return " Користувач: " + message.getFrom().getUserName() + " " + message.getFrom().getFirstName();
    }

    public boolean isBotAdmin(Message message) {
        logger.info("isBotAdmin" + getInfoLogger(message));
        try {
            List<ChatMember> chatMembers = telegramClient.execute(new GetChatAdministrators(String.valueOf(message.getForwardFromChat().getId())));
            org.telegram.telegrambots.meta.api.objects.User user = telegramClient.execute(new GetMe());
            for (ChatMember member : chatMembers) {
                if (member.getUser().getId().equals(user.getId())) {
                    return true;
                }
            }
        } catch (TelegramApiException e) {
            logger.error("Бот не является администратором канала");
        }
        return false;
    }

    public void sendAnswerInlineQuery(AnswerInlineQuery answerInlineQuery) {
        try {
            telegramClient.executeAsync(answerInlineQuery);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkChannelSubscribe(Long userId) {
        if (!isMember(userId, -1002051140659L)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<MessageEntity> entities = new ArrayList<>();
            entities.add(MessageEntity.builder()
                    .type("bold")
                    .length(12)
                    .offset(148).build());
            entities.add(MessageEntity.builder()
                    .type("text_link")
                    .url("https://t.me/+KJCfnXsvY_02NjNi")
                    .length(12)
                    .offset(148).build());
            send(SendMessage.builder()
                    .text("Привет, бот всегда был для всех бесплатным и без каких-либо ограничений, и продолжает оставаться таковым.\n\nВзамен я прошу подписаться тебя на канал Manga Reader, где я пишу о развитии бота, обновлених и просто пощу всякое из манги и околояпонского контента. Подписывайся!\n\nТвоя глава находится выше, а это собщение перестанет появляться после подписки на канал ❤\uFE0F")
                    .entities(entities)
                    .disableWebPagePreview(true)
                    .chatId(userId).build());
        }
    }
}
