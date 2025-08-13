package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRoleRepository extends JpaRepository<GroupRole, Long> {

    boolean existsByGroup_IdAndRole_Id(Long groupId, Long roleId);

    List<GroupRole> findByGroup_IdInAndRole_IdIn(List<Long> groupIds, List<Long> roleIds);

    void deleteByGroup_IdInAndRole_IdIn(List<Long> groupIds, List<Long> roleIds);

    void deleteByGroup_IdIn(List<Long> groupIds);

    void deleteByRole_IdIn(List<Long> roleIds);

    List<GroupRole> findByGroup_Id(Long groupId);
}
