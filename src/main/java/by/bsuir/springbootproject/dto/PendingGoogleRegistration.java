package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingGoogleRegistration implements Serializable {
    private String googleSubject;
    private String email;
    private Boolean emailVerified;
    private String displayName;
    private String avatarUrl;
}