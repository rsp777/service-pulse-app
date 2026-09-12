package com.pawar.todo.amt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.todo.amt.model.AlertEventLog;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertEventLogRepository extends JpaRepository<AlertEventLog, Integer> {
    List<AlertEventLog> findByAlertIdOrderByTriggeredDttmDesc(Integer alertId);
    List<AlertEventLog> findByTriggeredDttmBetweenOrderByTriggeredDttmDesc(LocalDateTime start, LocalDateTime end);
    List<AlertEventLog> findByAlertStatusOrderByTriggeredDttmDesc(String alertStatus);
}
