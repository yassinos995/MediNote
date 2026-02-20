package com.medinote.medinotebackend.user;

import com.medinote.medinotebackend.user.dto.UserRequest;
import com.medinote.medinotebackend.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public UserResponse create(@RequestBody UserRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UserRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
