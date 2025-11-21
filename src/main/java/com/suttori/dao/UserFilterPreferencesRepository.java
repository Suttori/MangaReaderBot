package com.suttori.dao;

import com.suttori.entity.User;
import com.suttori.entity.UserFilterPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserFilterPreferencesRepository extends JpaRepository<UserFilterPreference, Long> {


    UserFilterPreference findByUserIdAndFilterTypeAndFilterValueAndCatalogName(Long userId, String filterType, String filterValue, String catalogName);

    List<UserFilterPreference> findAllByUserIdAndFilterTypeAndCatalogName(Long userId, String filterType, String catalogName);

    List<UserFilterPreference> findAllByUserIdAndCatalogName(Long userId, String catalogName);

    @Transactional
    void deleteAllByUserIdAndFilterTypeAndCatalogName(Long userId, String filterType, String catalogName);

    @Transactional
    void deleteAllByUserIdAndCatalogName(Long userId, String catalogName);

    boolean existsByUserIdAndFilterTypeAndCatalogName(Long userId, String filterType, String catalogName);

    boolean existsByUserIdAndCatalogName(Long userId, String catalogName);

//    @Transactional
//    @Modifying
//    @Query(value = "UPDATE user_sort_preference SET sort_type = ? WHERE user_database_id = ?", nativeQuery = true)
//    void setSortType(@Param("sort_type") String sort_type, @Param("user_database_id") Long user_database_id);

}
