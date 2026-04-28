package by.bsuir.springbootproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UploadProperties.class)
public class UploadWebConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/covers/**")
                .addResourceLocations(uploadProperties.coversPath().toUri().toString());

        registry.addResourceHandler("/assets/pages/**")
                .addResourceLocations(uploadProperties.pagesPath().toUri().toString());
    }
}
