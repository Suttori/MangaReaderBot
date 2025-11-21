package com.suttori.dao;

import com.suttori.dto.UserFileSizeDto;
import com.suttori.entity.AwsUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwsUrlRepository extends JpaRepository<AwsUrl, Long> {

    List<AwsUrl> findAllByChapterDataBaseId(Long chapterDataBaseId);

    @Query(value = """
        SELECT user_id, ROUND(SUM(file_size) / 1048576.0, 1) AS totalSizeMb
        FROM aws_url
        WHERE user_id IS NOT NULL
        GROUP BY user_id
        ORDER BY totalSizeMb DESC
        LIMIT 50
    """, nativeQuery = true)
    List<Object[]> findTopUsersByFileSizeRaw();

    @Query(value = """
    SELECT user_id, ROUND(SUM(file_size) / 1048576.0, 1) AS totalSizeMb
    FROM aws_url
    WHERE user_id IS NOT NULL
      AND added_at >= NOW() - INTERVAL '7 days'
    GROUP BY user_id
    ORDER BY totalSizeMb DESC
    LIMIT 100
""", nativeQuery = true)
    List<Object[]> findTopUsersByFileSizeForLastWeek();

}
