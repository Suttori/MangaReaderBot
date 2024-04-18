package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity(name = "message_entity")
public class MessageFormatting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;
    private Long replyId;
    private String type;
    private Integer start;
    private Integer length;
    private String text;
    private String url;
    private String language;
    private String customEmojiId;
    private Long channelId;
    private boolean isAutoCaption;

    public MessageFormatting() {
    }

    public MessageFormatting(Long postId, String type, Integer start, Integer length, String text, String url, String language, String customEmojiId, boolean isAutoCaption) {
        this.postId = postId;
        this.type = type;
        this.start = start;
        this.length = length;
        this.text = text;
        this.url = url;
        this.language = language;
        this.customEmojiId = customEmojiId;
        this.isAutoCaption = isAutoCaption;
    }

    public MessageFormatting(String type, Integer start, Integer length, String text, String url, String language, String customEmojiId, Long channelId, boolean isAutoCaption) {
        this.channelId = channelId;
        this.type = type;
        this.start = start;
        this.length = length;
        this.text = text;
        this.url = url;
        this.language = language;
        this.customEmojiId = customEmojiId;
        this.isAutoCaption = isAutoCaption;
    }

    public MessageFormatting(String type, Integer start, Integer length, String text, String url, String language, String customEmojiId, boolean isAutoCaption, Long replyId) {
        this.replyId = replyId;
        this.type = type;
        this.start = start;
        this.length = length;
        this.text = text;
        this.url = url;
        this.language = language;
        this.customEmojiId = customEmojiId;
        this.isAutoCaption = isAutoCaption;
    }


    public Long getReplyId() {
        return replyId;
    }

    public void setReplyId(Long replyId) {
        this.replyId = replyId;
    }

    public boolean isAutoCaption() {
        return isAutoCaption;
    }

    public void setAutoCaption(boolean autoCaption) {
        isAutoCaption = autoCaption;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCustomEmojiId() {
        return customEmojiId;
    }

    public void setCustomEmojiId(String customEmojiId) {
        this.customEmojiId = customEmojiId;
    }
}
