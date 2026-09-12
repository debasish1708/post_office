package com.postoffice.controller;

import com.postoffice.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String path = supabaseStorageService.uploadFile(file, "letters");
        String url = supabaseStorageService.getPublicUrl(path);
        return ResponseEntity.ok(Map.of("url", url, "path", path));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMedia(@RequestParam("path") String path) {
        supabaseStorageService.deleteFile(path);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllMedia() {
        supabaseStorageService.deleteAllFiles();
        return ResponseEntity.ok(Map.of("message", "All files deleted successfully"));
    }
}
