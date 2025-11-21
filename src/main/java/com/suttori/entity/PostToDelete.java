package com.suttori.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "post_to_delete")
@Getter
@Setter
public class PostToDelete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Long chatId;
    private Long postId;
    private Integer messageId;
    private Long channelId;
    private Timestamp deletionTime;

    public PostToDelete() {

    }

    public PostToDelete(Long chatId, Long postId, Integer messageId, Long channelId) {
        this.chatId = chatId;
        this.postId = postId;
        this.messageId = messageId;
        this.channelId = channelId;
    }

}
