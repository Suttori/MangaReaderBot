package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "message_entity")
@Getter
@Setter
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


}
