package com.example.camera;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class MediaController {

    // Saari photos/videos project folder ke "uploads" me save hongi
    private final Path root = Paths.get("uploads").toAbsolutePath().normalize();

    public MediaController() throws IOException {
        Files.createDirectories(root);
    }

    @PostMapping("/photos")
    public Map<String, String> uploadPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        return save(file, "photo", ".png");
    }

    @PostMapping("/videos")
    public Map<String, String> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        String type = file.getContentType() == null ? "" : file.getContentType();
        // Chrome/Firefox -> webm, Safari -> mp4
        String ext = type.contains("mp4") ? ".mp4" : ".webm";
        return save(file, "video", ext);
    }

    @GetMapping("/files")
    public List<String> list() throws IOException {
        try (Stream<Path> s = Files.list(root)) {
            return s.map(p -> p.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
    }

    @GetMapping("/files/{name:.+}")
    public ResponseEntity<Resource> get(@PathVariable String name) {
        Path file = root.resolve(name).normalize();
        // path traversal (../) se bachne ke liye check
        if (!file.startsWith(root) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        String ct = name.endsWith(".png") ? "image/png"
                  : name.endsWith(".mp4") ? "video/mp4" : "video/webm";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .body(new FileSystemResource(file));
    }

    private Map<String, String> save(MultipartFile file, String prefix, String ext) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        // client ka filename use nahi karte, apna naam banate hain (security)
        String name = prefix + "_" + System.currentTimeMillis() + ext;
        file.transferTo(root.resolve(name));
        return Map.of("name", name, "url", "/api/files/" + name);
    }
}
