package com.suttori.dao;

import com.suttori.entity.MangaStatusParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface MangaStatusParameterRepository extends JpaRepository<MangaStatusParameter, Long> {


        MangaStatusParameter findByMangaDatabaseIdAndUserId(Long mangaDatabaseId, Long userId);

        MangaStatusParameter findByMangaIdAndUserId(String mangaId, Long userId);

        ArrayList<MangaStatusParameter> findAllByUserIdAndStatus(Long userId, String status);

        ArrayList<MangaStatusParameter> findAllByUserIdAndStatus(Long userId, String status, Pageable pageable);

}
