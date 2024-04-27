package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@Entity(name = "\"user\"")
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
    private String currentSetName;
    private String currentStickerSetAddress;
    private int currentPageInStickerSetStorage;
    private int balance;
    private Long temporaryMessageId;
    private String temporarySourceStickerSetName;
    private String temporaryFileUniqueId;
    private Timestamp lastActivity;
    private String currentMangaCatalog;


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

    public String getCurrentMangaCatalog() {
        return currentMangaCatalog;
    }
    public void setCurrentMangaCatalog(String currentMangaCatalog) {
        this.currentMangaCatalog = currentMangaCatalog;
    }

    public Timestamp getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Timestamp lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getTemporaryFileUniqueId() {
        return temporaryFileUniqueId;
    }

    public void setTemporaryFileUniqueId(String temporaryFileUniqueId) {
        this.temporaryFileUniqueId = temporaryFileUniqueId;
    }

    public String getTemporarySourceStickerSetName() {
        return temporarySourceStickerSetName;
    }

    public void setTemporarySourceStickerSetName(String temporarySourceStickerSetName) {
        this.temporarySourceStickerSetName = temporarySourceStickerSetName;
    }

    public int getCurrentPageInStickerSetStorage() {
        return currentPageInStickerSetStorage;
    }

    public void setCurrentPageInStickerSetStorage(int currentPageInStickerSetStorage) {
        this.currentPageInStickerSetStorage = currentPageInStickerSetStorage;
    }

    public Long getTemporaryMessageId() {
        return temporaryMessageId;
    }

    public void setTemporaryMessageId(Long temporaryMessageId) {
        this.temporaryMessageId = temporaryMessageId;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrentSetName() {
        return currentSetName;
    }

    public void setCurrentSetName(String currentSetName) {
        this.currentSetName = currentSetName;
    }

    public String getCurrentStickerSetAddress() {
        return currentStickerSetAddress;
    }

    public void setCurrentStickerSetAddress(String currentStickerSetAddress) {
        this.currentStickerSetAddress = currentStickerSetAddress;
    }

    public Boolean getTelegramPremium() {
        return isTelegramPremium;
    }

    public Boolean getAccessStatus() {
        return accessStatus;
    }

    public Boolean getAlive() {
        return isAlive;
    }

    public void setAlive(Boolean alive) {
        isAlive = alive;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReferral() {
        return referral;
    }

    public void setReferral(String referral) {
        this.referral = referral;
    }

    public Boolean isAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(Boolean accessStatus) {
        this.accessStatus = accessStatus;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPremiumBotUser() {
        return premiumBotUser;
    }

    public void setPremiumBotUser(String premiumBotUser) {
        this.premiumBotUser = premiumBotUser;
    }

    public Timestamp getSubscriptionEndDate() {
        return subscriptionEndDate;
    }

    public void setSubscriptionEndDate(Timestamp subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Boolean isTelegramPremium() {
        return isTelegramPremium;
    }

    public void setTelegramPremium(Boolean telegramPremium) {
        isTelegramPremium = telegramPremium;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Timestamp getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Timestamp registerTime) {
        this.registerTime = registerTime;
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
                ", currentSetName='" + currentSetName + '\'' +
                ", currentStickerSetAddress='" + currentStickerSetAddress + '\'' +
                ", currentPageInStickerSetStorage=" + currentPageInStickerSetStorage +
                ", balance=" + balance +
                ", temporaryMessageId=" + temporaryMessageId +
                ", temporarySourceStickerSetName='" + temporarySourceStickerSetName + '\'' +
                '}';
    }

    public String getMinInfoAboutUser() {
        return "id: " + id + "\n" +
                "userId: " + userId + "\n" +
                "firstName: " + firstName + "\n" +
                "lastName: " + lastName + "\n" +
                "userName: @" + userName + "\n" +
                "languageCode: " + languageCode + "\n" +
                "registerTime: " + registerTime + "\n" +
                "balance: " + balance;
    }
}
