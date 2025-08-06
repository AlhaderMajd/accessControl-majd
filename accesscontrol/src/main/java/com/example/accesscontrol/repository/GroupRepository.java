package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
