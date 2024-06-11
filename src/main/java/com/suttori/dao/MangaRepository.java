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
    @Query(value = "UPDATE manga SET cover_file_id = ? WHERE manga_id = ? AND catalog_name = ?", nativeQuery = true)
    void setCoverFileId(@Param("cover_file_id") String cover_file_id, @Param("manga_id") String manga_id, @Param("catalog_name") String catalog_name);

}
