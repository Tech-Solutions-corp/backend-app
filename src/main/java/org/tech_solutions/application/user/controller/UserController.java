package org.tech_solutions.application.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.user.dto.UserRegisterDTO;
import org.tech_solutions.application.user.dto.UserDataDTO;
import org.tech_solutions.application.auth.dto.LoginDTO;
import org.tech_solutions.application.user.dto.ForgotPasswordRequestDTO;
import org.tech_solutions.application.user.dto.ResetPasswordRequestDTO;
import org.tech_solutions.application.user.dto.UserLogedDTO;
import org.tech_solutions.application.user.mapper.UserMapper;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.auth.service.AuthService;
import org.tech_solutions.application.user.service.PasswordRecoveryService;
import org.tech_solutions.application.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("api/v1/users")
public class UserController {
    private final AuthService authService;
    private final UserService userService;
    private final PasswordRecoveryService passwordRecoveryService;

    public UserController(
            AuthService authService,
            UserService userService,
            PasswordRecoveryService passwordRecoveryService
    ) {
        this.authService = authService;
        this.userService = userService;
        this.passwordRecoveryService = passwordRecoveryService;
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

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordRecoveryService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordRecoveryService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserDataDTO>> listAll() {
        List<User> foundUsers = userService.listAll();

        return foundUsers.isEmpty() ?
                ResponseEntity.status(204).build()
             :  ResponseEntity.status(200).body(UserMapper.toDTO(foundUsers));
    }
}
