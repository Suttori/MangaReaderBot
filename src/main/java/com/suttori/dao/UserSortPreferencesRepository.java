package com.suttori.dao;

import com.suttori.entity.User;
import com.suttori.entity.UserSortPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserSortPreferencesRepository extends JpaRepository<UserSortPreferences, Long> {



    UserSortPreferences findByUserIdAndCatalogName(Long userId, String catalogName);

    boolean existsByUserIdAndCatalogName(Long userId, String catalogName);

    @Transactional
    void deleteAllByUserIdAndCatalogName(Long userId, String catalogName);

    @Transactional
    @Modifying
    @Query(value = "UPDATE user_sort_preference SET sort_name = ?, sort_type = ? WHERE user_id = ? AND catalog_name = ?", nativeQuery = true)
    void setSortNameAndSortType(@Param("sort_name") String sort_name, @Param("sort_type") String sort_type, @Param("user_id") Long user_id, @Param("catalog_name") String catalog_name);

    @Transactional
    @Modifying
    @Query(value = "UPDATE user_sort_preference SET sort_type = ? WHERE user_id = ? AND catalog_name = ?", nativeQuery = true)
    void setSortType(@Param("sort_type") String sort_type, @Param("user_id") Long user_id, @Param("catalog_name") String catalog_name);

}
