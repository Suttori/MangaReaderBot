package com.suttori.dao;

import com.suttori.entity.MangaDesu.CopyMessageManga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Repository
public interface CopyMessageMangaRepository extends JpaRepository<CopyMessageManga, Long> {

    CopyMessageManga findFirstByMangaIdAndVolAndChapter(Long mangaId, int vol, int chapter);

    ArrayList<CopyMessageManga> findAllByBackupMessageIdIsNull();

    @Transactional
    void deleteByMessageId(Integer messageId);

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
