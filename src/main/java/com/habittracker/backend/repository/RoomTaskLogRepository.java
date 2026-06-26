package com.habittracker.backend.repository;

import com.habittracker.backend.model.RoomTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomTaskLogRepository extends JpaRepository<RoomTaskLog, Long> {
    Optional<RoomTaskLog> findByRoomTaskIdAndUserIdAndLogDate(Long taskId, Long userId, LocalDate date);
    long countByRoomTaskIdInAndUserIdAndLogDateAndCompletedTrue(List<Long> taskIds, Long userId, LocalDate date);
}