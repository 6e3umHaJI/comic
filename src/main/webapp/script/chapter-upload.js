(() => {
    const LIMITS = {
        maxPages: 200,
        maxFileSizeBytes: 1024 * 1024,
        languageSearch: 35,
        title: 255
    };

    const FILE_PATTERN = /^(\d{1,3})\.(jpg|webp)$/i;
    const ALLOWED_MIME_TYPES = ["image/jpeg", "image/webp"];

    const root = document.getElementById("chapterSubmissionPage");
    if (!root) {
        return;
    }

    const form = document.getElementById("chapterSubmissionForm");
    const languageSearch = document.getElementById("languageSearch");
    const languageSelect = document.getElementById("languageId");
    const chapterSelect = document.getElementById("chapterNumber");
    const titleInput = document.getElementById("title");
    const fileInput = document.getElementById("pageFiles");
    const filesList = document.getElementById("chapterSubmissionFilesList");
    const statusBox = document.getElementById("chapterSubmissionClientStatus");
    const submitButton = form?.querySelector('button[type="submit"]');
    const autoTranslateCheckbox = document.getElementById("autoTranslate");
    const sourceLanguageField = document.getElementById("sourceLanguageField");
    const sourceLanguageSelect = document.getElementById("sourceLanguageId");
    const selectedPageNumbersHolder = document.getElementById("selectedPageNumbersHolder");
    const loadingBox = document.getElementById("autoProcessingLoading");

    const originalLanguageOptions = languageSelect
        ? Array.from(languageSelect.options).map((option) => ({
            value: option.value,
            text: option.textContent,
            autoTranslateSupported: option.dataset.autoTranslateSupported === "true"
        }))
        : [];

    let renderedFiles = [];
    const selectedPages = new Set();

    function showStatus(message) {
        if (!statusBox) {
            return;
        }

        statusBox.textContent = String(message ?? "").trim();
        statusBox.classList.remove("hidden");
    }

    function hideStatus() {
        if (!statusBox) {
            return;
        }

        statusBox.textContent = "";
        statusBox.classList.add("hidden");
    }

    function trimAndLimit(value, maxLength) {
        return String(value ?? "").trim().slice(0, maxLength);
    }

    function limitOnly(value, maxLength) {
        return String(value ?? "").slice(0, maxLength);
    }

    function extractPageNumber(file) {
        const match = FILE_PATTERN.exec(file?.name || "");
        return match ? Number(match[1]) : null;
    }

    function getLanguageOptionsForCurrentMode() {
        if (!autoTranslateCheckbox?.checked) {
            return originalLanguageOptions;
        }

        return originalLanguageOptions.filter((item) => item.autoTranslateSupported);
    }

    function rebuildLanguageOptions() {
        if (!languageSelect) {
            return;
        }

        if (languageSearch) {
            languageSearch.value = limitOnly(languageSearch.value, LIMITS.languageSearch);
        }

        const query = String(languageSearch?.value || "").trim().toLowerCase();
        const currentValue = languageSelect.value;

        const options = getLanguageOptionsForCurrentMode().filter((item) => {
            return !query || item.text.toLowerCase().includes(query);
        });

        languageSelect.innerHTML = "";

        if (!options.length) {
            const option = document.createElement("option");
            option.value = "";
            option.textContent = "Ничего не найдено";
            option.disabled = true;
            option.selected = true;
            languageSelect.appendChild(option);
            return;
        }

        options.forEach((item) => {
            const option = document.createElement("option");
            option.value = item.value;
            option.textContent = item.text;
            option.dataset.autoTranslateSupported = String(item.autoTranslateSupported);

            if (item.value === currentValue) {
                option.selected = true;
            }

            languageSelect.appendChild(option);
        });

        if (!Array.from(languageSelect.options).some((option) => option.selected) && languageSelect.options.length > 0) {
            languageSelect.selectedIndex = 0;
        }

        ensureDifferentLanguages(true);
    }

    function ensureDifferentLanguages(silent) {
        if (!autoTranslateCheckbox?.checked || !languageSelect || !sourceLanguageSelect) {
            return true;
        }

        const targetLanguageId = String(languageSelect.value || "");
        const sourceLanguageId = String(sourceLanguageSelect.value || "");

        if (!targetLanguageId || !sourceLanguageId || targetLanguageId !== sourceLanguageId) {
            return true;
        }

        const fallback = Array.from(languageSelect.options).find((option) => {
            return option.value && option.value !== sourceLanguageId && !option.disabled;
        });

        if (fallback) {
            languageSelect.value = fallback.value;
            return true;
        }

        if (!silent) {
            showStatus("Язык исходного текста и язык перевода должны отличаться.");
        }

        return false;
    }

    function validateFiles(files, allowEmpty) {
        const actualFiles = Array.from(files || []).filter(Boolean);

        if (!actualFiles.length) {
            if (allowEmpty) {
                return { ok: true, files: [] };
            }
            return { ok: false, message: "Загрузите страницы перевода." };
        }

        if (actualFiles.length > LIMITS.maxPages) {
            return { ok: false, message: `Максимум можно загрузить ${LIMITS.maxPages} страниц.` };
        }

        for (const file of actualFiles) {
            if (file.size > LIMITS.maxFileSizeBytes) {
                return { ok: false, message: "Каждое изображение должно быть не больше 1 МБ." };
            }

            if (!FILE_PATTERN.test(file.name || "")) {
                return {
                    ok: false,
                    message: "Можно загружать только JPG и WEBP с именами вида 001.jpg, 002.jpg, 003.jpg или 001.webp, 002.webp, 003.webp."
                };
            }

            const mimeType = String(file.type || "").toLowerCase();
            if (!ALLOWED_MIME_TYPES.includes(mimeType)) {
                return { ok: false, message: "Можно загружать только файлы JPG и WEBP." };
            }
        }

        const sorted = actualFiles
            .map((file) => ({
                file,
                pageNumber: extractPageNumber(file)
            }))
            .sort((left, right) => left.pageNumber - right.pageNumber);

        for (let i = 0; i < sorted.length; i++) {
            if (sorted[i].pageNumber !== i + 1) {
                return {
                    ok: false,
                    message: "Файлы страниц должны идти подряд: 001.jpg, 002.jpg, 003.jpg и так далее."
                };
            }
        }

        return { ok: true, files: sorted };
    }

    function syncSelectedPagesWithRenderedFiles() {
        const availablePages = new Set(renderedFiles.map((entry) => entry.pageNumber));

        Array.from(selectedPages).forEach((pageNumber) => {
            if (!availablePages.has(pageNumber)) {
                selectedPages.delete(pageNumber);
            }
        });

        if (autoTranslateCheckbox?.checked && renderedFiles.length && selectedPages.size === 0) {
            renderedFiles.forEach((entry) => selectedPages.add(entry.pageNumber));
        }
    }

    function updateSelectedPageNumbersInputs() {
        if (!selectedPageNumbersHolder) {
            return;
        }

        selectedPageNumbersHolder.innerHTML = "";

        if (!autoTranslateCheckbox?.checked) {
            return;
        }

        Array.from(selectedPages)
            .sort((left, right) => left - right)
            .forEach((pageNumber) => {
                const input = document.createElement("input");
                input.type = "hidden";
                input.name = "selectedPageNumbers";
                input.value = String(pageNumber);
                selectedPageNumbersHolder.appendChild(input);
            });
    }

    function renderFilesList(showErrors) {
        if (!filesList || !fileInput) {
            return;
        }

        const hasFiles = Boolean(fileInput.files && fileInput.files.length > 0);

        if (!hasFiles) {
            renderedFiles = [];
            selectedPages.clear();
            updateSelectedPageNumbersInputs();
            filesList.textContent = "Файлы ещё не выбраны.";
            filesList.classList.add("chapter-upload-files-empty");
            return;
        }

        const validation = validateFiles(fileInput.files, false);
        if (!validation.ok) {
            renderedFiles = [];
            selectedPages.clear();
            updateSelectedPageNumbersInputs();
            filesList.textContent = validation.message;
            filesList.classList.add("chapter-upload-files-empty");

            if (showErrors) {
                showStatus(validation.message);
            }

            return;
        }

        renderedFiles = validation.files;
        syncSelectedPagesWithRenderedFiles();
        updateSelectedPageNumbersInputs();

        filesList.classList.remove("chapter-upload-files-empty");
        filesList.innerHTML = "";

        renderedFiles.forEach((entry) => {
            const row = document.createElement("div");
            row.className = "chapter-upload-file-entry" + (autoTranslateCheckbox?.checked ? " is-selectable" : "");

            const left = document.createElement("div");
            left.className = "chapter-upload-file-left";

            if (autoTranslateCheckbox?.checked) {
                const checkbox = document.createElement("input");
                checkbox.type = "checkbox";
                checkbox.className = "chapter-upload-file-checkbox";
                checkbox.checked = selectedPages.has(entry.pageNumber);
                checkbox.dataset.pageNumber = String(entry.pageNumber);
                checkbox.addEventListener("change", () => {
                    const pageNumber = Number(checkbox.dataset.pageNumber);
                    if (checkbox.checked) {
                        selectedPages.add(pageNumber);
                    } else {
                        selectedPages.delete(pageNumber);
                    }
                    updateSelectedPageNumbersInputs();
                });
                left.appendChild(checkbox);
            }

            const info = document.createElement("div");
            info.className = "chapter-upload-file-meta";

            const fileName = document.createElement("div");
            fileName.className = "chapter-upload-file-name";
            fileName.textContent = entry.file.name;

            const pageNumber = document.createElement("div");
            pageNumber.className = "chapter-upload-file-page";
            pageNumber.textContent = `Страница ${entry.pageNumber}`;

            info.append(fileName, pageNumber);
            left.appendChild(info);
            row.appendChild(left);
            filesList.appendChild(row);
        });
    }

    function syncAutoTranslateUi() {
        const enabled = Boolean(autoTranslateCheckbox?.checked);

        if (sourceLanguageField) {
            sourceLanguageField.classList.toggle("hidden", !enabled);
        }

        if (loadingBox) {
            loadingBox.classList.add("hidden");
        }

        rebuildLanguageOptions();
        renderFilesList(false);
        updateSelectedPageNumbersInputs();
    }

    function setSubmittingState(isSubmitting) {
        if (submitButton) {
            submitButton.disabled = isSubmitting;
            submitButton.textContent = isSubmitting
                ? (autoTranslateCheckbox?.checked ? "Обработка..." : "Сохранение...")
                : "Сохранить";
        }

        if (loadingBox) {
            loadingBox.classList.toggle("hidden", !(isSubmitting && autoTranslateCheckbox?.checked));
        }
    }

    async function loadChapterOptions() {
        if (!languageSelect || !chapterSelect || !languageSelect.value) {
            return;
        }

        try {
            const response = await fetch(
                `${root.dataset.optionsUrl}?languageId=${encodeURIComponent(languageSelect.value)}`,
                {
                    headers: {
                        "X-Requested-With": "XMLHttpRequest"
                    }
                }
            );

            if (!response.ok) {
                throw new Error("OPTIONS_LOAD_FAILED");
            }

            const data = await response.json();
            const values = Array.isArray(data.chapterNumbers) ? data.chapterNumbers : [];
            const previousValue = chapterSelect.value;

            chapterSelect.innerHTML = "";

            values.forEach((value) => {
                const option = document.createElement("option");
                option.value = String(value);
                option.textContent = `Глава ${value}`;
                if (String(value) === String(previousValue)) {
                    option.selected = true;
                }
                chapterSelect.appendChild(option);
            });

            if (!chapterSelect.value && data.defaultChapterNumber != null) {
                chapterSelect.value = String(data.defaultChapterNumber);
            }
        } catch (_) {
            showStatus("Не удалось загрузить доступные номера глав.");
        }
    }

    languageSearch?.addEventListener("input", async () => {
        rebuildLanguageOptions();
        await loadChapterOptions();
    });

    languageSelect?.addEventListener("change", async () => {
        hideStatus();

        if (autoTranslateCheckbox?.checked && !ensureDifferentLanguages(false)) {
            return;
        }

        await loadChapterOptions();
    });

    sourceLanguageSelect?.addEventListener("change", async () => {
        hideStatus();

        if (autoTranslateCheckbox?.checked && !ensureDifferentLanguages(false)) {
            return;
        }

        await loadChapterOptions();
    });

    chapterSelect?.addEventListener("change", hideStatus);

    titleInput?.addEventListener("input", () => {
        titleInput.value = limitOnly(titleInput.value, LIMITS.title);
    });

    titleInput?.addEventListener("blur", () => {
        titleInput.value = trimAndLimit(titleInput.value, LIMITS.title);
    });

    fileInput?.addEventListener("change", () => {
        hideStatus();
        renderFilesList(true);
    });

    autoTranslateCheckbox?.addEventListener("change", () => {
        hideStatus();
        syncAutoTranslateUi();
    });

    form?.addEventListener("submit", (event) => {
        hideStatus();

        if (titleInput) {
            titleInput.value = trimAndLimit(titleInput.value, LIMITS.title);
            if (!titleInput.value) {
                event.preventDefault();
                showStatus("Введите название перевода.");
                return;
            }
        }

        if (!languageSelect?.value) {
            event.preventDefault();
            showStatus("Выберите язык перевода.");
            return;
        }

        const validation = validateFiles(fileInput?.files, false);
        if (!validation.ok) {
            event.preventDefault();
            showStatus(validation.message);
            renderFilesList(true);
            return;
        }

        if (autoTranslateCheckbox?.checked) {
            const selectedOption = languageSelect.options[languageSelect.selectedIndex];
            if (!selectedOption || selectedOption.dataset.autoTranslateSupported !== "true") {
                event.preventDefault();
                showStatus("Для автоматического перевода можно выбрать только язык с заполненным кодом перевода.");
                return;
            }

            if (!sourceLanguageSelect?.value) {
                event.preventDefault();
                showStatus("Выберите язык исходного текста.");
                return;
            }

            if (!ensureDifferentLanguages(false)) {
                event.preventDefault();
                return;
            }

            if (selectedPages.size === 0) {
                event.preventDefault();
                showStatus("Отметьте хотя бы одну страницу для автоматического перевода.");
                return;
            }
        } else {
            selectedPages.clear();
        }

        updateSelectedPageNumbersInputs();
        setSubmittingState(true);
    });

    window.addEventListener("pageshow", () => {
        setSubmittingState(false);
    });

    rebuildLanguageOptions();
    renderFilesList(false);
    loadChapterOptions();
    syncAutoTranslateUi();
})();