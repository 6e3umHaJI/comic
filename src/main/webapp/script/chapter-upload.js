(() => {
    const LIMITS = {
        maxPages: 200,
        maxFileSizeBytes: 1024 * 1024,
        languageSearch: 35,
        title: 255
    };

    const FILE_PATTERN = /^(\d{1,3})\.[A-Za-z0-9]{1,10}$/i;

    const root = document.getElementById('chapterSubmissionPage');
    if (!root) {
        return;
    }

    const form = document.getElementById('chapterSubmissionForm');
    const languageSearch = document.getElementById('languageSearch');
    const languageSelect = document.getElementById('languageId');
    const chapterSelect = document.getElementById('chapterNumber');
    const titleInput = document.getElementById('title');
    const fileInput = document.getElementById('pageFiles');
    const filesList = document.getElementById('chapterSubmissionFilesList');
    const statusBox = document.getElementById('chapterSubmissionClientStatus');
    const autoTranslate = document.getElementById('autoTranslate');
    const readingDirectionField = document.getElementById('readingDirectionField');

    const originalLanguageOptions = languageSelect
        ? Array.from(languageSelect.options).map((option) => ({
            value: option.value,
            text: option.textContent
        }))
        : [];

    function showStatus(message) {
        if (!statusBox) return;
        statusBox.textContent = message;
        statusBox.classList.remove('hidden');
    }

    function hideStatus() {
        if (!statusBox) return;
        statusBox.textContent = '';
        statusBox.classList.add('hidden');
    }

    function trimAndLimit(value, maxLength) {
        return String(value ?? '').trim().slice(0, maxLength);
    }

    function limitOnly(value, maxLength) {
        return String(value ?? '').slice(0, maxLength);
    }

    function rebuildLanguageOptions() {
        if (!languageSearch || !languageSelect) return;

        languageSearch.value = limitOnly(languageSearch.value, LIMITS.languageSearch);
        const query = languageSearch.value.trim().toLowerCase();
        const selectedValue = languageSelect.value;

        const filtered = !query
            ? originalLanguageOptions
            : originalLanguageOptions.filter((item) => item.text.toLowerCase().includes(query));

        languageSelect.innerHTML = '';

        if (!filtered.length) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = 'Ничего не найдено';
            option.disabled = true;
            option.selected = true;
            languageSelect.appendChild(option);
            return;
        }

        filtered.forEach((item) => {
            const option = document.createElement('option');
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

    function extractPageNumber(file) {
        const match = FILE_PATTERN.exec(file.name || '');
        return match ? Number(match[1]) : null;
    }

    function validateFiles(files) {
        const actualFiles = Array.from(files || []).filter(Boolean);

        if (!actualFiles.length) {
            return { ok: false, message: 'Выберите страницы перевода.' };
        }

        if (actualFiles.length > LIMITS.maxPages) {
            return { ok: false, message: `Максимум можно загрузить ${LIMITS.maxPages} страниц.` };
        }

        for (const file of actualFiles) {
            if (file.size > LIMITS.maxFileSizeBytes) {
                return { ok: false, message: 'Каждое изображение должно быть не больше 1 МБ.' };
            }

            if (!FILE_PATTERN.test(file.name || '')) {
                return { ok: false, message: 'Имена файлов должны быть вида 001.jpg, 002.jpg, 003.jpg и так далее.' };
            }

            if (!String(file.type || '').toLowerCase().startsWith('image/')) {
                return { ok: false, message: 'Можно загружать только изображения.' };
            }
        }

        const sorted = actualFiles
            .map((file) => ({ file, pageNumber: extractPageNumber(file) }))
            .sort((left, right) => left.pageNumber - right.pageNumber);

        for (let i = 0; i < sorted.length; i++) {
            if (sorted[i].pageNumber !== i + 1) {
                return { ok: false, message: 'Файлы страниц должны идти подряд: 001, 002, 003 и так далее.' };
            }
        }

        return { ok: true, files: sorted };
    }

    function renderFilesList() {
        if (!filesList || !fileInput) return;

        const validation = validateFiles(fileInput.files);

        if (!validation.ok) {
            filesList.textContent = validation.message;
            filesList.classList.add('chapter-upload-files-empty');
            showStatus(validation.message);
            return;
        }

        hideStatus();
        filesList.classList.remove('chapter-upload-files-empty');
        filesList.innerHTML = '';

        validation.files.forEach((entry) => {
            const row = document.createElement('div');
            row.className = 'chapter-upload-file-row';

            const left = document.createElement('span');
            left.textContent = `Страница ${entry.pageNumber}`;

            const right = document.createElement('span');
            right.textContent = entry.file.name;

            row.append(left, right);
            filesList.appendChild(row);
        });
    }

    async function loadChapterOptions() {
        if (!languageSelect || !chapterSelect || !languageSelect.value) {
            return;
        }

        try {
            const response = await fetch(
                `${root.dataset.optionsUrl}?languageId=${encodeURIComponent(languageSelect.value)}`,
                { headers: { 'X-Requested-With': 'XMLHttpRequest' } }
            );

            if (!response.ok) {
                throw new Error('options load failed');
            }

            const data = await response.json();
            const numbers = Array.isArray(data.chapterNumbers) ? data.chapterNumbers : [];
            const previousValue = chapterSelect.value;

            chapterSelect.innerHTML = '';

            numbers.forEach((number) => {
                const option = document.createElement('option');
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
            showStatus('Не удалось загрузить доступные номера глав.');
        }
    }

    function syncAutoTranslateField() {
        if (!autoTranslate || !readingDirectionField) return;
        readingDirectionField.classList.toggle('hidden', !autoTranslate.checked);
    }

    languageSearch?.addEventListener('input', () => {
        rebuildLanguageOptions();
        loadChapterOptions();
    });

    languageSelect?.addEventListener('change', loadChapterOptions);

    titleInput?.addEventListener('input', () => {
        titleInput.value = limitOnly(titleInput.value, LIMITS.title);
        hideStatus();
    });

    titleInput?.addEventListener('blur', () => {
        titleInput.value = trimAndLimit(titleInput.value, LIMITS.title);
    });

    fileInput?.addEventListener('change', renderFilesList);
    autoTranslate?.addEventListener('change', syncAutoTranslateField);

    form?.addEventListener('submit', (event) => {
        hideStatus();

        if (titleInput) {
            titleInput.value = trimAndLimit(titleInput.value, LIMITS.title);
            if (!titleInput.value) {
                event.preventDefault();
                showStatus('Введите название перевода.');
                return;
            }
        }

        if (!languageSelect || !languageSelect.value) {
            event.preventDefault();
            showStatus('Выберите язык перевода.');
            return;
        }

        const validation = validateFiles(fileInput?.files);
        if (!validation.ok) {
            event.preventDefault();
            showStatus(validation.message);
        }
    });

    rebuildLanguageOptions();
    loadChapterOptions();
    syncAutoTranslateField();
})();
