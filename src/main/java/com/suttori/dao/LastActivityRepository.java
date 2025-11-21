package com.suttori.dao;

import com.suttori.entity.LastActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Optional;

@Repository
public interface LastActivityRepository extends JpaRepository<LastActivity, Long> {

    ArrayList<LastActivity> findAllByDateAfter(Timestamp date);

    @Query(value = """
    SELECT * 
    FROM last_activity 
    WHERE CAST(date AS DATE) = CAST(:date AS DATE)
    LIMIT 1
""", nativeQuery = true)
    Optional<LastActivity> findBySpecificDate(@Param("date") Timestamp date);


}
