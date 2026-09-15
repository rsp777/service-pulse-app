package com.pawar.todo.amt.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawar.todo.amt.model.AlertEvent;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Integer> {
    List<AlertEvent> findTop50ByStatusOrderByTriggeredDttmDesc(String status);
    List<AlertEvent> findByAlertIdAndServerIdAndServiceIdAndStatus(Integer alertId, Integer serverId, Integer serviceId, String status);
    List<AlertEvent> findByServerIdAndServiceIdAndStatus(Integer serverId, Integer serviceId, String status);
    boolean existsByServerIdAndServiceIdAndStatus(Integer serverId, Integer serviceId, String status);
    boolean existsByAlertIdAndServerIdAndServiceIdAndStatus(Integer alertId, Integer serverId, Integer serviceId, String status);
    
    long countByIsReadFalse();
    List<AlertEvent> findByIsReadFalseOrderByTriggeredDttmDesc();
}
