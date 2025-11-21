package com.suttori.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "ActivationToken")
@Getter
@Setter
public class ActivationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String token;
    private Long invoiceId;
    private Timestamp generationDate;
    private Timestamp expirationDate;
    private String status;

    public ActivationToken() {
    }


    public ActivationToken(Long userId, String token, Timestamp generationDate, Timestamp expirationDate, String status) {
        this.userId = userId;
        this.token = token;
        this.generationDate = generationDate;
        this.expirationDate = expirationDate;
        this.status = status;
    }


}
