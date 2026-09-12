package com.postoffice.controller;

import com.postoffice.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-storage")
public class StorageTestController {

    @Autowired
    private SupabaseStorageService storageService;

    @GetMapping("/download/{path}")
    public ResponseEntity<byte[]> download(@PathVariable String path) {
        byte[] data = storageService.downloadFile(path);
        
        String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
