package by.bsuir.springbootproject.services;

import jakarta.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface UploadStorageService {

    @PostConstruct
    void init();

    Path coversDirectory();

    Path pagesDirectory();

    String storeCover(MultipartFile file, String fileName) throws IOException;

    void storePage(MultipartFile file, String fileName) throws IOException;

    void copyPage(Path sourcePath, String fileName) throws IOException;

    void deleteCoverIfExists(String fileName);

    void deletePageIfExists(String fileName);

    Path resolveCover(String fileName);

    Path resolvePage(String fileName);
}
