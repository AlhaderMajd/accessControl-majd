package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroup.Id> {
    List<UserGroup> findByIdUserId(Long userId);
    List<UserGroup> findByIdUserIdInAndIdGroupIdIn(List<Long> userIds, List<Long> groupIds);
    boolean existsByIdUserIdAndIdGroupId(Long userId, Long groupId);
    int deleteByIdUserIdInAndIdGroupIdIn(List<Long> userIds, List<Long> groupIds);
    void deleteByIdUserIdIn(List<Long> userIds);
    List<UserGroup> findByIdGroupId(Long groupId);
    void deleteByIdGroupIdIn(List<Long> groupIds);

}
