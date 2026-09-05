package com.postoffice.controller;

import com.postoffice.dto.LetterResponse;
import com.postoffice.model.Letter;
import com.postoffice.model.User;
import com.postoffice.repository.LetterRepository;
import com.postoffice.service.AuthService;
import com.postoffice.service.LetterService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/letters")
public class LetterController {

    private final LetterService letterService;
    private final LetterRepository letterRepository;
    private final AuthService authService;

    public LetterController(LetterService letterService,
                            LetterRepository letterRepository,
                            AuthService authService) {
        this.letterService = letterService;
        this.letterRepository = letterRepository;
        this.authService = authService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bookLetter(
            @RequestParam("receiverId") Long receiverId,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "rawMessage", required = false) String rawMessage,
            @RequestParam("letterFile") MultipartFile letterFile,
            @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
            @RequestParam("serviceName") String serviceName) {
        try {
            User sender = authService.requireCurrentUser();
            String note = message != null ? message : rawMessage;
            Letter letter = letterService.bookLetter(sender, receiverId, note, letterFile, attachmentFile, serviceName);
            return ResponseEntity.ok(letterService.toResponse(letter));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(error("File storage error: " + e.getMessage()));
        }
    }

    @GetMapping("/sent")
    public List<LetterResponse> getSentLetters() {
        User user = authService.requireCurrentUser();
        return letterRepository.findBySenderIdAndDeletedAtIsNullOrderByPostDateDesc(user.getId())
                .stream().map(letterService::toResponse).toList();
    }

    @GetMapping("/received")
    public List<LetterResponse> getReceivedLetters() {
        User user = authService.requireCurrentUser();
        return letterRepository.findByReceiverIdAndDeletedAtIsNullOrderByPostDateDesc(user.getId())
                .stream().map(letterService::toResponse).toList();
    }

    @PostMapping("/{id}/change-service")
    public ResponseEntity<?> changeService(@PathVariable Long id, @RequestParam("newServiceName") String newServiceName) {
        try {
            Letter letter = letterService.changeService(authService.requireCurrentUser(), id, newServiceName);
            return ResponseEntity.ok(letterService.toResponse(letter));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/verify-otp")
    public ResponseEntity<?> verifyOtp(@PathVariable Long id, @RequestParam("otp") String otp) {
        try {
            Letter letter = letterService.verifyOtpAndOpen(authService.requireCurrentUser(), id, otp);
            return ResponseEntity.ok(letterService.toResponse(letter));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/simulate-delivery")
    public ResponseEntity<?> simulateDelivery(@PathVariable Long id) {
        try {
            Letter letter = letterService.forceDeliver(authService.requireCurrentUser(), id);
            return ResponseEntity.ok(letterService.toResponse(letter));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            User user = authService.requireCurrentUser();
            Letter letter = letterRepository.findAll().stream()
                    .filter(l -> filename.equals(l.getLetterImage()) || filename.equals(l.getAttachmentImage()))
                    .findFirst()
                    .orElse(null);
            if (letter == null) {
                return ResponseEntity.notFound().build();
            }
            if (!letter.getReceiver().getId().equals(user.getId()) || !letter.isRead()) {
                return ResponseEntity.status(403).build();
            }

            Path filePath = letterService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
