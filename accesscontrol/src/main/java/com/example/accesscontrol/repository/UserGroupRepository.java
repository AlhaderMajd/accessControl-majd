package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.entity.UserGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroupId> {
    List<UserGroup> findByUserId(Long userId);
    List<UserGroup> findByUserIdInAndGroupIdIn(List<Long> userIds, List<Long> groupIds);
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
    int deleteByUserIdInAndGroupIdIn(List<Long> userIds, List<Long> groupIds);
    void deleteByUserIdIn(List<Long> userIds);

}
