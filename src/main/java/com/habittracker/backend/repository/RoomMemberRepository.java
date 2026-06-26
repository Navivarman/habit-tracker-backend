package com.habittracker.backend.repository;

import com.habittracker.backend.model.RoomMember;
import com.habittracker.backend.model.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    // 1. Used by RoomController to find a single member context
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    // 2. Used by RoomService to update steps for any room a user belongs to
    List<RoomMember> findByUserId(Long userId);

    // 3. Used by RoomController to fetch standings ordered by Ludo steps
    List<RoomMember> findByRoomIdOrderByCurrentStepDesc(Long roomId);

    // 4. Used by RoomController to get all APPROVED rooms a user has joined
    @Query("SELECT rm FROM RoomMember rm WHERE rm.user.id = :userId AND rm.status = 'APPROVED'")
    List<RoomMember> findApprovedRoomsByUserId(@Param("userId") Long userId);

    // 5. Used by RoomController to fetch pending join requests for an admin
    @Query("SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.status = 'PENDING'")
    List<RoomMember> findPendingRequestsByRoomId(@Param("roomId") Long roomId);

    // Delete an existing member mapping cleanly on room departure
    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}