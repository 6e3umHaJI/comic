package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.TranslationSubmissionForm;
import by.bsuir.springbootproject.entities.ApiMonthlyUsage;
import by.bsuir.springbootproject.entities.AutoTranslationPreview;
import by.bsuir.springbootproject.entities.AutoTranslationPreviewPage;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Language;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.ApiMonthlyUsageRepository;
import by.bsuir.springbootproject.repositories.AutoTranslationPreviewPageRepository;
import by.bsuir.springbootproject.repositories.AutoTranslationPreviewRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.LanguageRepository;
import by.bsuir.springbootproject.services.AutoTranslationService;
import by.bsuir.springbootproject.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.net.URI;
import java.nio.file.StandardCopyOption;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;


@Service
@RequiredArgsConstructor
@Transactional
public class AutoTranslationServiceImpl implements AutoTranslationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String OCR_PROVIDER = "OCR_SPACE";
    private static final String GOOGLE_TRANSLATE_PROVIDER = "GOOGLE_CLOUD_TRANSLATE";

    private static final int MAX_PAGE_COUNT = 200;
    private static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;
    private static final int MAX_FONT_SIZE = 42;
    private static final int MIN_FONT_SIZE = 9;
    private static final int TEXT_PADDING_X = 4;
    private static final int TEXT_PADDING_Y = 3;
    private static final int ERASE_PADDING_X = 2;
    private static final int ERASE_PADDING_Y = 2;
    private static final int SAMPLE_RING = 10;
    private static final int SMOOTH_ITERATIONS = 20;
    private static final int TEXT_STROKE_WIDTH = 2;
    private static final int PHRASE_BOX_PADDING_X = 4;
    private static final int PHRASE_BOX_PADDING_Y = 3;
    private static final double MERGE_IOU_THRESHOLD = 0.35;
    private static final int OCR_ENGINE = 2;

    private static final Pattern PAGE_FILE_PATTERN =
            Pattern.compile("^(\\d{1,3})\\.(jpg|webp)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SEPARATOR_PATTERN =
            Pattern.compile("^[\\p{Punct}«»“”‘’…0-9]+$");

    private static final Map<Character, Character> RU_CONFUSABLES = Map.ofEntries(
            Map.entry('A', 'А'),
            Map.entry('a', 'а'),
            Map.entry('B', 'В'),
            Map.entry('C', 'С'),
            Map.entry('c', 'с'),
            Map.entry('E', 'Е'),
            Map.entry('e', 'е'),
            Map.entry('H', 'Н'),
            Map.entry('K', 'К'),
            Map.entry('k', 'к'),
            Map.entry('M', 'М'),
            Map.entry('O', 'О'),
            Map.entry('o', 'о'),
            Map.entry('P', 'Р'),
            Map.entry('p', 'р'),
            Map.entry('T', 'Т'),
            Map.entry('X', 'Х'),
            Map.entry('x', 'х'),
            Map.entry('Y', 'У'),
            Map.entry('y', 'у')
    );

   private static final Path PAGES_STORAGE_DIR =
            Paths.get("src/main/webapp/assets/pages");

    private final ComicRepository comicRepository;
    private final LanguageRepository languageRepository;
    private final AutoTranslationPreviewRepository previewRepository;
    private final AutoTranslationPreviewPageRepository previewPageRepository;
    private final ApiMonthlyUsageRepository usageRepository;
    private final NotificationService notificationService;
    @Value("${ocrspace.api.key:}")
    private String ocrApiKey;

    @Value("${ocrspace.monthly-request-limit:25000}")
    private int ocrMonthlyRequestLimit;

    @Value("${gcp.translation.project-id:}")
    private String gcpTranslationProjectId;

    @Value("${gcp.translation.location:global}")
    private String gcpTranslationLocation;

    @Value("${gcp.translation.monthly-char-limit:500000}")
    private int gcpTranslationMonthlyCharLimit;

    @Value("${gcp.translation.estimated-chars-per-page:3500}")
    private int gcpTranslationEstimatedCharsPerPage;

    @Override
    public List<ComicPage> generateTranslationPages(Translation translation,
                                                    Integer sourceLanguageId,
                                                    User admin,
                                                    MultipartFile[] pageFiles,
                                                    List<Integer> selectedPageNumbers) {
        requireAdmin(admin);

        if (translation == null || translation.getId() == null) {
            throw new IllegalArgumentException("Перевод для автоматического перевода не найден.");
        }

        if (sourceLanguageId == null) {
            throw new IllegalArgumentException("Выберите язык исходного текста.");
        }

        Language sourceLanguage = languageRepository.findById(sourceLanguageId)
                .orElseThrow(() -> new IllegalArgumentException("Язык исходного текста не найден."));

        Language targetLanguage = translation.getLanguage();
        if (targetLanguage == null || targetLanguage.getId() == null) {
            throw new IllegalStateException("Язык перевода не найден.");
        }

        if (targetLanguage.getId().equals(sourceLanguage.getId())) {
            throw new IllegalArgumentException("Язык исходного текста и язык перевода должны отличаться.");
        }

        validateLanguageCodes(sourceLanguage, targetLanguage);

        List<PageFileCandidate> files = prepareFiles(pageFiles);
        Set<Integer> selectedPages = normalizeSelectedPages(selectedPageNumbers, files);

        QuotaSnapshot beforeUsage = getQuotaSnapshot();

        if (selectedPages.size() > beforeUsage.remainingOcrRequests()) {
            throw new IllegalStateException(
                    "В OCR.space осталось слишком мало запросов для этого запуска. Нужно страниц: "
                            + selectedPages.size()
                            + ", доступно: " + beforeUsage.remainingOcrRequests() + "."
            );
        }

        int estimatedChars = selectedPages.size() * Math.max(1, gcpTranslationEstimatedCharsPerPage);
        if (estimatedChars > beforeUsage.remainingMyMemoryChars()) {
            throw new IllegalStateException(
                    "На Google Cloud Translation, скорее всего, не хватит символов для перевода выбранных страниц. Нужно примерно: "
                            + estimatedChars
                            + ", доступно: " + beforeUsage.remainingMyMemoryChars() + "."
            );
        }

        try {
            Files.createDirectories(PAGES_STORAGE_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось подготовить директорию для автоматического перевода.");
        }

        List<String> createdFileNames = new ArrayList<>();
        List<ComicPage> resultPages = new ArrayList<>();
        int ocrRequestsUsed = 0;
        int translationCharsUsed = 0;

        try {
            for (PageFileCandidate candidate : files) {
                Path tempSource = Files.createTempFile(
                        "auto_src_" + translation.getId() + "_" + candidate.pageNumber() + "_",
                        ".jpg"
                );
                Path tempResult = Files.createTempFile(
                        "auto_result_" + translation.getId() + "_" + candidate.pageNumber() + "_",
                        ".jpg"
                );

                try {
                    writeMultipartAsJpg(candidate.file(), tempSource);

                    String finalFileName = buildAutoStoredFileName(translation.getId(), candidate.pageNumber());
                    Path finalPath = PAGES_STORAGE_DIR.resolve(finalFileName);
                    boolean selected = selectedPages.contains(candidate.pageNumber());

                    if (selected) {
                        TranslationResult translationResult = translatePage(
                                tempSource,
                                tempResult,
                                sourceLanguage,
                                targetLanguage,
                                beforeUsage.remainingMyMemoryChars() - translationCharsUsed
                        );

                        Files.copy(tempResult, finalPath, StandardCopyOption.REPLACE_EXISTING);
                        ocrRequestsUsed += 1;
                        translationCharsUsed += translationResult.sourceCharacters();
                    } else {
                        Files.copy(tempSource, finalPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    createdFileNames.add(finalFileName);
                    resultPages.add(
                            ComicPage.builder()
                                    .translation(translation)
                                    .pageNumber(candidate.pageNumber())
                                    .imagePath(finalFileName)
                                    .build()
                    );
                } finally {
                    Files.deleteIfExists(tempSource);
                    Files.deleteIfExists(tempResult);
                }
            }

            applyUsage(ocrRequestsUsed, translationCharsUsed);
            return resultPages;
        } catch (IOException e) {
            deleteFinalFiles(createdFileNames);
            throw new IllegalStateException("Не удалось сохранить изображения автоматического перевода.");
        } catch (RuntimeException e) {
            deleteFinalFiles(createdFileNames);
            throw e;
        }
    }

    private void writeMultipartAsJpg(MultipartFile file, Path targetPath) {
        try {
            BufferedImage sourceImage = ImageIO.read(file.getInputStream());
            if (sourceImage == null) {
                throw new IllegalStateException("Не удалось прочитать исходное изображение для автоматического перевода.");
            }

            BufferedImage rgbImage = new BufferedImage(
                    sourceImage.getWidth(),
                    sourceImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D graphics = rgbImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            graphics.drawImage(sourceImage, 0, 0, null);
            graphics.dispose();

            if (!ImageIO.write(rgbImage, "jpg", targetPath.toFile())) {
                throw new IllegalStateException("Не удалось сохранить временное JPG-изображение.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось подготовить JPG-файл для OCR.space.");
        }
    }

    private String buildAutoStoredFileName(Integer translationId, int pageNumber) {
        return "tr_%d_p%03d_%s.jpg".formatted(
                translationId,
                pageNumber,
                UUID.randomUUID().toString().replace("-", "")
        );
    }

    private TranslationResult translatePage(Path sourcePath,
                                            Path resultPath,
                                            Language sourceLanguage,
                                            Language targetLanguage,
                                            int remainingGoogleChars) {
        BufferedImage originalImage = readImage(sourcePath);

        List<OcrWord> ocrWords = runOcr(sourcePath, sourceLanguage);
        List<WordBox> filteredBoxes = filterBoxesForSourceLanguage(ocrWords, sourceLanguage);

        if (filteredBoxes.isEmpty()) {
            throw new IllegalStateException("На выбранной странице не удалось найти текст исходного языка для автоматического перевода.");
        }

        List<Rectangle> eraseRects = mergeRectangles(
                filteredBoxes.stream()
                        .map(this::toExpandedRectangle)
                        .toList()
        );

        BufferedImage erasedImage = eraseText(originalImage, eraseRects);
        List<PhraseBox> phrases = groupIntoPhrases(filteredBoxes);

        if (phrases.isEmpty()) {
            throw new IllegalStateException("На выбранной странице не удалось собрать реплики для перевода.");
        }

        int sourceCharsUsed = phrases.stream()
                .map(PhraseBox::translationText)
                .mapToInt(String::length)
                .sum();

        if (sourceCharsUsed > remainingGoogleChars) {
            throw new IllegalStateException(
                    "После OCR выяснилось, что на Google Cloud Translation не хватает лимита символов. "
                            + "Для текущих страниц доступно: " + remainingGoogleChars
                            + ", нужно: " + sourceCharsUsed + "."
            );
        }

        List<String> translatedTexts = translateWithGoogleCloud(
                phrases.stream().map(PhraseBox::translationText).toList(),
                sourceLanguage,
                targetLanguage
        );

        BufferedImage rendered = renderTranslatedTexts(erasedImage, phrases, translatedTexts);
        writeImage(rendered, resultPath);

        return new TranslationResult(sourceCharsUsed);
    }


    private List<OcrWord> runOcr(Path imagePath, Language sourceLanguage) {
        String apiKey = requireConfiguredOcrApiKey();
        RestTemplate restTemplate = new RestTemplate();

        try {
            byte[] fileBytes = Files.readAllBytes(imagePath);

            HttpHeaders headers = new HttpHeaders();
            headers.add("apikey", apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("language", stringValue(sourceLanguage.getOcrSpaceCode()).trim().toLowerCase(Locale.ROOT));
            body.add("isOverlayRequired", "true");
            body.add("OCREngine", String.valueOf(OCR_ENGINE));
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return imagePath.getFileName().toString();
                }
            });

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.ocr.space/parse/image",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> data = response.getBody();
            if (data == null) {
                throw new ExternalApiException("OCR.space не вернул ответ.");
            }

            if (Boolean.TRUE.equals(data.get("IsErroredOnProcessing"))) {
                throw new ExternalApiException("OCR.space вернул ошибку обработки изображения.");
            }

            List<Map<String, Object>> parsedResults = castList(data.get("ParsedResults"));
            List<OcrWord> result = new ArrayList<>();

            for (Map<String, Object> parsed : parsedResults) {
                Map<String, Object> overlay = castMap(parsed.get("TextOverlay"));
                List<Map<String, Object>> lines = castList(overlay.get("Lines"));

                for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                    Map<String, Object> line = lines.get(lineIndex);
                    List<Map<String, Object>> lineWords = castList(line.get("Words"));

                    for (int wordIndex = 0; wordIndex < lineWords.size(); wordIndex++) {
                        Map<String, Object> word = lineWords.get(wordIndex);

                        String rawText = stringValue(word.get("WordText"));
                        if (!StringUtils.hasText(rawText)) {
                            continue;
                        }

                        result.add(
                                new OcrWord(
                                        lineIndex,
                                        wordIndex,
                                        rawText,
                                        toInt(word.get("Left")),
                                        toInt(word.get("Top")),
                                        toInt(word.get("Width")),
                                        toInt(word.get("Height"))
                                )
                        );
                    }
                }
            }

            if (result.isEmpty()) {
                throw new ExternalApiException("OCR.space не нашёл текст на изображении.");
            }

            return result;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException("Не удалось получить ответ от OCR.space.");
        }
    }

    private List<String> translateWithGoogleCloud(List<String> texts,
                                                  Language sourceLanguage,
                                                  Language targetLanguage) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<String> normalizedTexts = texts.stream()
                .map(this::cleanPhraseText)
                .toList();

        List<Integer> indexesToTranslate = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<String> result = new ArrayList<>(Collections.nCopies(texts.size(), ""));

        for (int i = 0; i < normalizedTexts.size(); i++) {
            String normalized = normalizedTexts.get(i);
            if (StringUtils.hasText(normalized)) {
                indexesToTranslate.add(i);
                contents.add(normalized);
            }
        }

        if (contents.isEmpty()) {
            return result;
        }

        String projectId = requireConfiguredGoogleProjectId();
        String location = requireConfiguredGoogleLocation();
        String sourceCode = normalizeTranslationLanguageCode(sourceLanguage.getTranslationCode(), "исходного текста");
        String targetCode = normalizeTranslationLanguageCode(targetLanguage.getTranslationCode(), "перевода");
        String parent = LocationName.of(projectId, location).toString();

        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            TranslateTextRequest request = TranslateTextRequest.newBuilder()
                    .setParent(parent)
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(sourceCode)
                    .setTargetLanguageCode(targetCode)
                    .addAllContents(contents)
                    .build();

            TranslateTextResponse response = client.translateText(request);
            List<com.google.cloud.translate.v3.Translation> googleTranslations = response.getTranslationsList();

            if (googleTranslations.size() != contents.size()) {
                throw new ExternalApiException("Google Cloud Translation вернул неполный список переводов.");
            }

            for (int i = 0; i < googleTranslations.size(); i++) {
                String translated = stringValue(googleTranslations.get(i).getTranslatedText()).trim();

                if (!StringUtils.hasText(translated)) {
                    throw new ExternalApiException("Google Cloud Translation вернул пустой перевод.");
                }

                String finalTranslated = HtmlUtils.htmlUnescape(translated).trim();
                int originalIndex = indexesToTranslate.get(i);
                result.set(originalIndex, finalTranslated);
            }

            return result;
        } catch (ApiException e) {
            String errorMessage = StringUtils.hasText(e.getMessage())
                    ? e.getMessage()
                    : String.valueOf(e.getStatusCode());

            throw new ExternalApiException("Google Cloud Translation вернул ошибку: " + errorMessage);
        } catch (IOException e) {
            throw new ExternalApiException("Не удалось создать клиент Google Cloud Translation.");
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException("Не удалось получить ответ от Google Cloud Translation.");
        }
    }


    private String requireConfiguredGoogleProjectId() {
        if (!StringUtils.hasText(gcpTranslationProjectId)) {
            throw new IllegalStateException("Не задан gcp.translation.project-id для автоматического перевода.");
        }
        return gcpTranslationProjectId.trim();
    }

    private String requireConfiguredGoogleLocation() {
        String location = stringValue(gcpTranslationLocation).trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(location)) {
            return "global";
        }
        return location;
    }

    private String normalizeTranslationLanguageCode(String rawCode, String label) {
        String code = stringValue(rawCode).trim().toLowerCase(Locale.ROOT);

        if (!StringUtils.hasText(code)) {
            throw new IllegalStateException("Для языка " + label + " не настроен код перевода.");
        }

        return code;
    }

    private List<WordBox> filterBoxesForSourceLanguage(List<OcrWord> words, Language sourceLanguage) {
        List<WordBox> allBoxes = new ArrayList<>();

        for (OcrWord word : words) {
            String rawText = word.text().trim();
            String normalized = normalizeSourceToken(rawText, sourceLanguage);

            allBoxes.add(
                    new WordBox(
                            word.lineIndex(),
                            word.wordIndex(),
                            normalized,
                            rawText,
                            word.left(),
                            word.top(),
                            word.width(),
                            word.height(),
                            isSeparatorToken(normalized),
                            isTextInSourceLanguage(normalized, sourceLanguage)
                    )
            );
        }

        List<WordBox> sourceBoxes = allBoxes.stream()
                .filter(WordBox::sourceWord)
                .toList();

        if (sourceBoxes.isEmpty()) {
            return List.of();
        }

        List<WordBox> filtered = new ArrayList<>();
        for (WordBox box : allBoxes) {
            if (box.sourceWord()) {
                filtered.add(box);
                continue;
            }

            if (box.separator() && isSeparatorNearSource(box, sourceBoxes)) {
                filtered.add(box);
            }
        }

        filtered.sort(Comparator.comparingInt(WordBox::top).thenComparingInt(WordBox::left));
        return filtered;
    }

    private List<PhraseBox> groupIntoPhrases(List<WordBox> boxes) {
        if (boxes.isEmpty()) {
            return List.of();
        }

        List<LineGroup> rawLines = groupRawLines(boxes);
        List<Segment> segments = new ArrayList<>();

        for (int lineNo = 0; lineNo < rawLines.size(); lineNo++) {
            List<List<WordBox>> splitSegments = splitLineByAdaptiveGaps(rawLines.get(lineNo).items());
            for (List<WordBox> segmentItems : splitSegments) {
                if (!segmentItems.isEmpty()) {
                    segments.add(new Segment(lineNo, segmentItems));
                }
            }
        }

        if (segments.isEmpty()) {
            return List.of();
        }

        DisjointSet dsu = new DisjointSet(segments.size());

        for (int i = 0; i < segments.size(); i++) {
            Segment a = segments.get(i);

            for (int j = i + 1; j < segments.size(); j++) {
                Segment b = segments.get(j);

                if (a.lineNo() == b.lineNo()) {
                    continue;
                }

                double fontHeightA = localMedianHeight(a.items());
                double fontHeightB = localMedianHeight(b.items());

                if (!isFontSizeCompatibleForVerticalMerge(fontHeightA, fontHeightB)) {
                    continue;
                }

                double currentVerticalGap = verticalGap(a.rect(), b.rect());
                double maxAllowedVerticalGap = Math.min(fontHeightA, fontHeightB) * 1.6;

                if (currentVerticalGap > maxAllowedVerticalGap) {
                    continue;
                }

                double overlapRatio = xOverlapRatio(a.rect(), b.rect());
                double centerDistance = Math.abs(centerX(a.rect()) - centerX(b.rect()));
                double localFontHeight = (fontHeightA + fontHeightB) / 2.0;

                boolean horizontallyAligned =
                        overlapRatio >= 0.18
                                || centerDistance <= Math.max(
                                localFontHeight * 1.5,
                                Math.max(a.rect().width, b.rect().width) * 0.22
                        );

                if (horizontallyAligned) {
                    dsu.union(i, j);
                }
            }
        }

        Map<Integer, List<Segment>> grouped = new HashMap<>();
        for (int i = 0; i < segments.size(); i++) {
            grouped.computeIfAbsent(dsu.find(i), key -> new ArrayList<>()).add(segments.get(i));
        }

        List<PhraseBox> phrases = new ArrayList<>();

        for (List<Segment> group : grouped.values()) {
            group.sort(Comparator.comparingInt((Segment s) -> s.rect().y).thenComparingInt(s -> s.rect().x));

            List<String> lineTexts = new ArrayList<>();
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            int bottom = Integer.MIN_VALUE;

            for (Segment segment : group) {
                String text = joinTokensPreservingPunctuation(
                        segment.items().stream().map(WordBox::text).toList()
                );

                if (StringUtils.hasText(text)) {
                    lineTexts.add(text);
                }

                left = Math.min(left, segment.rect().x);
                top = Math.min(top, segment.rect().y);
                right = Math.max(right, segment.rect().x + segment.rect().width);
                bottom = Math.max(bottom, segment.rect().y + segment.rect().height);
            }

            String joined = String.join("\n", lineTexts);
            String translationText = cleanPhraseText(joined);

            if (!StringUtils.hasText(translationText)) {
                continue;
            }

            Rectangle phraseRect = new Rectangle(
                    Math.max(0, left - TEXT_PADDING_X),
                    Math.max(0, top - TEXT_PADDING_Y),
                    Math.max(1, (right - left) + TEXT_PADDING_X * 2),
                    Math.max(1, (bottom - top) + TEXT_PADDING_Y * 2)
            );

            phrases.add(new PhraseBox(phraseRect, translationText));
        }

        phrases.sort(Comparator.comparingInt((PhraseBox p) -> p.rect().y).thenComparingInt(p -> p.rect().x));
        return phrases;
    }

    private double localMedianHeight(List<WordBox> items) {
        if (items == null || items.isEmpty()) {
            return 18.0;
        }

        List<Integer> heights = items.stream()
                .map(WordBox::height)
                .sorted()
                .toList();

        return median(heights);
    }

    private boolean isFontSizeCompatibleForVerticalMerge(double fontHeightA, double fontHeightB) {
        double min = Math.max(1.0, Math.min(fontHeightA, fontHeightB));
        double max = Math.max(fontHeightA, fontHeightB);
        double ratio = max / min;

        return ratio <= 1.35;
    }


    private BufferedImage eraseText(BufferedImage source, List<Rectangle> eraseRects) {
        BufferedImage result = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        for (Rectangle rect : eraseRects) {
            Rectangle clipped = clipRectToImage(rect, result.getWidth(), result.getHeight());
            if (clipped.width <= 0 || clipped.height <= 0) {
                continue;
            }

            fillRectWithSmoothedBackground(result, clipped);
        }

        return result;
    }


    private BufferedImage renderTranslatedTexts(BufferedImage base,
                                                List<PhraseBox> phrases,
                                                List<String> translatedTexts) {
        BufferedImage result = new BufferedImage(
                base.getWidth(),
                base.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2 = result.createGraphics();
        g2.drawImage(base, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (int i = 0; i < phrases.size(); i++) {
            String translated = i < translatedTexts.size() ? translatedTexts.get(i) : "";
            if (!StringUtils.hasText(translated)) {
                continue;
            }

            Rectangle clipped = clipRectToImage(phrases.get(i).rect(), result.getWidth(), result.getHeight());
            if (clipped.width <= 2 || clipped.height <= 2) {
                continue;
            }

            drawTextIntoRect(g2, translated, clipped);
        }

        g2.dispose();
        return result;
    }

    private void drawTextIntoRect(Graphics2D graphics, String text, Rectangle rect) {
        String normalized = stringValue(text).trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return;
        }

        BufferedImage layer = new BufferedImage(
                Math.max(1, rect.width),
                Math.max(1, rect.height),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D boxGraphics = layer.createGraphics();
        boxGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boxGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boxGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        TextFit fit = fitTextToRect(boxGraphics, normalized, rect.width, rect.height);
        boxGraphics.setFont(fit.font());
        FontMetrics metrics = boxGraphics.getFontMetrics(fit.font());

        int lineHeight = metrics.getHeight();
        int totalHeight = lineHeight * fit.lines().size() + Math.max(0, fit.lines().size() - 1) * fit.spacing();
        int startY = ((rect.height - totalHeight) / 2) + metrics.getAscent();

        for (int i = 0; i < fit.lines().size(); i++) {
            String line = fit.lines().get(i);
            int lineWidth = metrics.stringWidth(line);
            int x = Math.max(TEXT_PADDING_X, (rect.width - lineWidth) / 2);
            int y = startY + i * (lineHeight + fit.spacing());

            boxGraphics.setColor(Color.WHITE);
            for (int dx = -TEXT_STROKE_WIDTH; dx <= TEXT_STROKE_WIDTH; dx++) {
                for (int dy = -TEXT_STROKE_WIDTH; dy <= TEXT_STROKE_WIDTH; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    if (dx * dx + dy * dy > TEXT_STROKE_WIDTH * TEXT_STROKE_WIDTH) {
                        continue;
                    }
                    boxGraphics.drawString(line, x + dx, y + dy);
                }
            }

            boxGraphics.setColor(Color.BLACK);
            boxGraphics.drawString(line, x, y);
        }

        boxGraphics.dispose();
        graphics.drawImage(layer, rect.x, rect.y, null);
    }


    private List<String> wrapText(Graphics2D graphics, String text, Font font, int maxWidth) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        List<String> result = new ArrayList<>();

        for (String rawLine : text.split("\\n")) {
            if (rawLine.isBlank()) {
                result.add("");
                continue;
            }

            String[] words = rawLine.split("\\s+");
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                if (!StringUtils.hasText(word)) {
                    continue;
                }

                List<String> pieces = (metrics.stringWidth(word) + TEXT_STROKE_WIDTH * 2 > maxWidth)
                        ? splitLongWord(graphics, word, font, maxWidth)
                        : List.of(word);

                for (String piece : pieces) {
                    String candidate = current.isEmpty() ? piece : current + " " + piece;

                    if (metrics.stringWidth(candidate) + TEXT_STROKE_WIDTH * 2 <= maxWidth) {
                        current = new StringBuilder(candidate);
                    } else {
                        if (!current.isEmpty()) {
                            result.add(current.toString());
                        }
                        current = new StringBuilder(piece);
                    }
                }
            }

            if (!current.isEmpty()) {
                result.add(current.toString());
            }
        }

        return result.isEmpty() ? List.of(text) : result;
    }

    private TextFit fitTextToRect(Graphics2D graphics, String text, int boxWidth, int boxHeight) {
        int usableWidth = Math.max(1, boxWidth - TEXT_PADDING_X * 2);
        int usableHeight = Math.max(1, boxHeight - TEXT_PADDING_Y * 2);

        for (int size = MAX_FONT_SIZE; size >= MIN_FONT_SIZE; size--) {
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
            List<String> lines = wrapText(graphics, text, font, usableWidth);
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics(font);

            int spacing = Math.max(1, Math.round(size * 0.15f));
            int maxLineWidth = 0;
            for (String line : lines) {
                maxLineWidth = Math.max(maxLineWidth, metrics.stringWidth(line));
            }

            int totalHeight = metrics.getHeight() * lines.size() + Math.max(0, lines.size() - 1) * spacing;
            int totalWidth = maxLineWidth + TEXT_STROKE_WIDTH * 2;

            if (totalWidth <= usableWidth && totalHeight <= usableHeight) {
                return new TextFit(font, lines, spacing);
            }
        }

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, MIN_FONT_SIZE);
        List<String> lines = wrapText(graphics, text, font, usableWidth);
        int spacing = Math.max(1, Math.round(MIN_FONT_SIZE * 0.15f));
        return new TextFit(font, lines, spacing);
    }

    private List<String> splitLongWord(Graphics2D graphics, String word, Font font, int maxWidth) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char ch : word.toCharArray()) {
            String candidate = current + String.valueOf(ch);

            if (metrics.stringWidth(candidate) + TEXT_STROKE_WIDTH * 2 <= maxWidth || current.isEmpty()) {
                current = new StringBuilder(candidate);
            } else {
                parts.add(current.toString());
                current = new StringBuilder(String.valueOf(ch));
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts;
    }

    private Rectangle clipRectToImage(Rectangle rect, int imageWidth, int imageHeight) {
        int x1 = Math.max(0, rect.x);
        int y1 = Math.max(0, rect.y);
        int x2 = Math.min(imageWidth, rect.x + rect.width);
        int y2 = Math.min(imageHeight, rect.y + rect.height);

        return new Rectangle(
                x1,
                y1,
                Math.max(0, x2 - x1),
                Math.max(0, y2 - y1)
        );
    }

    private void fillRectWithSmoothedBackground(BufferedImage image, Rectangle rect) {
        Color sampled = sampleMedianBackgroundColor(image, rect);
        int fillRgb = sampled.getRGB();

        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                image.setRGB(x, y, fillRgb);
            }
        }

        for (int iteration = 0; iteration < SMOOTH_ITERATIONS; iteration++) {
            int[][] next = new int[rect.height][rect.width];

            for (int y = rect.y; y < rect.y + rect.height; y++) {
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    int up = image.getRGB(x, Math.max(0, y - 1));
                    int down = image.getRGB(x, Math.min(image.getHeight() - 1, y + 1));
                    int left = image.getRGB(Math.max(0, x - 1), y);
                    int right = image.getRGB(Math.min(image.getWidth() - 1, x + 1), y);

                    next[y - rect.y][x - rect.x] = averageRgb(up, down, left, right);
                }
            }

            for (int y = rect.y; y < rect.y + rect.height; y++) {
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    image.setRGB(x, y, next[y - rect.y][x - rect.x]);
                }
            }
        }
    }

    private Color sampleMedianBackgroundColor(BufferedImage image, Rectangle rect) {
        int startX = Math.max(0, rect.x - SAMPLE_RING);
        int startY = Math.max(0, rect.y - SAMPLE_RING);
        int endX = Math.min(image.getWidth(), rect.x + rect.width + SAMPLE_RING);
        int endY = Math.min(image.getHeight(), rect.y + rect.height + SAMPLE_RING);

        List<Integer> reds = new ArrayList<>();
        List<Integer> greens = new ArrayList<>();
        List<Integer> blues = new ArrayList<>();

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                if (x >= rect.x && x < rect.x + rect.width && y >= rect.y && y < rect.y + rect.height) {
                    continue;
                }

                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                reds.add(color.getRed());
                greens.add(color.getGreen());
                blues.add(color.getBlue());
            }
        }

        if (reds.isEmpty()) {
            return Color.WHITE;
        }

        return new Color(
                (int) Math.round(median(reds)),
                (int) Math.round(median(greens)),
                (int) Math.round(median(blues))
        );
    }

    private int averageRgb(int... rgbs) {
        int red = 0;
        int green = 0;
        int blue = 0;

        for (int rgb : rgbs) {
            Color color = new Color(rgb);
            red += color.getRed();
            green += color.getGreen();
            blue += color.getBlue();
        }

        int count = Math.max(1, rgbs.length);
        return new Color(red / count, green / count, blue / count).getRGB();
    }

    private double intersectionOverUnion(Rectangle a, Rectangle b) {
        int x1 = Math.max(a.x, b.x);
        int y1 = Math.max(a.y, b.y);
        int x2 = Math.min(a.x + a.width, b.x + b.width);
        int y2 = Math.min(a.y + a.height, b.y + b.height);

        int intersectionWidth = Math.max(0, x2 - x1);
        int intersectionHeight = Math.max(0, y2 - y1);
        double intersection = (double) intersectionWidth * intersectionHeight;

        if (intersection <= 0.0) {
            return 0.0;
        }

        double union = (double) a.width * a.height + (double) b.width * b.height - intersection;
        return union <= 0.0 ? 0.0 : intersection / union;
    }

    private double medianHeight(List<WordBox> boxes) {
        List<Integer> heights = boxes.stream()
                .map(WordBox::height)
                .sorted()
                .toList();

        return median(heights);
    }

    private double percentile(List<Integer> values, double percentile) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);

        if (sorted.size() == 1) {
            return sorted.get(0);
        }

        double rank = (percentile / 100.0) * (sorted.size() - 1);
        int low = (int) Math.floor(rank);
        int high = (int) Math.ceil(rank);

        if (low == high) {
            return sorted.get(low);
        }

        double weight = rank - low;
        return sorted.get(low) * (1.0 - weight) + sorted.get(high) * weight;
    }

    private List<LineGroup> groupRawLines(List<WordBox> boxes) {
        Map<Integer, List<WordBox>> byLine = new HashMap<>();
        boolean hasLineIndexes = boxes.stream().anyMatch(box -> box.lineIndex() != null);

        if (hasLineIndexes) {
            for (WordBox box : boxes) {
                int lineKey = box.lineIndex() == null ? Integer.MAX_VALUE - box.top() : box.lineIndex();
                byLine.computeIfAbsent(lineKey, key -> new ArrayList<>()).add(box);
            }

            return byLine.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new LineGroup(
                            entry.getValue().stream()
                                    .sorted(Comparator.comparingInt(WordBox::left))
                                    .toList()
                    ))
                    .toList();
        }

        List<WordBox> sorted = new ArrayList<>(boxes);
        sorted.sort(Comparator.comparingInt(WordBox::top).thenComparingInt(WordBox::left));

        List<LineGroup> groups = new LinkedList<>();

        for (WordBox box : sorted) {
            boolean added = false;

            for (LineGroup group : groups) {
                double centerDiff = Math.abs(centerY(group.boundingRect()) - centerY(box.toRect()));
                double tolerance = Math.max(4.0, averageHeight(group.items()) * 0.6);

                if (centerDiff <= tolerance) {
                    group.items().add(box);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<WordBox> items = new ArrayList<>();
                items.add(box);
                groups.add(new LineGroup(items));
            }
        }

        for (LineGroup group : groups) {
            group.items().sort(Comparator.comparingInt(WordBox::left));
        }

        return groups;
    }

    private List<List<WordBox>> splitLineByAdaptiveGaps(List<WordBox> items) {
        List<WordBox> sorted = items.stream()
                .sorted(Comparator.comparingInt(WordBox::left))
                .toList();

        if (sorted.size() <= 1) {
            return List.of(sorted);
        }

        List<Integer> wordGaps = new ArrayList<>();
        List<Integer> allGaps = new ArrayList<>();

        for (int i = 0; i < sorted.size() - 1; i++) {
            WordBox left = sorted.get(i);
            WordBox right = sorted.get(i + 1);
            int gap = horizontalGap(left.toRect(), right.toRect());

            if (gap <= 0) {
                continue;
            }

            allGaps.add(gap);
            if (!left.separator() && !right.separator()) {
                wordGaps.add(gap);
            }
        }

        double medianHeight = averageHeight(sorted);
        double normalGap = median(wordGaps.isEmpty() ? allGaps : wordGaps);

        if (normalGap <= 0) {
            normalGap = medianHeight * 0.55;
        }

        double splitThreshold = Math.max(22.0, Math.max(medianHeight * 2.1, normalGap * 3.2));

        List<List<WordBox>> result = new ArrayList<>();
        List<WordBox> current = new ArrayList<>();
        current.add(sorted.get(0));

        for (int i = 0; i < sorted.size() - 1; i++) {
            WordBox left = sorted.get(i);
            WordBox right = sorted.get(i + 1);
            int gap = horizontalGap(left.toRect(), right.toRect());

            boolean splitHere = !left.separator() && !right.separator() && gap > splitThreshold;
            if (splitHere) {
                result.add(new ArrayList<>(current));
                current.clear();
            }

            current.add(right);
        }

        if (!current.isEmpty()) {
            result.add(current);
        }

        return result;
    }



    private Rectangle toExpandedRectangle(WordBox box) {
        return new Rectangle(
                Math.max(0, box.left() - ERASE_PADDING_X),
                Math.max(0, box.top() - ERASE_PADDING_Y),
                Math.max(1, box.width() + ERASE_PADDING_X * 2),
                Math.max(1, box.height() + ERASE_PADDING_Y * 2)
        );
    }

    private List<Rectangle> mergeRectangles(List<Rectangle> rects) {
        List<Rectangle> working = new ArrayList<>(rects);
        boolean merged = true;

        while (merged) {
            merged = false;
            List<Rectangle> next = new ArrayList<>();
            boolean[] used = new boolean[working.size()];

            for (int i = 0; i < working.size(); i++) {
                if (used[i]) {
                    continue;
                }

                Rectangle current = new Rectangle(working.get(i));
                used[i] = true;

                for (int j = i + 1; j < working.size(); j++) {
                    if (used[j]) {
                        continue;
                    }

                    Rectangle other = working.get(j);
                    double iou = intersectionOverUnion(current, other);

                    if (iou >= MERGE_IOU_THRESHOLD || isSameLineAndClose(current, other)) {
                        current = current.union(other);
                        used[j] = true;
                        merged = true;
                    }
                }

                next.add(current);
            }

            working = next;
        }

        return working;
    }


    private boolean isSameLineAndClose(Rectangle a, Rectangle b) {
        boolean sameLine = Math.abs(a.y - b.y) <= 6
                && Math.abs((a.y + a.height) - (b.y + b.height)) <= 6;
        int horizontalDistance = horizontalGap(a, b);
        return sameLine && horizontalDistance <= 8;
    }

    private void validateLanguageCodes(Language sourceLanguage, Language targetLanguage) {
        if (!StringUtils.hasText(stringValue(sourceLanguage.getOcrSpaceCode()).trim())) {
            throw new IllegalStateException("Для языка исходного текста не настроен код OCR.space.");
        }

        if (!StringUtils.hasText(stringValue(sourceLanguage.getTranslationCode()).trim())) {
            throw new IllegalStateException("Для языка исходного текста не настроен код перевода.");
        }

        if (!StringUtils.hasText(stringValue(targetLanguage.getTranslationCode()).trim())) {
            throw new IllegalStateException("Для языка перевода не настроен код перевода.");
        }
    }


    private Set<Integer> normalizeSelectedPages(List<Integer> selectedPageNumbers,
                                                List<PageFileCandidate> files) {
        Set<Integer> available = files.stream()
                .map(PageFileCandidate::pageNumber)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Integer> selected = selectedPageNumbers == null || selectedPageNumbers.isEmpty()
                ? new LinkedHashSet<>(available)
                : selectedPageNumbers.stream()
                  .filter(available::contains)
                  .collect(Collectors.toCollection(LinkedHashSet::new));

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Отметьте хотя бы одну страницу для автоматического перевода.");
        }

        return selected;
    }

    private List<PageFileCandidate> prepareFiles(MultipartFile[] pageFiles) {
        if (pageFiles == null) {
            throw new IllegalArgumentException("Загрузите страницы перевода.");
        }

        List<MultipartFile> actualFiles = List.of(pageFiles).stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (actualFiles.isEmpty()) {
            throw new IllegalArgumentException("Загрузите страницы перевода.");
        }

        if (actualFiles.size() > MAX_PAGE_COUNT) {
            throw new IllegalArgumentException("Максимум можно загрузить 200 страниц.");
        }

        List<PageFileCandidate> result = new ArrayList<>();

        for (MultipartFile file : actualFiles) {
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("Каждое изображение должно быть не больше 1 МБ.");
            }

            String originalName = StringUtils.getFilename(file.getOriginalFilename());
            if (!StringUtils.hasText(originalName)) {
                throw new IllegalArgumentException("У всех файлов должны быть корректные имена.");
            }

            Matcher matcher = PAGE_FILE_PATTERN.matcher(originalName);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Можно загружать только JPG и WEBP с именами вида 001.jpg, 002.jpg, 003.jpg или 001.webp, 002.webp, 003.webp.");
            }

            String contentType = stringValue(file.getContentType()).toLowerCase(Locale.ROOT);
            if (!"image/jpeg".equals(contentType) && !"image/webp".equals(contentType)) {
                throw new IllegalArgumentException("Можно загружать только файлы JPG и WEBP.");
            }

            result.add(new PageFileCandidate(Integer.parseInt(matcher.group(1)), file));
        }

        result.sort(Comparator.comparingInt(PageFileCandidate::pageNumber));

        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).pageNumber() != i + 1) {
                throw new IllegalArgumentException("Файлы страниц должны идти подряд: 001.jpg, 002.jpg, 003.jpg и так далее.");
            }
        }

        return result;
    }

    private String normalizeSourceToken(String text, Language sourceLanguage) {
        String normalized = stringValue(text).trim();

        if (!"ru".equalsIgnoreCase(sourceLanguage.getTranslationCode())) {
            return normalized;
        }

        if (!normalized.matches(".*[A-Za-z].*") || normalized.matches(".*[А-Яа-яЁё].*")) {
            return normalized;
        }

        StringBuilder fixed = new StringBuilder();
        boolean changed = false;

        for (char ch : normalized.toCharArray()) {
            Character replacement = RU_CONFUSABLES.get(ch);

            if (replacement != null) {
                fixed.append(replacement);
                changed = true;
                continue;
            }

            if (Character.isLetter(ch)) {
                return normalized;
            }

            fixed.append(ch);
        }

        String result = fixed.toString();
        return changed && result.matches(".*[А-Яа-яЁё].*") ? result : normalized;
    }

    private boolean isSeparatorToken(String text) {
        return StringUtils.hasText(text) && SEPARATOR_PATTERN.matcher(text.trim()).matches();
    }

    private boolean isSeparatorNearSource(WordBox candidate, List<WordBox> sourceBoxes) {
        for (WordBox source : sourceBoxes) {
            if (!sameLineOrClose(candidate, source)) {
                continue;
            }

            int gap = horizontalGap(candidate.toRect(), source.toRect());
            if (gap <= Math.max(8, candidate.height())) {
                return true;
            }
        }

        return false;
    }

    private boolean sameLineOrClose(WordBox left, WordBox right) {
        if (left.lineIndex() != null && right.lineIndex() != null) {
            return left.lineIndex().equals(right.lineIndex());
        }

        return Math.abs(centerY(left.toRect()) - centerY(right.toRect()))
                <= Math.max(left.height(), right.height()) * 0.75;
    }

    private boolean isTextInSourceLanguage(String text, Language language) {
        String value = stringValue(text).trim();
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String code = stringValue(language.getTranslationCode()).toLowerCase(Locale.ROOT);

        return switch (code) {
            case "ru" -> value.matches(".*[А-Яа-яЁё].*");
            case "en" -> value.matches(".*[A-Za-z].*") && !value.matches(".*[À-ÖØ-öø-ÿ].*");
            case "fr" -> value.matches(".*[A-Za-zÀ-ÖØ-öø-ÿŒœÆæÇç].*");
            case "de" -> value.matches(".*[A-Za-zÄäÖöÜüẞß].*");
            case "es" -> value.matches(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*");
            case "it" -> value.matches(".*[A-Za-zÀÈÉÌÍÎÒÓÙÚàèéìíîòóùú].*");
            case "pt" -> value.matches(".*[A-Za-zÁÂÃÀÇÉÊÍÓÔÕÚÜáâãàçéêíóôõúü].*");
            default -> false;
        };
    }

    private String cleanPhraseText(String value) {
        String text = stringValue(value).replace("\r", "");
        text = text.replaceAll("([\\p{L}]{2,})\\s*[-‐-‒–—]\\s*\\n\\s*([\\p{L}]{2,})", "$1$2");
        text = text.replaceAll("\\s*\\n\\s*", " ");
        text = text.replaceAll("\\s+", " ").trim();
        text = text.replaceAll("\\s+([,.!?;:%])", "$1");
        text = text.replaceAll("([\\(\\[\\{«])\\s+", "$1");
        text = text.replaceAll("\\s+([)\\]}»])", "$1");
        return text.trim();
    }

    private String joinTokensPreservingPunctuation(List<String> tokens) {
        StringBuilder builder = new StringBuilder();
        Set<String> noSpaceBefore = Set.of(".", ",", "!", "?", ";", ":", "%", ")", "]", "}", "»", "…");
        Set<String> noSpaceAfter = Set.of("(", "[", "{", "«");

        for (String raw : tokens) {
            String token = stringValue(raw).trim();

            if (!StringUtils.hasText(token)) {
                continue;
            }

            if (builder.isEmpty()) {
                builder.append(token);
                continue;
            }

            String previous = builder.substring(builder.length() - 1);

            if (noSpaceBefore.contains(token)
                    || noSpaceAfter.contains(previous)
                    || "/".equals(token)
                    || "/".equals(previous)) {
                builder.append(token);
            } else {
                builder.append(' ').append(token);
            }
        }

        return builder.toString();
    }

    private void applyUsage(int ocrRequestsUsed, int translationCharsUsed) {
        YearMonth month = YearMonth.now();

        if (ocrRequestsUsed > 0) {
            ApiMonthlyUsage usage = getOrCreateUsage(OCR_PROVIDER, month);
            usage.setRequestsUsed(usage.getRequestsUsed() + ocrRequestsUsed);
            usage.setUpdatedAt(LocalDateTime.now());
            usageRepository.save(usage);
        }

        if (translationCharsUsed > 0) {
            ApiMonthlyUsage usage = getOrCreateUsage(GOOGLE_TRANSLATE_PROVIDER, month);
            usage.setCharsUsed(usage.getCharsUsed() + translationCharsUsed);
            usage.setUpdatedAt(LocalDateTime.now());
            usageRepository.save(usage);
        }
    }

    private QuotaSnapshot getQuotaSnapshot() {
        YearMonth month = YearMonth.now();
        ApiMonthlyUsage ocr = getOrCreateUsage(OCR_PROVIDER, month);
        ApiMonthlyUsage googleTranslateUsage = getOrCreateUsage(GOOGLE_TRANSLATE_PROVIDER, month);

        return new QuotaSnapshot(
                Math.max(0, ocr.getRequestLimit() - ocr.getRequestsUsed()),
                Math.max(0, googleTranslateUsage.getCharLimit() - googleTranslateUsage.getCharsUsed())
        );
    }

    private ApiMonthlyUsage getOrCreateUsage(String provider, YearMonth month) {
        String monthKey = month.toString();

        return usageRepository.findByProviderAndMonthKey(provider, monthKey)
                .orElseGet(() -> usageRepository.save(
                        ApiMonthlyUsage.builder()
                                .provider(provider)
                                .monthKey(monthKey)
                                .requestLimit(OCR_PROVIDER.equals(provider)
                                        ? Math.max(0, ocrMonthlyRequestLimit)
                                        : 0)
                                .requestsUsed(0)
                                .charLimit(GOOGLE_TRANSLATE_PROVIDER.equals(provider)
                                        ? Math.max(0, gcpTranslationMonthlyCharLimit)
                                        : 0)
                                .charsUsed(0)
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }

    private void deleteFinalFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            if (!StringUtils.hasText(fileName)) {
                continue;
            }

            try {
                Files.deleteIfExists(PAGES_STORAGE_DIR.resolve(fileName));
            } catch (IOException ignored) {
            }
        }
    }

    private String buildStoredFileName(Integer translationId, int pageNumber, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String normalizedExtension = StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ROOT) : "jpg";

        return "tr_%d_p%03d_%s.%s".formatted(
                translationId,
                pageNumber,
                UUID.randomUUID().toString().replace("-", ""),
                normalizedExtension
        );
    }

    private BufferedImage readImage(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IllegalStateException("Не удалось прочитать изображение для автоматического перевода.");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать изображение для автоматического перевода.");
        }
    }

    private void writeImage(BufferedImage image, Path path) {
        String extension = getFileExtension(path.getFileName().toString());

        try {
            ImageIO.write(image, extension, path.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить результат автоматического перевода.");
        }
    }

    private String requireConfiguredOcrApiKey() {
        if (!StringUtils.hasText(ocrApiKey)) {
            throw new IllegalStateException("Не задан ocrspace.api.key для автоматического перевода.");
        }

        return ocrApiKey.trim();
    }

    private void requireAdmin(User admin) {
        if (admin == null || admin.getRole() == null || !ADMIN_ROLE.equalsIgnoreCase(admin.getRole().getName())) {
            throw new IllegalStateException("Недостаточно прав.");
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return (int) Math.round(number.doubleValue());
        }

        return (int) Math.round(Double.parseDouble(String.valueOf(value)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String getFileExtension(String filename) {
        String extension = org.springframework.util.StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private double averageHeight(List<WordBox> boxes) {
        return boxes.stream().mapToInt(WordBox::height).average().orElse(18.0);
    }

    private double median(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);

        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        }

        return sorted.get(middle);
    }

    private int horizontalGap(Rectangle left, Rectangle right) {
        if (left.x + left.width < right.x) {
            return right.x - (left.x + left.width);
        }

        if (right.x + right.width < left.x) {
            return left.x - (right.x + right.width);
        }

        return 0;
    }

    private double verticalGap(Rectangle first, Rectangle second) {
        int firstBottom = first.y + first.height;
        int secondTop = second.y;

        if (firstBottom < secondTop) {
            return secondTop - firstBottom;
        }

        int secondBottom = second.y + second.height;
        int firstTop = first.y;

        if (secondBottom < firstTop) {
            return firstTop - secondBottom;
        }

        return 0.0;
    }

    private double xOverlapRatio(Rectangle a, Rectangle b) {
        int overlap = Math.max(0, Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x));
        int minWidth = Math.max(1, Math.min(a.width, b.width));
        return overlap / (double) minWidth;
    }

    private double centerX(Rectangle rect) {
        return rect.x + rect.width / 2.0;
    }

    private double centerY(Rectangle rect) {
        return rect.y + rect.height / 2.0;
    }

    private record PageFileCandidate(int pageNumber, MultipartFile file) {
    }

    private record OcrWord(Integer lineIndex,
                           Integer wordIndex,
                           String text,
                           int left,
                           int top,
                           int width,
                           int height) {
    }

    private record WordBox(Integer lineIndex,
                           Integer wordIndex,
                           String text,
                           String rawText,
                           int left,
                           int top,
                           int width,
                           int height,
                           boolean separator,
                           boolean sourceWord) {
        private Rectangle toRect() {
            return new Rectangle(left, top, width, height);
        }
    }

    private record PhraseBox(Rectangle rect, String translationText) {
    }

    private record TranslationResult(int sourceCharacters) {
    }

    private record TextFit(Font font, List<String> lines, int spacing) {
    }

    private record QuotaSnapshot(int remainingOcrRequests, int remainingMyMemoryChars) {
    }

    private static class LineGroup {
        private final List<WordBox> items;

        private LineGroup(List<WordBox> items) {
            this.items = items;
        }

        public List<WordBox> items() {
            return items;
        }

        public Rectangle boundingRect() {
            int left = items.stream().mapToInt(WordBox::left).min().orElse(0);
            int top = items.stream().mapToInt(WordBox::top).min().orElse(0);
            int right = items.stream().mapToInt(box -> box.left() + box.width()).max().orElse(1);
            int bottom = items.stream().mapToInt(box -> box.top() + box.height()).max().orElse(1);
            return new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
        }
    }

    private record Segment(int lineNo, List<WordBox> items) {
        private Rectangle rect() {
            int left = items.stream().mapToInt(WordBox::left).min().orElse(0);
            int top = items.stream().mapToInt(WordBox::top).min().orElse(0);
            int right = items.stream().mapToInt(box -> box.left() + box.width()).max().orElse(1);
            int bottom = items.stream().mapToInt(box -> box.top() + box.height()).max().orElse(1);
            return new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
        }
    }

    private static class DisjointSet {
        private final int[] parent;

        private DisjointSet(int size) {
            this.parent = new int[size];
            for (int i = 0; i < size; i++) {
                this.parent[i] = i;
            }
        }

        private int find(int value) {
            if (parent[value] == value) {
                return value;
            }

            parent[value] = find(parent[value]);
            return parent[value];
        }

        private void union(int left, int right) {
            int rootLeft = find(left);
            int rootRight = find(right);

            if (rootLeft != rootRight) {
                parent[rootRight] = rootLeft;
            }
        }
    }

    private static class ExternalApiException extends RuntimeException {
        private ExternalApiException(String message) {
            super(message);
        }
    }
}