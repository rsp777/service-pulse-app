package com.pawar.todo.amt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.todo.amt.model.AlertConditionType;
import java.util.Optional;

@Repository
public interface AlertConditionTypeRepository extends JpaRepository<AlertConditionType, Integer> {
    Optional<AlertConditionType> findByName(String name);
}
