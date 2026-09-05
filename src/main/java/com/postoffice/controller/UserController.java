package com.postoffice.controller;

import com.postoffice.dto.LedgerDto;
import com.postoffice.dto.UserDto;
import com.postoffice.model.User;
import com.postoffice.repository.UserRepository;
import com.postoffice.repository.WalletLedgerRepository;
import com.postoffice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final AuthService authService;

    public UserController(UserRepository userRepository,
                          WalletLedgerRepository ledgerRepository,
                          AuthService authService) {
        this.userRepository = userRepository;
        this.ledgerRepository = ledgerRepository;
        this.authService = authService;
    }

    @GetMapping
    public List<UserDto> getReceivers() {
        User me = authService.requireCurrentUser();
        return userRepository.findByDeletedAtIsNullAndIdNot(me.getId())
                .stream().map(UserDto::from).toList();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe() {
        return ResponseEntity.ok(UserDto.from(authService.requireCurrentUser()));
    }

    @GetMapping("/me/ledger")
    public List<LedgerDto> getMyLedger() {
        User me = authService.requireCurrentUser();
        return ledgerRepository.findByUserIdOrderByCreatedAtDesc(me.getId())
                .stream().map(LedgerDto::from).toList();
    }
}
