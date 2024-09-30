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
public interface ProfileRepository extends JpaRepository<Profile_child, Integer> {

    List<Profile_child> findByMemberUser(MemberUser memberUser);

    Optional<Profile_child> findByChildId(Integer childId);

    @Query("SELECT COALESCE(MAX(p.childId), 0) FROM Profile_child p WHERE p.memberUser = :memberUser")
    Integer findMaxChildIdByMemberUser(@Param("memberUser") MemberUser memberUser);

    List<Profile_child> findByNameContaining(String name);

    List<Profile_child> findBySex(Short sex);

    List<Profile_child> findByBirthdate(LocalDate birthdate);
}
