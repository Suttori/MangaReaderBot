package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "user_blacklist")
@Getter
@Setter
public class BannedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String userName;

    public BannedUser() {
    }

    public BannedUser(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }


    @Override
    public String toString() {
        return "id: " + id + "\n" +
                "userId: <code>" + userId + "</code>\n" +
                "userName: @" + userName + "\n\n";
    }
}
