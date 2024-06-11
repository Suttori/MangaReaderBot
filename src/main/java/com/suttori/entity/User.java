package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

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
    private String premiumBotUser;
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


    public User() {
    }

    public User(Long userId, Long chatId, String firstName, String lastName, String userName, String premiumBotUser, Boolean isTelegramPremium, String languageCode,
                Timestamp registerTime, String referral, Boolean accessStatus, String position, int balance) {
        this.userId = userId;
        this.chatId = chatId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.premiumBotUser = premiumBotUser;
        this.isTelegramPremium = isTelegramPremium;
        this.languageCode = languageCode;
        this.registerTime = registerTime;
        this.referral = referral;
        this.accessStatus = accessStatus;
        this.position = position;
        this.balance = balance;
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
                ", premiumBotUser='" + premiumBotUser + '\'' +
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
