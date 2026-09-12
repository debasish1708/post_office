package com.postoffice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.secret-key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = java.util.UUID.randomUUID().toString() + fileExtension;
            String path = folder + "/" + filename;
            String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + path;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return filename; // Returning only filename
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Supabase", e);
        }
        return null;
    }

    public byte[] downloadFile(String path) {
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new RuntimeException("Failed to download file from Supabase: " + response.getStatusCode());
    }

    public void deleteFile(String path) {
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public java.util.List<String> listFiles() {
        String url = supabaseUrl + "/storage/v1/object/list/" + bucketName;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(
            "{\"prefix\": \"letters/\", \"limit\": 1000, \"offset\": 0}", 
            headers
        );
        
        try {
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, 
                new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            if (response.getBody() == null) {
                return java.util.Collections.emptyList();
            }

            return response.getBody().stream()
                    .map(m -> (String) m.get("name"))
                    .toList();
        } catch (Exception e) {
            System.err.println("Error listing files: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error listing files from Supabase", e);
        }
    }

    public void deleteAllFiles() {
        java.util.List<String> files = listFiles();
        if (!files.isEmpty()) {
            deleteFiles(files);
        }
    }

    public void deleteFiles(java.util.List<String> paths) {
        String url = supabaseUrl + "/storage/v1/object/" + bucketName;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Process in batches of 50 to avoid API request size/count limits
        int batchSize = 50;
        for (int i = 0; i < paths.size(); i += batchSize) {
            int end = Math.min(i + batchSize, paths.size());
            java.util.List<String> batch = paths.subList(i, end);
            
            java.util.Map<String, java.util.List<String>> body = java.util.Map.of("objects", batch);
            HttpEntity<java.util.Map<String, java.util.List<String>>> entity = new HttpEntity<>(body, headers);
            
            try {
                restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            } catch (Exception e) {
                System.err.println("Error deleting batch of files: " + e.getMessage());
                throw new RuntimeException("Error deleting batch of files from Supabase", e);
            }
        }
    }

    public String getPublicUrl(String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + path;
    }
}
