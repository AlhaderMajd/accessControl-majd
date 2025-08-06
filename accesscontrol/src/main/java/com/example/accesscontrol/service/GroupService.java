package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.AssignUsersToGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    public Group getByIdOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    public void getByIdsOrThrow(List<Long> groupIds) {
        List<Long> existingIds = groupRepository.findAllById(groupIds)
                .stream()
                .map(Group::getId)
                .toList();

        if (existingIds.size() != groupIds.size()) {
            throw new ResourceNotFoundException("Some groups not found");
        }
    }

}
