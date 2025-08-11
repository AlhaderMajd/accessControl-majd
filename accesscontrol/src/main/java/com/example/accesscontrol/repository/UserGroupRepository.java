package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    // Exists (used in initializer sometimes)
    boolean existsByUser_IdAndGroup_Id(Long userId, Long groupId);

    // Used by UserGroupService.assignUsersToGroups
    List<UserGroup> findByUser_IdInAndGroup_IdIn(List<Long> userIds, List<Long> groupIds);

    // Used by UserGroupService.getGroupIdsByUserId / getUserIdsByGroupId
    List<UserGroup> findByUser_Id(Long userId);

    List<UserGroup> findByGroup_Id(Long groupId);

    // Used by UserGroupService.deassignUsersFromGroups
    int deleteByUser_IdInAndGroup_IdIn(List<Long> userIds, List<Long> groupIds);

    // Used by UserGroupService.deleteByUserIds / deleteByGroupIds
    void deleteByUser_IdIn(List<Long> userIds);

    void deleteByGroup_IdIn(List<Long> groupIds);
}
