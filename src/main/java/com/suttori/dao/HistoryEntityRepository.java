package com.suttori.dao;

import com.suttori.entity.HistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface HistoryEntityRepository extends JpaRepository<HistoryEntity, Long> {

    HistoryEntity findByMangaIdAndUserId(Long mangaId, Long userId);

    ArrayList<HistoryEntity> findAllByUserId(Long userId, Pageable pageable);
}
