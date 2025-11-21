package com.suttori.dao;

import com.suttori.entity.NotificationChapterMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationChapterMappingRepository extends JpaRepository<NotificationChapterMapping, Long> {

    NotificationChapterMapping findByMangaDatabaseId(Long mangaDatabaseId);

    NotificationChapterMapping findByMangaIdAndCatalogName(String mangaId, String catalogName);

    @Transactional
    @Modifying
    @Query(value = "UPDATE notification_chapter_mapping SET chapter = :chapter WHERE manga_id = :mangaId AND catalog_name = :catalogName", nativeQuery = true)
    void setChapter(@Param("chapter") String chapter, @Param("mangaId") String mangaId, @Param("catalogName") String catalogName);

    @Transactional
    @Modifying
    @Query(value = "UPDATE notification_chapter_mapping SET chapter = :chapter WHERE manga_database_id = :mangaDatabaseId", nativeQuery = true)
    void setChapter(@Param("chapter") String chapter, @Param("mangaDatabaseId") Long mangaDatabaseId);


}
