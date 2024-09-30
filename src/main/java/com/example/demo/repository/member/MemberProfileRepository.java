package com.example.demo.repository.member;

import com.example.demo.entity.MemberProfile;
import com.example.demo.entity.MemberUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Optional<MemberProfile> findByMemberUser(MemberUser memberUser);

    Optional<MemberProfile> findByUserName(String userName);

    List<MemberProfile> findByJoinDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT mp FROM MemberProfile mp WHERE mp.memberUser.userEmail = :email")
    Optional<MemberProfile> findByUserEmail(@Param("email") String email);

    boolean existsByMemberUser(MemberUser memberUser);

    @Query("SELECT COUNT(mp) FROM MemberProfile mp WHERE mp.joinDate = :date")
    long countProfilesCreatedOn(@Param("date") LocalDate date);
}