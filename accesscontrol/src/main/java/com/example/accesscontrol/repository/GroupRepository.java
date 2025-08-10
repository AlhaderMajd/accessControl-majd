package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByNameInIgnoreCase(Collection<String> names);
    Page<Group> findByNameContainingIgnoreCase(String search, Pageable pageable);
}
