package com.suttori.dao;

import com.suttori.entity.MangaDesu.NotificationChapterMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationChapterMappingRepository extends JpaRepository<NotificationChapterMapping, Long> {

    NotificationChapterMapping findByMangaId(Long mangaId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE notification_chapter_mapping SET chapter = ? WHERE manga_id = ?", nativeQuery = true)
    void setChapter(@Param("chapter") Long chapter, @Param("manga_id") Long manga_id);
}
