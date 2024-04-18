package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Timestamp;


@Entity(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;
    private Long fromChatId;
    private Long channelId;
    private Integer messageId;
    private Integer messageFromChatId;
    private Integer messageThreadId;

    private String text;

    private String caption;
    private String parseMode;
    private String button;
    private Boolean disableNotification;
    private Boolean disableWebPagePreview;
    private boolean allowSendingWithoutReply;
    private boolean protectContent;

    private boolean changeText;
    private boolean addButton;
    private boolean isTextMessage;
    private boolean isMediaGroup;
    private boolean autoCaption;
    private boolean changePostingTime;
    private Timestamp timeCreate;
    private Timestamp publicationTime;
    private Timestamp deletionTime;
    private Long millisecondsForDeleting;
    private Boolean isForwardMessage;
    private Boolean isCreative;
    private String nameCreative;
    private Boolean changeOneLink;
    private Boolean changeManyLinks;
    private Boolean isCopyPost;


    public Post() {
    }

    public Post(Long chatId, Long fromChatId, Integer messageId, String text,
                boolean isTextMessage, Timestamp timeCreate,
                Long channelId, boolean autoCaption, Boolean disableWebPagePreview, Boolean disableNotification) {
        this.chatId = chatId;
        this.fromChatId = fromChatId;
        this.messageId = messageId;
        this.text = text;
        this.isTextMessage = isTextMessage;
        this.timeCreate = timeCreate;
        this.channelId = channelId;
        this.autoCaption = autoCaption;
        this.disableWebPagePreview = disableWebPagePreview;
        this.disableNotification = disableNotification;
    }

    public Post(Long chatId, Long fromChatId, Integer messageId, boolean isTextMessage, String caption, Timestamp timeCreate,
                Long channelId, boolean autoCaption, Boolean disableWebPagePreview, Boolean disableNotification) {
        this.chatId = chatId;
        this.fromChatId = fromChatId;
        this.messageId = messageId;
        this.isTextMessage = isTextMessage;
        this.caption = caption;
        this.timeCreate = timeCreate;
        this.channelId = channelId;
        this.autoCaption = autoCaption;
        this.disableWebPagePreview = disableWebPagePreview;
        this.disableNotification = disableNotification;
    }

    public Post(Long chatId, Long fromChatId, Integer messageId, boolean isTextMessage, String caption, boolean isMediaGroup, Timestamp timeCreate,
                Long channelId, boolean autoCaption, Boolean disableWebPagePreview, Boolean disableNotification) {
        this.chatId = chatId;
        this.fromChatId = fromChatId;
        this.messageId = messageId;
        this.isTextMessage = isTextMessage;
        this.caption = caption;
        this.timeCreate = timeCreate;
        this.isMediaGroup = isMediaGroup;
        this.channelId = channelId;
        this.autoCaption = autoCaption;
        this.disableWebPagePreview = disableWebPagePreview;
        this.disableNotification = disableNotification;
    }

    public Post(Long chatId, Long fromChatId, Integer messageId, String text, boolean isTextMessage, Timestamp timeCreate,
                Boolean disableWebPagePreview, Boolean isCreative, Integer messageFromChatId, boolean isMediaGroup) {
        this.chatId = chatId;
        this.fromChatId = fromChatId;
        this.messageId = messageId;
        this.text = text;
        this.isTextMessage = isTextMessage;
        this.timeCreate = timeCreate;
        this.disableWebPagePreview = disableWebPagePreview;
        this.isCreative = isCreative;
        this.messageFromChatId = messageFromChatId;
    }

    public Post(Long chatId, Long fromChatId, Integer messageId, boolean isTextMessage, String caption, Timestamp timeCreate,
                Boolean disableWebPagePreview, Boolean isCreative, Integer messageFromChatId, boolean isMediaGroup) {
        this.chatId = chatId;
        this.fromChatId = fromChatId;
        this.messageId = messageId;
        this.caption = caption;
        this.isTextMessage = isTextMessage;
        this.timeCreate = timeCreate;
        this.disableWebPagePreview = disableWebPagePreview;
        this.isCreative = isCreative;
        this.messageFromChatId = messageFromChatId;
        this.isMediaGroup = isMediaGroup;
    }

    public Post(Post post) {
        this.chatId = post.getChatId();
        this.fromChatId = post.getFromChatId();
        this.channelId = post.getChannelId();
        this.messageId = post.getMessageId();
        this.messageFromChatId = post.messageFromChatId;
        this.messageThreadId = post.getMessageThreadId();
        this.text = post.getText();
        this.caption = post.getCaption();
        this.parseMode = post.getParseMode();
        this.button = post.getButton();
        this.disableNotification = post.getDisableNotification();
        this.disableWebPagePreview = post.getDisableWebPagePreview();
        this.allowSendingWithoutReply = post.isAllowSendingWithoutReply();
        this.protectContent = post.isProtectContent();
        this.changeText = post.isChangeText();
        this.addButton = post.isAddButton();
        this.isTextMessage = post.isTextMessage();
        this.isMediaGroup = post.isMediaGroup();
        this.autoCaption = post.isAutoCaption();
        this.changePostingTime = post.isChangePostingTime();
        this.timeCreate = post.getTimeCreate();
        this.publicationTime = post.getPublicationTime();
        this.deletionTime = post.getDeletionTime();
        this.millisecondsForDeleting = post.getMillisecondsForDeleting();
        this.isForwardMessage = post.getForwardMessage();
        this.isCreative = post.getCreative();
        this.nameCreative = post.getNameCreative();
        this.changeOneLink = post.getChangeOneLink();
        this.changeManyLinks = post.getChangeManyLinks();
        this.isCopyPost = post.getCopyPost();
    }

    public Boolean getCopyPost() {
        return isCopyPost;
    }

    public void setCopyPost(Boolean copyPost) {
        isCopyPost = copyPost;
    }

    public Boolean getChangeOneLink() {
        return changeOneLink;
    }

    public void setChangeOneLink(Boolean changeOneLink) {
        this.changeOneLink = changeOneLink;
    }

    public Boolean getChangeManyLinks() {
        return changeManyLinks;
    }

    public void setChangeManyLinks(Boolean changeManyLinks) {
        this.changeManyLinks = changeManyLinks;
    }

    public String getNameCreative() {
        return nameCreative;
    }

    public void setNameCreative(String nameCreative) {
        this.nameCreative = nameCreative;
    }

    public Boolean getCreative() {
        return isCreative;
    }

    public void setCreative(Boolean creative) {
        isCreative = creative;
    }

    public Boolean getForwardMessage() {
        return isForwardMessage;
    }

    public void setForwardMessage(Boolean forwardMessage) {
        isForwardMessage = forwardMessage;
    }


    public Integer getMessageFromChatId() {
        return messageFromChatId;
    }

    public void setMessageFromChatId(Integer messageFromChatId) {
        this.messageFromChatId = messageFromChatId;
    }

    public boolean isChangePostingTime() {
        return changePostingTime;
    }

    public void setChangePostingTime(boolean changePostingTime) {
        this.changePostingTime = changePostingTime;
    }

    public Long getMillisecondsForDeleting() {
        return millisecondsForDeleting;
    }

    public void setMillisecondsForDeleting(Long millisecondsForDeleting) {
        this.millisecondsForDeleting = millisecondsForDeleting;
    }

    public Timestamp getDeletionTime() {
        return deletionTime;
    }

    public void setDeletionTime(Timestamp deletionTime) {
        this.deletionTime = deletionTime;
    }

    public boolean isAutoCaption() {
        return autoCaption;
    }

    public void setAutoCaption(boolean autoCaption) {
        this.autoCaption = autoCaption;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getFromChatId() {
        return fromChatId;
    }

    public void setFromChatId(Long fromChatId) {
        this.fromChatId = fromChatId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Integer getMessageThreadId() {
        return messageThreadId;
    }

    public void setMessageThreadId(Integer messageThreadId) {
        this.messageThreadId = messageThreadId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getParseMode() {
        return parseMode;
    }

    public void setParseMode(String parseMode) {
        this.parseMode = parseMode;
    }

    public String getButton() {
        return button;
    }

    public void setButton(String button) {
        this.button = button;
    }

    public Boolean getDisableNotification() {
        return disableNotification;
    }

    public void setDisableNotification(Boolean disableNotification) {
        this.disableNotification = disableNotification;
    }

    public Boolean getDisableWebPagePreview() {
        return disableWebPagePreview;
    }

    public void setDisableWebPagePreview(Boolean disableWebPagePreview) {
        this.disableWebPagePreview = disableWebPagePreview;
    }

    public boolean isAllowSendingWithoutReply() {
        return allowSendingWithoutReply;
    }

    public void setAllowSendingWithoutReply(boolean allowSendingWithoutReply) {
        this.allowSendingWithoutReply = allowSendingWithoutReply;
    }

    public boolean isProtectContent() {
        return protectContent;
    }

    public void setProtectContent(boolean protectContent) {
        this.protectContent = protectContent;
    }

    public boolean isChangeText() {
        return changeText;
    }

    public void setChangeText(boolean changeText) {
        this.changeText = changeText;
    }

    public boolean isAddButton() {
        return addButton;
    }

    public void setAddButton(boolean addButton) {
        this.addButton = addButton;
    }

    public boolean isTextMessage() {
        return isTextMessage;
    }

    public void setTextMessage(boolean textMessage) {
        isTextMessage = textMessage;
    }

    public boolean isMediaGroup() {
        return isMediaGroup;
    }

    public void setMediaGroup(boolean mediaGroup) {
        isMediaGroup = mediaGroup;
    }

    public Timestamp getTimeCreate() {
        return timeCreate;
    }

    public void setTimeCreate(Timestamp timeCreate) {
        this.timeCreate = timeCreate;
    }

    public Timestamp getPublicationTime() {
        return publicationTime;
    }

    public void setPublicationTime(Timestamp publicationTime) {
        this.publicationTime = publicationTime;
    }
}
