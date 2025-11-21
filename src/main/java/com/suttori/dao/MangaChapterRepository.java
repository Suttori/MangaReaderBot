package com.suttori.dao;

import com.suttori.dto.ChapterDto;
import com.suttori.entity.Chapter;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MangaChapterRepository extends JpaRepository<Chapter, Long>, CustomMangaChapterRepository {

    @Query("SELECT new com.suttori.dto.ChapterDto(" +
            "c.id, c.messageId, c.backupMessageId, c.catalogName, c.mangaId, c.mangaDataBaseId, " +
            "c.chapterId, c.nextChapter.chapterId, c.prevChapter.chapterId, c.type, c.format, c.name, c.telegraphUrl, " +
            "c.vol, c.chapter, c.status, c.addedAt, c.languageCode, c.pdfMessageId, c.telegraphMessageId, c.pdfStatusDownload, c.telegraphStatusDownload, c.chapterName) " +
            "FROM Chapter c WHERE c.mangaId = :mangaId AND c.catalogName = :catalogName")
    List<ChapterDto> findAllByMangaIdAndCatalogName(@Param("mangaId") String mangaId, @Param("catalogName") String catalogName);

    @Query("SELECT new com.suttori.dto.ChapterDto(" +
            "c.id, c.messageId, c.backupMessageId, c.catalogName, c.mangaId, c.mangaDataBaseId, " +
            "c.chapterId, c.nextChapter.chapterId, c.prevChapter.chapterId, c.type, c.format, c.name, c.telegraphUrl, " +
            "c.vol, c.chapter, c.status, c.addedAt, c.languageCode, c.pdfMessageId, c.telegraphMessageId, c.pdfStatusDownload, c.telegraphStatusDownload, c.chapterName) " +
            "FROM Chapter c WHERE c.mangaId = :mangaId AND c.catalogName = :catalogName AND c.languageCode = :languageCode")
    List<ChapterDto> findAllByMangaIdAndCatalogNameAndLanguageCode(@Param("mangaId") String mangaId, @Param("catalogName") String catalogName, @Param("languageCode") String languageCode);

    @Query("SELECT new com.suttori.dto.ChapterDto(" +
            "c.id, c.messageId, c.backupMessageId, c.catalogName, c.mangaId, c.mangaDataBaseId, " +
            "c.chapterId, c.nextChapter.chapterId, c.prevChapter.chapterId, c.type, c.format, c.name, c.telegraphUrl, " +
            "c.vol, c.chapter, c.status, c.addedAt, c.languageCode, c.pdfMessageId, c.telegraphMessageId, c.pdfStatusDownload, c.telegraphStatusDownload, c.chapterName) " +
            "FROM Chapter c WHERE c.id = :id")
    ChapterDto findChapterDtoById(@Param("id") Long id);

    @Query("SELECT new com.suttori.dto.ChapterDto(" +
            "c.id, c.messageId, c.backupMessageId, c.catalogName, c.mangaId, c.mangaDataBaseId, " +
            "c.chapterId, c.nextChapter.chapterId, c.prevChapter.chapterId, c.type, c.format, c.name, c.telegraphUrl, " +
            "c.vol, c.chapter, c.status, c.addedAt, c.languageCode, c.pdfMessageId, c.telegraphMessageId, c.pdfStatusDownload, c.telegraphStatusDownload, c.chapterName) " +
            "FROM Chapter c WHERE c.chapterId = :chapterId")
    ChapterDto findChapterDtoByChapterId(@Param("chapterId") String chapterId);

    @Query("SELECT new com.suttori.dto.ChapterDto(" +
            "c.id, c.messageId, c.backupMessageId, c.catalogName, c.mangaId, c.mangaDataBaseId, " +
            "c.chapterId, c.nextChapter.chapterId, c.prevChapter.chapterId, c.type, c.format, c.name, c.telegraphUrl, " +
            "c.vol, c.chapter, c.status, c.addedAt, c.languageCode, c.pdfMessageId, c.telegraphMessageId, c.pdfStatusDownload, c.telegraphStatusDownload, c.chapterName) " +
            "FROM Chapter c WHERE c.telegraphUrl IS NOT NULL")
    List<ChapterDto> findChapterDtoByTelegraphUrlIsNotNull();

    Long countAllByPdfStatusDownloadOrTelegraphStatusDownload(String pdfStatus, String telegraphStatus);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM copy_message_manga WHERE chapter_id = :chapterId", nativeQuery = true)
    void deleteChapterById(@Param("chapterId") String chapterId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET next_chapter_id = ? WHERE id = ?", nativeQuery = true)
    void setNextChapter(@Param("next_chapter_id") String next_chapter_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET manga_data_base_id = :mangaDataBaseId, type = :type WHERE id = :id", nativeQuery = true)
    void setMangaDatabaseIdAndType(@Param("mangaDataBaseId") Long mangaDataBaseId, @Param("type") String type, @Param("id") Long id);

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
    @Query(value = "UPDATE copy_message_manga SET telegraph_message_id = ? WHERE id = ?", nativeQuery = true)
    void setTelegraphMessageId(@Param("telegraph_message_id") Integer telegraph_message_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET pdf_message_id = ? WHERE id = ?", nativeQuery = true)
    void setPdfMessageId(@Param("pdf_message_id") Integer pdf_message_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET status = ? WHERE id = ?", nativeQuery = true)
    void setStatus(@Param("status") String status, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET telegraph_status_download = ? WHERE id = ?", nativeQuery = true)
    void setTelegraphStatusDownload(@Param("telegraph_status_download") String telegraph_status_download, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET pdf_status_download = ? WHERE id = ?", nativeQuery = true)
    void setPdfStatusDownload(@Param("pdf_status_download") String pdf_status_download, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE copy_message_manga SET telegraph_url = ? WHERE id = ?", nativeQuery = true)
    void setTelegraphUrl(@Param("telegraph_url") String telegraph_url, @Param("id") Long id);
}
