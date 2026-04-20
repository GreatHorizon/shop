package com.example.main.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FilesService {
    @Value("${main.uploads-path}")
    private String UPLOAD_DIR;

    public Mono<String> upload(FilePart file) {
        return Mono.fromCallable(() -> {
                    Path dir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
                    Files.createDirectories(dir);

                    String originalName = file.filename();
                    String ext = "";

                    int dotIndex = originalName.lastIndexOf('.');
                    if (dotIndex >= 0) {
                        ext = originalName.substring(dotIndex);
                    }

                    String newFileName = UUID.randomUUID() + ext;

                    return dir.resolve(newFileName);
                })
                .flatMap(filePath -> file.transferTo(filePath)
                        .thenReturn(filePath.getFileName().toString()));
    }

}