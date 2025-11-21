package com.suttori.dao;

import com.suttori.entity.Chapter;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CustomMangaChapterRepositoryImpl implements CustomMangaChapterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = "INSERT INTO copy_message_manga (" +
            "message_id, backup_message_id, catalog_name, manga_id, manga_data_base_id, chapter_id, " +
            "next_chapter_id, prev_chapter_id, type, format, name, telegraph_url, vol, chapter, " +
            "status, added_at, language_code" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (chapter_id) DO UPDATE SET " +
            "message_id = EXCLUDED.message_id, backup_message_id = EXCLUDED.backup_message_id, " +
            "catalog_name = EXCLUDED.catalog_name, manga_id = EXCLUDED.manga_id, " +
            "manga_data_base_id = EXCLUDED.manga_data_base_id, next_chapter_id = EXCLUDED.next_chapter_id, " +
            "prev_chapter_id = EXCLUDED.prev_chapter_id, type = EXCLUDED.type, format = EXCLUDED.format, " +
            "name = EXCLUDED.name, telegraph_url = EXCLUDED.telegraph_url, vol = EXCLUDED.vol, " +
            "chapter = EXCLUDED.chapter, status = EXCLUDED.status, added_at = EXCLUDED.added_at, " +
            "language_code = EXCLUDED.language_code";

    @Override
    @Transactional
    public void saveAllChapters(List<Chapter> chapters) {
        for (Chapter chapter : chapters) {
            jdbcTemplate.update(INSERT_SQL,
                    chapter.getMessageId(),
                    chapter.getBackupMessageId(),
                    chapter.getCatalogName(),
                    chapter.getMangaId(),
                    chapter.getMangaDataBaseId(),
                    chapter.getChapterId(),
                    chapter.getNextChapter() != null ? chapter.getNextChapter().getChapterId() : null,
                    chapter.getPrevChapter() != null ? chapter.getPrevChapter().getChapterId() : null,
                    chapter.getType(),
                    chapter.getFormat(),
                    chapter.getName(),
                    chapter.getTelegraphUrl(),
                    chapter.getVol(),
                    chapter.getChapter(),
                    chapter.getStatus(),
                    chapter.getAddedAt(),
                    chapter.getLanguageCode()
            );
        }
    }
}
