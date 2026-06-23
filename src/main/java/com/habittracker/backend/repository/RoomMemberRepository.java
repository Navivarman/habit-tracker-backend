package com.habittracker.backend.repository;

import com.habittracker.backend.model.RoomMember;
import com.habittracker.backend.model.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {
    List<RoomMember> findByUserId(Long userId);
    List<RoomMember> findByRoomIdOrderByCurrentStepDesc(Long roomId); // For leaderboards
}