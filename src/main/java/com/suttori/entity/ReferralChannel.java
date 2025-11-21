package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "channel")
@Getter
@Setter
public class ReferralChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Long channelId;
    private String channelName;
    private String channelUsername;
    private Long userId;
    private String username;
    private String link;
    private Boolean enableChannel;
    private Boolean setLink;
    private Timestamp addChannel;
    private Timestamp addInReferral;
    private boolean isBot;
    private String botToken;

    public ReferralChannel(Long channelId, String channelName, String channelUsername, Long userId, String username, String link, Boolean enableChannel, Timestamp addChannel, boolean isBot) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelUsername = channelUsername;
        this.userId = userId;
        this.username = username;
        this.link = link;
        this.enableChannel = enableChannel;
        this.addChannel = addChannel;
        this.isBot = isBot;
    }

    public ReferralChannel() {

    }


}
