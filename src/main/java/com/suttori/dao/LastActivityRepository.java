package com.suttori.dao;

import com.suttori.entity.LastActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;

@Repository
public interface LastActivityRepository extends JpaRepository<LastActivity, Long> {

    ArrayList<LastActivity> findAllByDateAfter(Timestamp date);

}
