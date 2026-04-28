package by.bsuir.springbootproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private Path root = Path.of("/mnt/uploads");
    private String coversDir = "covers";
    private String pagesDir = "pages";

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public String getCoversDir() {
        return coversDir;
    }

    public void setCoversDir(String coversDir) {
        this.coversDir = coversDir;
    }

    public String getPagesDir() {
        return pagesDir;
    }

    public void setPagesDir(String pagesDir) {
        this.pagesDir = pagesDir;
    }

    public Path coversPath() {
        return root.resolve(coversDir).normalize();
    }

    public Path pagesPath() {
        return root.resolve(pagesDir).normalize();
    }
}
