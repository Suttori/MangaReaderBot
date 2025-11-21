package com.suttori.dao;


import com.suttori.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {


    ActivationToken findByIdAndUserIdAndToken(Long id, Long userId, String token);

}
