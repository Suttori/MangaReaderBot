package com.suttori.dao;

import com.suttori.entity.Manga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MangaRepository extends JpaRepository<Manga, Long> {


    Manga findByMangaIdAndCatalogName(String mangaId, String catalogName);

    Manga findByMangaIdAndCatalogNameAndLanguageCode(String mangaId, String catalogName, String languageCode);


    @Transactional
    @Modifying
    @Query(value = "UPDATE manga SET cover_file_id = ? WHERE id = ?", nativeQuery = true)
    void setCoverFileId(@Param("cover_file_id") String cover_file_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE manga SET cover_url = ? WHERE id = ?", nativeQuery = true)
    void setCoverUrl(@Param("cover_url") String cover_url, @Param("id") Long id);

}
