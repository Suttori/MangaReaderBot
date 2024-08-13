package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "media_group")
@Getter
@Setter
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


}
