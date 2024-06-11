package com.suttori.dao;

import com.suttori.entity.MangaDesu.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationEntityRepository extends JpaRepository<NotificationEntity, Long> {


    NotificationEntity findByMangaIdAndUserId(String mangaId, Long userId);


}
