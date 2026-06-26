package com.habittracker.backend.repository;

import com.habittracker.backend.model.RoomTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomTaskRepository extends JpaRepository<RoomTask, Long> {
    List<RoomTask> findByRoomId(Long roomId);
    long countByRoomId(Long roomId);
}