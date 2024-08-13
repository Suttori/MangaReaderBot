package com.suttori.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Set;

@Entity(name = "\"user\"")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private Boolean isPremiumBotUser;
    private Timestamp subscriptionEndDate;
    private String position;
    private Boolean isTelegramPremium;
    private String languageCode;
    private Timestamp registerTime;
    private String referral;
    private Boolean accessStatus;
    private Boolean isAlive;
    private Boolean isAccessStatus;
    private int balance;
    private Timestamp lastActivity;
    private String currentMangaCatalog;
    private String sortParam;
    private String languageCodeForCatalog;
    private String mangaFormatParameter;
    private String temporaryMessageId;
    private String numberOfChaptersSent;


    public User() {
    }

    public User(Long userId, Long chatId, String firstName, String lastName, String userName, Boolean isPremiumBotUser, Boolean isTelegramPremium, String languageCode,
                Timestamp registerTime, String referral, Boolean accessStatus, String position, int balance, String currentMangaCatalog) {
        this.userId = userId;
        this.chatId = chatId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.isPremiumBotUser = isPremiumBotUser;
        this.isTelegramPremium = isTelegramPremium;
        this.languageCode = languageCode;
        this.registerTime = registerTime;
        this.referral = referral;
        this.accessStatus = accessStatus;
        this.position = position;
        this.balance = balance;
        this.currentMangaCatalog = currentMangaCatalog;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId=" + userId +
                ", chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", premiumBotUser='" + isPremiumBotUser + '\'' +
                ", subscriptionEndDate=" + subscriptionEndDate +
                ", position='" + position + '\'' +
                ", isTelegramPremium=" + isTelegramPremium +
                ", languageCode='" + languageCode + '\'' +
                ", registerTime=" + registerTime +
                ", referral='" + referral + '\'' +
                ", accessStatus=" + accessStatus +
                ", isAlive=" + isAlive +
                ", balance=" + balance +
                '}';
    }
}
