package com.pawar.todo.amt.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pawar.todo.amt.model.UiAction;

@Repository
public interface UiActionRepository extends JpaRepository<UiAction, Integer> {

    List<UiAction> findByViewContextOrderBySidebarCategoryAscPanelTitleAscActionLabelAsc(String viewContext);

    @Query("select distinct action.viewContext from UiAction action")
    List<String> findDistinctViewContexts();
}