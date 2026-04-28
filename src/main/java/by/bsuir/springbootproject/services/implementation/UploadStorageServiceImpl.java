package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.config.UploadProperties;
import by.bsuir.springbootproject.services.UploadStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class UploadStorageServiceImpl implements UploadStorageService {

    private final UploadProperties uploadProperties;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(coversDirectory());
            Files.createDirectories(pagesDirectory());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось подготовить общее хранилище загрузок.", e);
        }
    }

    public Path coversDirectory() {
        return uploadProperties.coversPath().toAbsolutePath().normalize();
    }

    public Path pagesDirectory() {
        return uploadProperties.pagesPath().toAbsolutePath().normalize();
    }

    public String storeCover(MultipartFile file, String fileName) throws IOException {
        Files.createDirectories(coversDirectory());
        Path target = resolveCover(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public void storePage(MultipartFile file, String fileName) throws IOException {
        Files.createDirectories(pagesDirectory());
        Path target = resolvePage(fileName);
        file.transferTo(target);
    }

    public void copyPage(Path sourcePath, String fileName) throws IOException {
        Files.createDirectories(pagesDirectory());
        Files.copy(sourcePath, resolvePage(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteCoverIfExists(String fileName) {
        deleteIfExists(resolveCoverOrNull(fileName));
    }

    public void deletePageIfExists(String fileName) {
        deleteIfExists(resolvePageOrNull(fileName));
    }

    public Path resolveCover(String fileName) {
        return resolveSafe(coversDirectory(), fileName);
    }

    public Path resolvePage(String fileName) {
        return resolveSafe(pagesDirectory(), fileName);
    }

    private Path resolveCoverOrNull(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        return resolveCover(fileName);
    }

    private Path resolvePageOrNull(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        return resolvePage(fileName);
    }

    private Path resolveSafe(Path directory, String fileName) {
        String cleanName = StringUtils.cleanPath(fileName == null ? "" : fileName.trim());

        if (!StringUtils.hasText(cleanName)
                || cleanName.contains("/")
                || cleanName.contains("\\")
                || cleanName.contains("..")) {
            throw new IllegalArgumentException("Некорректное имя файла загрузки.");
        }

        Path target = directory.resolve(cleanName).normalize();

        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("Некорректный путь файла загрузки.");
        }

        return target;
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
