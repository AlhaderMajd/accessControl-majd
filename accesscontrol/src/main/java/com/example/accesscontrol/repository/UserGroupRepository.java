package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    boolean existsByUser_IdAndGroup_Id(Long userId, Long groupId);

    List<UserGroup> findByUser_IdInAndGroup_IdIn(List<Long> userIds, List<Long> groupIds);

    List<UserGroup> findByUser_Id(Long userId);

    List<UserGroup> findByGroup_Id(Long groupId);

    int deleteByUser_IdInAndGroup_IdIn(List<Long> userIds, List<Long> groupIds);

    void deleteByUser_IdIn(List<Long> userIds);

    void deleteByGroup_IdIn(List<Long> groupIds);
}
