package com.example.demo.repository;

import com.example.demo.entity.MemberUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberUserRepository extends JpaRepository<MemberUser, Long> {

    Optional<MemberUser> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);

    @Query("SELECT m FROM MemberUser m WHERE m.userEmail LIKE %:keyword%")
    List<MemberUser> findByUserEmailContaining(@Param("keyword") String keyword);

    @Query("SELECT COUNT(m) FROM MemberUser m")
    long countTotalUsers();

    @Query(value = "SELECT * FROM member_user ORDER BY user_no DESC LIMIT 1", nativeQuery = true)
    Optional<MemberUser> findLatestUser();

    @Query("SELECT m FROM MemberUser m LEFT JOIN FETCH m.profiles")
    List<MemberUser> findAllWithProfiles();
}