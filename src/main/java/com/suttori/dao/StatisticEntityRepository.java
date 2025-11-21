package com.suttori.dao;

import com.suttori.dto.UserChapterStatisticsDTO;
import com.suttori.entity.StatisticEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public interface StatisticEntityRepository extends JpaRepository<StatisticEntity, Long> {

    ArrayList<StatisticEntity> findAllByUserId(Long userId);

    @Query(value = "SELECT s.user_id, COUNT(s.id) FROM statistic_entity s GROUP BY s.user_id", nativeQuery = true)
    List<Object[]> findUserChapterStatistics();

    @Query(value = "SELECT new com.suttori.dto.UserChapterStatisticsDTO(s.userId, COUNT(s.id)) FROM StatisticEntity s GROUP BY s.userId")
    List<UserChapterStatisticsDTO> findUserChapterStatisticsDto();

    @Query(value = """
    SELECT new com.suttori.dto.UserChapterStatisticsDTO(s.userId, COUNT(s.id))
    FROM StatisticEntity s
    WHERE s.addedAt >= :startOfDay
    GROUP BY s.userId""")
    List<UserChapterStatisticsDTO> findUserChapterStatisticsToday(@Param("startOfDay") Timestamp startOfDay);

    @Query(value = "SELECT COUNT(*) from statistic_entity WHERE added_at BETWEEN :added_at1 and :added_at2", nativeQuery = true)
    Long findAllByAddedAt(@Param("added_at1") Timestamp added_at1, @Param("added_at2") Timestamp added_at2);

}
