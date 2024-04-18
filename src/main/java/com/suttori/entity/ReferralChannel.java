package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@Entity(name = "channel")
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

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public Timestamp getAddChannel() {
        return addChannel;
    }

    public void setAddChannel(Timestamp addChannel) {
        this.addChannel = addChannel;
    }

    public Timestamp getAddInReferral() {
        return addInReferral;
    }

    public void setAddInReferral(Timestamp addInReferral) {
        this.addInReferral = addInReferral;
    }

    public Boolean getSetLink() {
        return setLink;
    }

    public void setSetLink(Boolean setLink) {
        this.setLink = setLink;
    }

    public Boolean getEnableChannel() {
        return enableChannel;
    }

    public void setEnableChannel(Boolean enableChannel) {
        this.enableChannel = enableChannel;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelUsername() {
        return channelUsername;
    }

    public void setChannelUsername(String channelUsername) {
        this.channelUsername = channelUsername;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
