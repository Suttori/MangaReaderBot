package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity(name = "media_group")
public class MediaGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;
    private Long chatId;
    private String fieldId;
    private boolean isPhoto;
    private boolean isVideo;
    private boolean isAudio;
    private boolean isDocument;


    public MediaGroup(Long postId, Long chatId, String fieldId) {
        this.postId = postId;
        this.chatId = chatId;
        this.fieldId = fieldId;
    }

    public MediaGroup() {

    }

    public MediaGroup(MediaGroup mediaGroup) {
        this.postId = mediaGroup.getPostId();
        this.chatId = mediaGroup.getChatId();
        this.fieldId = mediaGroup.getFieldId();
        this.isPhoto = mediaGroup.isPhoto();
        this.isVideo = mediaGroup.isVideo();
        this.isAudio = mediaGroup.isAudio();
        this.isDocument = mediaGroup.isDocument();
    }

    public boolean isPhoto() {
        return isPhoto;
    }

    public void setPhoto(boolean photo) {
        isPhoto = photo;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public boolean isAudio() {
        return isAudio;
    }

    public void setAudio(boolean audio) {
        isAudio = audio;
    }

    public boolean isDocument() {
        return isDocument;
    }

    public void setDocument(boolean document) {
        isDocument = document;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }
}
