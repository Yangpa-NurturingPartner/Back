package com.example.demo.repository;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile_child, Long> {

    List<Profile_child> findByMemberUser(MemberUser memberUser);

    Optional<Profile_child> findByChildId(Long childId);

    List<Profile_child> findByNameContaining(String name);

    List<Profile_child> findBySex(Short sex);

    List<Profile_child> findByBirthdate(LocalDate birthdate);

    @Query("SELECT p FROM Profile_child p WHERE p.memberUser.userEmail = :email")
    List<Profile_child> findByMemberUserEmail(@Param("email") String email);

    @Query("SELECT COUNT(p) FROM Profile_child p WHERE p.memberUser = :memberUser")
    long countByMemberUser(@Param("memberUser") MemberUser memberUser);

    @Query("SELECT p FROM Profile_child p WHERE p.birthdate BETWEEN :startDate AND :endDate")
    List<Profile_child> findByBirthdateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Profile_child p WHERE p.joinDate = :joinDate")
    List<Profile_child> findByJoinDate(@Param("joinDate") LocalDate joinDate);
}