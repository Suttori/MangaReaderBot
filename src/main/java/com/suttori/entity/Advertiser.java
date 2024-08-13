package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "Advertiser")
@Getter
@Setter
public class Advertiser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userid;
    private String userName;

    public Advertiser() {
    }

    public Advertiser(Long userid, String userName) {
        this.userid = userid;
        this.userName = userName;
    }

}
