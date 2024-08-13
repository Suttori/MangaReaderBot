package com.suttori.dao;

import com.suttori.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {


    ReadStatus findByMangaIdAndChapterIdAndUserIdAndCatalogName(String mangaId, String chapterId, Long userId, String catalogName);

    boolean existsByMangaIdAndChapterIdAndUserIdAndCatalogName(String mangaId, String chapterId, Long userId, String catalogName);
}
