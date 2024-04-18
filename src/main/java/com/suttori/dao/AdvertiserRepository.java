package com.suttori.dao;

import com.suttori.entity.Advertiser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {

    boolean existsByUserid(Long userId);

    @Transactional
    void deleteByUserid(Long userId);
}
