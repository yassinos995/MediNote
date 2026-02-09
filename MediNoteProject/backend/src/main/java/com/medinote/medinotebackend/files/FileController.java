package com.medinote.medinotebackend.files;

import com.medinote.medinotebackend.storage.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storageService;

    public FileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) throws Exception {
        storageService.uploadFile(file);
        return "Uploaded!";
    }
}
