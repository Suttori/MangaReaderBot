package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
public class LastActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int activeUsers;
    private Timestamp date;


    @Override
    public String toString() {
        return "id=" + id +
                "\nactiveUsers=" + activeUsers +
                "\ndate=" + date + "\n\n";
    }
}