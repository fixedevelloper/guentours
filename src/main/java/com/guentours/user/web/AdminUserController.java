package com.guentours.user.web;

import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
import com.guentours.user.service.AdminUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** Admin-only read access to every account (see {@code /api/admin/**} in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> allUsers() {
        List<AdminUserResponse> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(AdminUserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
