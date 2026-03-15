package com.jobagent.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.ProfileRequest;
import com.jobagent.server.dto.ProfileResponse;
import com.jobagent.server.repository.ProfileRepository;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.ProfileEntity;
import com.jobagent.server.store.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper mapper;

    public ProfileService(UserRepository userRepository,
                          ProfileRepository profileRepository,
                          ObjectMapper mapper) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mapper = mapper;
    }

    public ProfileResponse fetch(String userId) {
        UserEntity user = requireUser(userId);
        ProfileEntity profile = profileRepository.findById(userId)
            .orElseGet(() -> new ProfileEntity(userId, "", "", "", null, "", "[]", Instant.now()));
        return toResponse(user, profile);
    }

    public ProfileResponse save(String userId, ProfileRequest request) {
        UserEntity user = requireUser(userId);
        ProfileEntity profile = profileRepository.findById(userId)
            .orElseGet(() -> new ProfileEntity(userId, "", "", "", null, "", "[]", Instant.now()));
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        profile.setCity(request.city());
        profile.setYearsExperience(request.yearsExperience());
        profile.setSummary(request.summary());
        profile.setSkillsJson(writeSkills(request.skills()));
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        user.setProfileStatus(isComplete(profile) ? "COMPLETE" : "INCOMPLETE");
        userRepository.save(user);
        return toResponse(user, profile);
    }

    private ProfileResponse toResponse(UserEntity user, ProfileEntity profile) {
        return new ProfileResponse(new ProfileResponse.ProfilePayload(
            user.getAccount(),
            defaultString(user.getEmail()),
            defaultString(profile.getFullName()),
            defaultString(profile.getPhone()),
            defaultString(profile.getCity()),
            profile.getYearsExperience(),
            defaultString(profile.getSummary()),
            readSkills(profile.getSkillsJson()),
            user.getProfileStatus(),
            profile.getUpdatedAt()
        ));
    }

    private UserEntity requireUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    private boolean isComplete(ProfileEntity profile) {
        return !defaultString(profile.getFullName()).isBlank()
            && !defaultString(profile.getPhone()).isBlank()
            && !defaultString(profile.getCity()).isBlank();
    }

    private String writeSkills(List<String> skills) {
        try {
            return mapper.writeValueAsString(skills == null ? List.of() : skills);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readSkills(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
