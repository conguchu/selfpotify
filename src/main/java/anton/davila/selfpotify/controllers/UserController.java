package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.ChangeRoleRequest;
import anton.davila.selfpotify.controllers.dto.SignupRequest;
import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.AdminRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import anton.davila.selfpotify.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder encoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @setupGuard.inSetupMode()")
    public List<User> getAllUsers() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/genres/recent")
    public ResponseEntity<List<String>> getRecentGenres(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getLast10GenresListened(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @setupGuard.inSetupMode()")
    public ResponseEntity<?> createUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.findByUsername(signUpRequest.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is already taken!");
        }

        User user;
        if (signUpRequest.isAdmin()) {
            user = new Admin();
        } else {
            user = new User();
        }

        user.setUsername(signUpRequest.getUsername());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("User created successfully by admin!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            // Encode password if it's being updated
            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                userDetails.setPassword(encoder.encode(userDetails.getPassword()));
            }
            return ResponseEntity.ok(userService.update(id, userDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody ChangeRoleRequest request) {
        try {
            return ResponseEntity.ok(userService.changeRole(id, request.isAdmin()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
