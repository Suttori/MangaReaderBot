package com.suttori.dao;

import com.suttori.entity.Chapter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface MangaChapterRepository extends JpaRepository<Chapter, Long> {


    @Query(value = """
        WITH RECURSIVE chapter_chain AS (
            SELECT *
            FROM copy_message_manga
            WHERE manga_id = :mangaId AND catalog_name = :catalogName AND language_code = :languageCode AND prev_chapter_id IS NULL
            UNION ALL
            SELECT c.*
            FROM copy_message_manga c
            INNER JOIN chapter_chain cc ON c.prev_chapter_id = cc.chapter_id
        )
        SELECT * FROM chapter_chain;
        """, nativeQuery = true)
    List<Chapter> getChaptersInOrder(@Param("mangaId") String mangaId, @Param("catalogName") String catalogName, @Param("languageCode") String languageCode);


    Chapter findFirstByMangaIdAndVolAndChapter(String mangaId, String vol, String chapter);

    Chapter findByChapterId(String chapterId);
    ArrayList<Chapter> findAllByBackupMessageIdIsNull();

    ArrayList<Chapter> findAllByMangaIdAndCatalogName(String mangaId, String catalogName);

    ArrayList<Chapter> findAllByMangaIdAndCatalogNameAndLanguageCode(String mangaId, String catalogName, String languageCode);

    boolean existsByChapterIdAndCatalogName(String chapterId, String catalogName);

    @Transactional
    void deleteByMessageId(Integer messageId);

    @Transactional
    void deleteByChapterId(String chapterId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET next_chapter_id = ? WHERE id = ?", nativeQuery = true)
    void setNextChapter(@Param("next_chapter_id") String next_chapter_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET next_chapter_id = ? WHERE chapter_id = ?", nativeQuery = true)
    void setNextChapterByChapterId(@Param("next_chapter_id") String next_chapter_id, @Param("chapter_id") String chapter_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET prev_chapter_id = ? WHERE chapter_id = ?", nativeQuery = true)
    void setPrevChapterByChapterId(@Param("prev_chapter_id") String prev_chapter_id, @Param("chapter_id") String chapter_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET prev_chapter_id = ? WHERE id = ?", nativeQuery = true)
    void setPrevChapter(@Param("prev_chapter_id") String prev_chapter_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET message_id = ? WHERE id = ?", nativeQuery = true)
    void setMessageId(@Param("message_id") Integer message_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET status = ? WHERE id = ?", nativeQuery = true)
    void setStatus(@Param("status") String status, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET telegraph_url = ? WHERE id = ?", nativeQuery = true)
    void setTelegraphUrl(@Param("telegraph_url") String telegraph_url, @Param("id") Long id);
}
