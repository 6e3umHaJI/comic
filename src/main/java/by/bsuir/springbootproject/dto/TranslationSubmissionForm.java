package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationSubmissionForm {

    private Integer comicId;
    private Integer chapterNumber;
    private Integer languageId;
    private Integer sourceLanguageId;
    private String title;
    private Boolean autoTranslate = false;
    private String autoTranslationPreviewToken;
}