package com.jobagent.server.controller;

import com.jobagent.server.dto.BlacklistCompanyRequest;
import com.jobagent.server.dto.FollowItem;
import com.jobagent.server.dto.FollowListResponse;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.repository.UserCompanyBlacklistRepository;
import com.jobagent.server.repository.UserJobActionRepository;
import com.jobagent.server.service.AuditService;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.UserCompanyBlacklistEntity;
import com.jobagent.server.store.UserJobActionEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class JobActionController {

    private final AuthService authService;
    private final AuditService auditService;
    private final JobPostRepository jobPostRepository;
    private final TaskRepository taskRepository;
    private final UserJobActionRepository userJobActionRepository;
    private final UserCompanyBlacklistRepository userCompanyBlacklistRepository;

    public JobActionController(AuthService authService,
                               AuditService auditService,
                               JobPostRepository jobPostRepository,
                               TaskRepository taskRepository,
                               UserJobActionRepository userJobActionRepository,
                               UserCompanyBlacklistRepository userCompanyBlacklistRepository) {
        this.authService = authService;
        this.auditService = auditService;
        this.jobPostRepository = jobPostRepository;
        this.taskRepository = taskRepository;
        this.userJobActionRepository = userJobActionRepository;
        this.userCompanyBlacklistRepository = userCompanyBlacklistRepository;
    }

    @PostMapping("/jobs/{jobPostId}/follow")
    public Map<String, Object> follow(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable("jobPostId") String jobPostId) {
        String userId = authService.requireUserId(authorization);
        JobPostEntity jobPost = requireOwnedJobPost(jobPostId, userId);
        UserJobActionEntity action = upsertJobAction(userId, jobPost, "FOLLOW");
        auditService.record(userId, "JOB_FOLLOW", "{\"job_post_id\":\"" + jobPostId + "\"}");
        return Map.of(
            "status", "ok",
            "follow_item", new FollowItem(
                jobPost.getId(),
                jobPost.getTitle(),
                jobPost.getCompany(),
                action.getCreatedAt()
            )
        );
    }

    @PostMapping("/jobs/{jobPostId}/ignore")
    public Map<String, String> ignore(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable("jobPostId") String jobPostId) {
        String userId = authService.requireUserId(authorization);
        JobPostEntity jobPost = requireOwnedJobPost(jobPostId, userId);
        upsertJobAction(userId, jobPost, "IGNORE");
        jobPost.setStatus("ARCHIVED");
        jobPostRepository.save(jobPost);
        auditService.record(userId, "JOB_IGNORE", "{\"job_post_id\":\"" + jobPostId + "\"}");
        return Map.of("status", "ok");
    }

    @PostMapping("/blacklist/company")
    public Map<String, String> blacklistCompany(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @RequestBody BlacklistCompanyRequest request) {
        String userId = authService.requireUserId(authorization);
        String companyName = normalize(request.companyName());
        String source = normalize(request.source());
        if (companyName == null || source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid blacklist request");
        }
        userCompanyBlacklistRepository.findByUserIdAndCompanyNameAndSource(userId, companyName, source)
            .orElseGet(() -> userCompanyBlacklistRepository.save(
                new UserCompanyBlacklistEntity(UUID.randomUUID().toString(), userId, companyName, source, Instant.now())
            ));
        auditService.record(userId, "COMPANY_BLACKLIST", "{\"company_name\":\"" + companyName + "\",\"source\":\"" + source + "\"}");
        return Map.of("status", "ok");
    }

    @GetMapping("/follows")
    public FollowListResponse follows(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "20") int size) {
        String userId = authService.requireUserId(authorization);
        var pageable = PageRequest.of(page, size);
        var actions = userJobActionRepository.findAllByUserIdAndActionTypeOrderByCreatedAtDescIdDesc(userId, "FOLLOW", pageable);
        List<FollowItem> items = actions.getContent().stream()
            .map(action -> toFollowItem(action, userId))
            .toList();
        return new FollowListResponse(items, page, size, actions.getTotalElements());
    }

    private FollowItem toFollowItem(UserJobActionEntity action, String userId) {
        JobPostEntity jobPost = requireOwnedJobPost(action.getJobPostId(), userId);
        return new FollowItem(
            jobPost.getId(),
            jobPost.getTitle(),
            jobPost.getCompany(),
            action.getCreatedAt()
        );
    }

    private JobPostEntity requireOwnedJobPost(String jobPostId, String userId) {
        JobPostEntity jobPost = jobPostRepository.findById(jobPostId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "job post not found"));
        taskRepository.findByIdAndUserId(jobPost.getTaskId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "job post not found"));
        return jobPost;
    }

    private UserJobActionEntity upsertJobAction(String userId, JobPostEntity jobPost, String actionType) {
        Instant now = Instant.now();
        UserJobActionEntity entity = userJobActionRepository.findByUserIdAndJobPostId(userId, jobPost.getId())
            .orElseGet(() -> new UserJobActionEntity(
                UUID.randomUUID().toString(),
                userId,
                jobPost.getId(),
                jobPost.getSource(),
                actionType,
                now
            ));
        boolean sameAction = actionType.equals(entity.getActionType());
        entity.setSource(jobPost.getSource());
        entity.setActionType(actionType);
        if (!sameAction) {
            entity.setCreatedAt(now);
        }
        return userJobActionRepository.save(entity);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
