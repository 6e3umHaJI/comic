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

    const autoTranslate = document.getElementById("autoTranslate");
    const autoTranslateSettings = document.getElementById("autoTranslateSettings");
    const sourceLanguageSelect = document.getElementById("sourceLanguageId");
    const buildAutoPreviewBtn = document.getElementById("buildAutoPreviewBtn");
    const autoPreviewTokenInput = document.getElementById("autoTranslationPreviewToken");
    const autoPreviewSection = document.getElementById("autoPreviewSection");
    const autoPreviewGallery = document.getElementById("autoPreviewGallery");
    const autoPreviewLoading = document.getElementById("autoPreviewLoading");
    const autoPreviewQuotaInfo = document.getElementById("autoPreviewQuotaInfo");

    const originalLanguageOptions = languageSelect
        ? Array.from(languageSelect.options).map((option) => ({
            value: option.value,
            text: option.textContent,
            autoTranslateSupported: option.dataset.autoTranslateSupported === "true"
        }))
        : [];

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
        const match = FILE_PATTERN.exec(file.name || "");
        return match ? Number(match[1]) : null;
    }

    function rebuildLanguageOptions() {
        if (!languageSearch || !languageSelect) {
            return;
        }

        languageSearch.value = limitOnly(languageSearch.value, LIMITS.languageSearch);

        const query = languageSearch.value.trim().toLowerCase();
        const selectedValue = languageSelect.value;

        const filtered = !query
            ? originalLanguageOptions
            : originalLanguageOptions.filter((item) => item.text.toLowerCase().includes(query));

        languageSelect.innerHTML = "";

        if (!filtered.length) {
            const option = document.createElement("option");
            option.value = "";
            option.textContent = "Ничего не найдено";
            option.disabled = true;
            option.selected = true;
            languageSelect.appendChild(option);
            return;
        }

        filtered.forEach((item) => {
            const option = document.createElement("option");
            option.value = item.value;
            option.textContent = item.text;
            if (item.value === selectedValue) {
                option.selected = true;
            }
            languageSelect.appendChild(option);
        });

        if (!Array.from(languageSelect.options).some((option) => option.selected)) {
            languageSelect.selectedIndex = 0;
        }
    }

    function syncAutoTranslateTargetOptions() {
        if (!languageSelect) {
            return;
        }

        const currentValue = languageSelect.value;
        const shouldFilter = Boolean(autoTranslate?.checked);

        const filtered = shouldFilter
            ? originalLanguageOptions.filter((item) => item.autoTranslateSupported)
            : originalLanguageOptions;

        languageSelect.innerHTML = "";

        filtered.forEach((item) => {
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
    }


    function validateFiles(files) {
        const actualFiles = Array.from(files || []).filter(Boolean);

        if (!actualFiles.length) {
            return { ok: false, message: "Выберите страницы перевода." };
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
            .map((file) => ({ file, pageNumber: extractPageNumber(file) }))
            .sort((left, right) => left.pageNumber - right.pageNumber);

        for (let i = 0; i < sorted.length; i++) {
            if (sorted[i].pageNumber !== i + 1) {
                return { ok: false, message: "Файлы страниц должны идти подряд: 001.jpg, 002.jpg, 003.jpg и так далее." };
            }
        }

        return { ok: true, files: sorted };
    }

    function clearAutoPreviewState() {
        if (autoPreviewTokenInput) {
            autoPreviewTokenInput.value = "";
        }

        if (autoPreviewGallery) {
            autoPreviewGallery.innerHTML = "";
        }

        if (autoPreviewSection) {
            autoPreviewSection.classList.add("hidden");
        }
    }

    function syncAutoTranslateUi() {
        if (!autoTranslate || !autoTranslateSettings) {
            return;
        }

        const enabled = autoTranslate.checked;
        autoTranslateSettings.classList.toggle("hidden", !enabled);

        syncAutoTranslateTargetOptions();

        document.querySelectorAll(".chapter-upload-page-check").forEach((checkbox) => {
            checkbox.disabled = !enabled;
        });

        if (!enabled) {
            clearAutoPreviewState();
        }
    }

    function renderFilesList() {
        if (!filesList || !fileInput) {
            return;
        }

        const validation = validateFiles(fileInput.files);
        if (!validation.ok) {
            filesList.textContent = validation.message;
            filesList.classList.add("chapter-upload-files-empty");
            showStatus(validation.message);
            clearAutoPreviewState();
            return;
        }

        hideStatus();
        clearAutoPreviewState();
        filesList.classList.remove("chapter-upload-files-empty");
        filesList.innerHTML = "";

        validation.files.forEach((entry) => {
            const row = document.createElement("div");
            row.className = "chapter-upload-file-entry";

            const left = document.createElement("div");
            left.className = "chapter-upload-file-left";

            if (autoTranslate) {
                const checkbox = document.createElement("input");
                checkbox.type = "checkbox";
                checkbox.className = "chapter-upload-page-check";
                checkbox.dataset.pageNumber = String(entry.pageNumber);
                checkbox.checked = autoTranslate.checked;
                checkbox.disabled = !autoTranslate.checked;
                checkbox.addEventListener("change", clearAutoPreviewState);
                left.appendChild(checkbox);
            }

            const labelWrap = document.createElement("div");

            const name = document.createElement("div");
            name.className = "chapter-upload-file-name";
            name.textContent = entry.file.name;

            const page = document.createElement("div");
            page.className = "chapter-upload-file-page";
            page.textContent = `Страница ${entry.pageNumber}`;

            labelWrap.append(name, page);
            left.appendChild(labelWrap);

            row.appendChild(left);
            filesList.appendChild(row);
        });
    }

    function getSelectedPageNumbers() {
        if (!autoTranslate || !autoTranslate.checked) {
            return [];
        }

        return Array.from(document.querySelectorAll(".chapter-upload-page-check:checked"))
            .map((input) => Number(input.dataset.pageNumber))
            .filter((value) => Number.isFinite(value))
            .sort((left, right) => left - right);
    }

    function renderPreview(payload) {
        if (!autoPreviewGallery || !autoPreviewSection) {
            return;
        }

        autoPreviewGallery.innerHTML = "";

        const pages = Array.isArray(payload.pages) ? payload.pages : [];
        pages.forEach((page) => {
            const card = document.createElement("div");
            card.className = "chapter-auto-preview-card";

            const badge = document.createElement("div");
            badge.className = "chapter-auto-preview-badge" + (page.translated ? " is-translated" : "");
            badge.textContent = page.translated
                ? `Страница ${page.pageNumber} · переведена`
                : `Страница ${page.pageNumber} · оставлена как есть`;

            const image = document.createElement("img");
            image.src = page.previewUrl;
            image.alt = `Предпросмотр страницы ${page.pageNumber}`;

            card.append(badge, image);
            autoPreviewGallery.appendChild(card);
        });

        autoPreviewSection.classList.remove("hidden");

        if (autoPreviewQuotaInfo) {
            autoPreviewQuotaInfo.textContent =
                `Осталось OCR.space: ${payload.remainingOcrRequests ?? "—"} · Осталось MyMemory: ${payload.remainingMyMemoryChars ?? "—"} символов.`;
        }
    }

    function setPreviewLoading(isLoading) {
        if (autoPreviewLoading) {
            autoPreviewLoading.classList.toggle("hidden", !isLoading);
        }

        if (buildAutoPreviewBtn) {
            buildAutoPreviewBtn.disabled = isLoading;
            buildAutoPreviewBtn.textContent = isLoading ? "Подождите…" : "Построить предпросмотр";
        }

        if (submitButton) {
            submitButton.disabled = isLoading;
        }
    }

    function setSubmittingState(isSubmitting) {
        if (!submitButton) {
            return;
        }

        submitButton.disabled = isSubmitting;
        submitButton.textContent = isSubmitting ? "Сохранение..." : "Сохранить";
    }

    function syncChapterOptionsFromServerHtml(html) {
        if (!chapterSelect || !html) {
            return;
        }

        const doc = new DOMParser().parseFromString(html, "text/html");
        const serverChapterSelect = doc.getElementById("chapterNumber");
        if (!serverChapterSelect) {
            return;
        }

        chapterSelect.innerHTML = serverChapterSelect.innerHTML;
        chapterSelect.value = serverChapterSelect.value;
    }

    function extractServerError(html) {
        if (!html) {
            return "";
        }

        const doc = new DOMParser().parseFromString(html, "text/html");
        const serverStatus = doc.getElementById("chapterSubmissionClientStatus");
        if (serverStatus) {
            return serverStatus.textContent.trim();
        }

        const fallback = doc.querySelector(".status-banner-error");
        return fallback ? fallback.textContent.trim() : "";
    }

    async function loadChapterOptions() {
        if (!languageSelect || !chapterSelect || !languageSelect.value) {
            return;
        }

        try {
            const response = await fetch(
                `${root.dataset.optionsUrl}?languageId=${encodeURIComponent(languageSelect.value)}`,
                {
                    headers: { "X-Requested-With": "XMLHttpRequest" }
                }
            );

            if (!response.ok) {
                throw new Error("options load failed");
            }

            const data = await response.json();
            const numbers = Array.isArray(data.chapterNumbers) ? data.chapterNumbers : [];
            const previousValue = chapterSelect.value;

            chapterSelect.innerHTML = "";

            numbers.forEach((number) => {
                const option = document.createElement("option");
                option.value = String(number);
                option.textContent = `Глава ${number}`;
                if (String(number) === previousValue) {
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
        syncAutoTranslateTargetOptions();
        clearAutoPreviewState();
        await loadChapterOptions();
    });

    languageSelect?.addEventListener("change", async () => {
        clearAutoPreviewState();
        syncAutoTranslateTargetOptions();
        await loadChapterOptions();
    });

    chapterSelect?.addEventListener("change", clearAutoPreviewState);
    sourceLanguageSelect?.addEventListener("change", clearAutoPreviewState);

    titleInput?.addEventListener("input", () => {
        titleInput.value = limitOnly(titleInput.value, LIMITS.title);
    });

    titleInput?.addEventListener("blur", () => {
        titleInput.value = trimAndLimit(titleInput.value, LIMITS.title);
    });

    fileInput?.addEventListener("change", renderFilesList);

    autoTranslate?.addEventListener("change", () => {
        syncAutoTranslateUi();
        renderFilesList();
    });

    buildAutoPreviewBtn?.addEventListener("click", async () => {
        hideStatus();

        if (!autoTranslate || !autoTranslate.checked) {
            showStatus("Сначала включите автоматический перевод.");
            return;
        }

        if (!languageSelect?.value) {
            showStatus("Выберите язык перевода.");
            return;
        }

        if (!sourceLanguageSelect?.value) {
            showStatus("Выберите язык исходного текста.");
            return;
        }

        const validation = validateFiles(fileInput?.files);
        if (!validation.ok) {
            showStatus(validation.message);
            return;
        }

        const selectedPageNumbers = getSelectedPageNumbers();
        if (!selectedPageNumbers.length) {
            showStatus("Отметьте хотя бы одну страницу для автоматического перевода.");
            return;
        }

        const previewUrl = root.dataset.previewUrl;
        if (!previewUrl) {
            showStatus("Не найден адрес предпросмотра автоматического перевода.");
            return;
        }

        setPreviewLoading(true);
        clearAutoPreviewState();

        try {
            const formData = new FormData(form);
            formData.delete("selectedPageNumbers");
            selectedPageNumbers.forEach((value) => formData.append("selectedPageNumbers", String(value)));

            const response = await fetch(previewUrl, {
                method: "POST",
                headers: { "X-Requested-With": "XMLHttpRequest" },
                body: formData
            });

            const payload = await response.json();

            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || "Не удалось построить предпросмотр автоматического перевода.");
            }

            if (autoPreviewTokenInput) {
                autoPreviewTokenInput.value = payload.previewToken || "";
            }

            renderPreview(payload);
        } catch (error) {
            showStatus(error.message || "Не удалось построить предпросмотр автоматического перевода.");
        } finally {
            setPreviewLoading(false);
        }
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideStatus();

        if (titleInput) {
            titleInput.value = trimAndLimit(titleInput.value, LIMITS.title);
            if (!titleInput.value) {
                showStatus("Введите название перевода.");
                return;
            }
        }

        if (!languageSelect || !languageSelect.value) {
            showStatus("Выберите язык перевода.");
            return;
        }

        if (autoTranslate?.checked) {
            if (!sourceLanguageSelect?.value) {
                showStatus("Выберите язык исходного текста.");
                return;
            }

            if (!autoPreviewTokenInput?.value) {
                showStatus("Сначала выполните предпросмотр автоматического перевода.");
                return;
            }
        } else {
            const validation = validateFiles(fileInput?.files);
            if (!validation.ok) {
                showStatus(validation.message);
                return;
            }
        }

        setSubmittingState(true);

        try {
            const response = await fetch(form.action, {
                method: "POST",
                body: new FormData(form),
                headers: { "X-Requested-With": "XMLHttpRequest" }
            });

            if (response.redirected) {
                window.location.assign(response.url);
                return;
            }

            const html = await response.text();

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            syncChapterOptionsFromServerHtml(html);

            const serverError = extractServerError(html);
            if (serverError) {
                showStatus(serverError);
                return;
            }

            window.location.reload();
        } catch (_) {
            showStatus("Не удалось сохранить перевод. Попробуйте ещё раз.");
        } finally {
            setSubmittingState(false);
        }
    });

    rebuildLanguageOptions();
    loadChapterOptions();
    syncAutoTranslateUi();
    renderFilesList();
})();