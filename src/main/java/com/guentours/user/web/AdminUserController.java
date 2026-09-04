package com.guentours.user.web;

import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
import com.guentours.user.service.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only read access to every account (see {@code /api/admin/**} in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** {@code q}, when present, matches against either the name or the email. */
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> allUsers(
            @RequestParam(value = "q", required = false) String q,
            Pageable pageable) {
        // Newest first by default (this used to sort by id, a random UUID - not actually
        // meaningful order) - still honors an explicit ?sort= from the client.
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        String term = (q == null || q.isBlank()) ? null : q.trim();
        Page<User> users = term == null
                ? userRepository.findAll(effective)
                : userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, effective);

        return ResponseEntity.ok(users.map(AdminUserResponse::from));
    }
}
