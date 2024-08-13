package com.suttori.dao;

import com.suttori.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationEntityRepository extends JpaRepository<NotificationEntity, Long> {

    NotificationEntity findByMangaIdAndUserId(String mangaId, Long userId);

    NotificationEntity findByMangaDatabaseIdAndUserId(Long mangaDatabaseId, Long userId);

    List<NotificationEntity> findAllByCatalogName(String catalogName);


}
