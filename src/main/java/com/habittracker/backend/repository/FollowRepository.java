package com.habittracker.backend.repository;

import com.habittracker.backend.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    // Get list of users that a specific user follows
    @Query("SELECT f.followed FROM Follow f WHERE f.follower.id = :userId")
    List<com.habittracker.backend.model.User> findFollowingByUserId(@Param("userId") Long userId);

    // Get list of users following a specific user
    @Query("SELECT f.follower FROM Follow f WHERE f.followed.id = :userId")
    List<com.habittracker.backend.model.User> findFollowersByUserId(@Param("userId") Long userId);
}