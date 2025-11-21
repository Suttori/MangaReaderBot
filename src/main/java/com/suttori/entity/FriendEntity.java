package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "FriendEntity")
@Getter
@Setter
public class FriendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long friendId;
    private Timestamp addedAt;

    public FriendEntity() {
    }

    public FriendEntity(Long userId, Long friendId, Timestamp addedAt) {
        this.userId = userId;
        this.friendId = friendId;
        this.addedAt = addedAt;
    }


}
