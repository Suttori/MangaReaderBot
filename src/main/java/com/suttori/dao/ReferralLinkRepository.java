package com.suttori.dao;

import com.suttori.entity.ReferralLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

    @Query(value = "SELECT * FROM link WHERE name LIKE %:name%", nativeQuery = true)
    List<ReferralLink> findAllContainsName(String name);

}
