package com.pawar.todo.amt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.todo.amt.model.AlertConditionMapping;
import java.util.List;

@Repository
public interface AlertConditionMappingRepository extends JpaRepository<AlertConditionMapping, Integer> {
    List<AlertConditionMapping> findByConditionTypeId(Integer conditionTypeId);
    List<AlertConditionMapping> findByConditionTypeIdOrderByConditionKeyAsc(Integer conditionTypeId);
}
