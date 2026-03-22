package org.tech_solutions.application.controller.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.controller.dto.UserRegisterDTO;
import org.tech_solutions.application.controller.dto.UserDataDTO;
import org.tech_solutions.application.controller.dto.LoginDTO;
import org.tech_solutions.application.controller.dto.UserLogedDTO;
import org.tech_solutions.application.model.User;
import org.tech_solutions.application.service.AuthService;
import org.tech_solutions.application.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/auth")
    public ResponseEntity<UserLogedDTO> login(@Valid @RequestBody LoginDTO request) {
        UserLogedDTO response = authService.autenticate(request);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDataDTO> register(@Valid @RequestBody UserRegisterDTO request) {
        User user = userService.registrate(UserMapper.toModel(request));
        return ResponseEntity.status(201).body(UserMapper.toDTO(user));
    }

    @GetMapping
    public ResponseEntity<List<UserDataDTO>> listAll() {
        List<User> foundUsers = userService.listAll();

        return foundUsers.isEmpty() ?
                ResponseEntity.status(204).build()
             :  ResponseEntity.status(200).body(UserMapper.toDTO(foundUsers));
    }
}
