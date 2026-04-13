(function () {
    const app = document.getElementById('readerApp');
    if (!app || !window.readerBootstrap) return;

    const ctx = app.dataset.contextPath || '';
    const translationId = Number(app.dataset.translationId);
    const totalPages = Number(app.dataset.totalPages || 0);
    const initialPage = Number(app.dataset.initialPage || 1);
    const isLogged = String(app.dataset.isLogged) === 'true';

    const topbar = document.getElementById('readerTopbar');
    const horizontalBox = document.getElementById('readerHorizontal');
    const currentImage = document.getElementById('readerCurrentImage');
    const verticalBox = document.getElementById('readerVertical');
    const pageCounter = document.getElementById('readerPageCounter');
    const pageCurrentEl = document.getElementById('readerPageCurrent');
    const pageTotalEl = document.getElementById('readerPageTotal');
    const settingsPanel = document.getElementById('readerSettingsPanel');
    const settingsBtn = document.getElementById('readerSettingsBtn');
    const settingsClose = document.getElementById('readerSettingsClose');
    const themeBtn = document.getElementById('readerThemeBtn');
    const complaintBtn = document.getElementById('readerComplaintBtn');

    const settingReadingMode = document.getElementById('settingReadingMode');
    const settingFitMode = document.getElementById('settingFitMode');
    const settingImageWidth = document.getElementById('settingImageWidth');
    const settingImageWidthValue = document.getElementById('settingImageWidthValue');
    const settingVerticalGap = document.getElementById('settingVerticalGap');
    const settingVerticalGapValue = document.getElementById('settingVerticalGapValue');
    const verticalGapGroup = document.getElementById('verticalGapGroup');
    const settingClickZones = document.getElementById('settingClickZones');
    const settingInvertClicks = document.getElementById('settingInvertClicks');
    const settingTopbarVisible = document.getElementById('settingTopbarVisible');
    const settingCounterVisible = document.getElementById('settingCounterVisible');
    const settingPrevKey = document.getElementById('settingPrevKey');
    const settingNextKey = document.getElementById('settingNextKey');
    const settingSettingsKey = document.getElementById('settingSettingsKey');

    const prevChapterBtn = document.getElementById('readerPrevChapterBtn');
    const nextChapterBtn = document.getElementById('readerNextChapterBtn');

    const warningModal = document.getElementById('readerLanguageWarning');
    const warningClose = document.getElementById('readerLanguageWarningClose');
    const warningTitle = document.getElementById('readerLanguageWarningTitle');
    const warningText = document.getElementById('readerLanguageWarningText');
    const warningContinue = document.getElementById('readerLanguageWarningContinue');
    const toast = document.getElementById('readerToast');

    const pages = window.readerBootstrap.pages || [];
    pageTotalEl.textContent = String(totalPages);

    const SETTINGS_KEY = 'comixuniverse.reader.settings';

    const defaultSettings = {
        readingMode: 'horizontal',
        fitMode: 'height',
        imageWidth: 100,
        verticalGap: 20,
        clickZones: 'left-right',
        invertClicks: false,
        topbarVisible: true,
        counterVisible: true,
        prevKey: 'ArrowLeft',
        nextKey: 'ArrowRight',
        settingsKey: 'KeyS'
    };

    const state = {
        currentPage: Math.min(Math.max(initialPage, 1), totalPages),
        activeVerticalImage: null,
        intersectionMap: new Map(),
        progressTimer: null,
        pendingChapterUrl: null,
        toastTimer: null,
        suppressVerticalAutoPageSync: false,
        verticalScrollEndTimer: null
    };

    function showToast(message) {
        if (!toast) {
            alert(message);
            return;
        }

        clearTimeout(state.toastTimer);
        toast.textContent = message;
        toast.classList.remove('visible');
        void toast.offsetWidth;
        toast.classList.add('visible', 'error');

        state.toastTimer = setTimeout(() => {
            toast.classList.remove('visible');
        }, 2600);
    }

    function loadSettings() {
        try {
            const raw = localStorage.getItem(SETTINGS_KEY);
            if (!raw) return {...defaultSettings};
            return {...defaultSettings, ...JSON.parse(raw)};
        } catch (e) {
            return {...defaultSettings};
        }
    }

    function saveSettings() {
        localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    }

    function queueProgressSave() {
        if (!isLogged) return;
        clearTimeout(state.progressTimer);
        state.progressTimer = setTimeout(() => {
            const body = new URLSearchParams();
            body.set('page', String(state.currentPage));
            fetch(`${ctx}/read/${translationId}/progress`, {
                method: 'POST',
                headers: {'X-Requested-With': 'XMLHttpRequest'},
                body
            }).catch(() => {
            });
        }, 450);
    }

    function resolveImageUrl(rawPath) {
        if (!rawPath) return '';
        if (/^https?:\/\//i.test(rawPath)) return rawPath;
        if (rawPath.startsWith('/')) return ctx + rawPath;
        return `${ctx}/assets/pages/${rawPath}`;
    }

    function syncSettingsInputs() {
        settingReadingMode.value = settings.readingMode;
        settingFitMode.value = settings.fitMode;
        settingImageWidth.value = String(settings.imageWidth);
        settingVerticalGap.value = String(settings.verticalGap);
        settingClickZones.value = settings.clickZones;
        settingInvertClicks.checked = !!settings.invertClicks;
        settingTopbarVisible.checked = !!settings.topbarVisible;
        settingCounterVisible.checked = !!settings.counterVisible;
        settingPrevKey.value = settings.prevKey;
        settingNextKey.value = settings.nextKey;
        settingSettingsKey.value = settings.settingsKey;
        settingImageWidthValue.textContent = `${settings.imageWidth}%`;
        settingVerticalGapValue.textContent = `${settings.verticalGap}px`;
        verticalGapGroup.style.display = settings.readingMode === 'vertical' ? 'block' : 'none';
    }

    function getTopbarHeight() {
        return topbar && settings.topbarVisible ? Math.round(topbar.getBoundingClientRect().height) : 0;
    }

    function syncTopbarOffset() {
        const topbarOffset = settings.topbarVisible ? `${getTopbarHeight()}px` : '0px';
        app.style.setProperty('--reader-topbar-offset', topbarOffset);
    }

    function getCounterHeight() {
        return settings.counterVisible ? pageCounter.getBoundingClientRect().height : 0;
    }

    function updatePageIndicator() {
        pageCurrentEl.textContent = String(state.currentPage);
        pageCounter.classList.toggle('hidden', !settings.counterVisible);
        queueProgressSave();
    }

    function renderHorizontalPage() {
        const page = pages[state.currentPage - 1];
        if (!page) return;

        horizontalBox.scrollTo({
            top: 0,
            left: 0,
            behavior: 'auto'
        });

        currentImage.src = resolveImageUrl(page.imagePath);
        currentImage.alt = `Страница ${page.number}`;
        updatePageIndicator();
    }

    function renderVerticalPages() {
        verticalBox.querySelectorAll('.reader-vertical-image').forEach((img) => {
            if (!img.getAttribute('src')) {
                img.setAttribute('src', resolveImageUrl(img.dataset.imagePath));
            }
        });
        requestAnimationFrame(() => {
            updateCurrentVerticalPage();
        });
    }

    function getBestVisibleImage() {
        const images = Array.from(verticalBox.querySelectorAll('.reader-vertical-image'));
        if (!images.length) return null;

        let best = null;
        let bestRatio = -1;

        images.forEach((img) => {
            const ratio = state.intersectionMap.get(img) ?? 0;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = img;
            }
        });

        if (best && bestRatio > 0) return best;

        const viewportCenter = window.innerHeight / 2;
        let nearest = images[0];
        let nearestDistance = Infinity;

        images.forEach((img) => {
            const rect = img.getBoundingClientRect();
            const center = rect.top + rect.height / 2;
            const distance = Math.abs(center - viewportCenter);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = img;
            }
        });

        return nearest;
    }

    function updateCurrentVerticalPage() {
        if (state.suppressVerticalAutoPageSync) return;

        const best = getBestVisibleImage();
        if (!best) return;

        state.activeVerticalImage = best;
        const current = Number(best.dataset.pageNumber || 1);

        if (current !== state.currentPage) {
            state.currentPage = current;
            updatePageIndicator();
        } else {
            pageCurrentEl.textContent = String(current);
        }
    }

    function scrollVerticalToPage(page, smooth) {
        const target = verticalBox.querySelector(`.reader-vertical-image[data-page-number="${page}"]`);
        if (!target) return;

        const boxRect = verticalBox.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();

        let nextScrollTop;

        if (settings.fitMode === 'height') {
            const offsetInsideBox = (targetRect.top - boxRect.top) + verticalBox.scrollTop;
            nextScrollTop = offsetInsideBox - ((verticalBox.clientHeight - target.offsetHeight) / 2);
        } else {
            const offsetInsideBox = (targetRect.top - boxRect.top) + verticalBox.scrollTop;
            nextScrollTop = offsetInsideBox;
        }

        nextScrollTop = Math.max(0, Math.round(nextScrollTop));

        clearTimeout(state.verticalScrollEndTimer);
        state.suppressVerticalAutoPageSync = true;
        state.activeVerticalImage = target;
        state.currentPage = page;
        updatePageIndicator();

        verticalBox.scrollTo({
            top: nextScrollTop,
            behavior: smooth ? 'smooth' : 'auto'
        });

        if (!smooth) {
            state.suppressVerticalAutoPageSync = false;
            return;
        }

        let lastTop = verticalBox.scrollTop;
        let stableFrames = 0;

        function watchScrollEnd() {
            const nowTop = verticalBox.scrollTop;

            if (Math.abs(nowTop - lastTop) <= 1) {
                stableFrames++;
            } else {
                stableFrames = 0;
                lastTop = nowTop;
            }

            if (stableFrames >= 4) {
                state.suppressVerticalAutoPageSync = false;
                state.activeVerticalImage = target;
                state.currentPage = page;
                updatePageIndicator();
                return;
            }

            state.verticalScrollEndTimer = requestAnimationFrame(watchScrollEnd);
        }

        state.verticalScrollEndTimer = requestAnimationFrame(watchScrollEnd);
    }

    function showLanguageWarning(targetUrl, targetLang, targetChapter) {
        state.pendingChapterUrl = targetUrl;
        warningTitle.textContent = 'Язык перевода изменится';
        warningText.textContent = `Для главы ${targetChapter} доступен перевод на языке «${targetLang}». Если нужен другой перевод, вернитесь на страницу комикса и выберите его вручную.`;
        warningModal.classList.add('visible');
    }

    function closeLanguageWarning() {
        state.pendingChapterUrl = null;
        warningModal.classList.remove('visible');
    }

    function openChapterLink(targetUrl, targetLang, targetChapter) {
        const currentLang = app.dataset.languageName || '';
        if (targetLang && currentLang && targetLang !== currentLang) {
            showLanguageWarning(targetUrl, targetLang, targetChapter);
            return;
        }
        window.location.href = targetUrl;
    }

    function goPrevChapter() {
        const prevId = app.dataset.prevTranslationId;
        if (!prevId) return;
        openChapterLink(
            `${ctx}/read/${prevId}`,
            app.dataset.prevLanguageName || '',
            app.dataset.prevChapterNumber || ''
        );
    }

    function goNextChapter() {
        const nextId = app.dataset.nextTranslationId;
        if (!nextId) return;
        openChapterLink(
            `${ctx}/read/${nextId}`,
            app.dataset.nextLanguageName || '',
            app.dataset.nextChapterNumber || ''
        );
    }

    function prevPage() {
        if (settings.readingMode === 'horizontal') {
            if (state.currentPage > 1) {
                state.currentPage--;
                renderHorizontalPage();
            } else {
                goPrevChapter();
            }
        } else {
            if (state.currentPage > 1) {
                const prevPageNumber = state.currentPage - 1;
                state.currentPage = prevPageNumber;
                scrollVerticalToPage(prevPageNumber, true);
            } else {
                goPrevChapter();
            }
        }
    }

    function nextPage() {
        if (settings.readingMode === 'horizontal') {
            if (state.currentPage < totalPages) {
                state.currentPage++;
                renderHorizontalPage();
            } else {
                goNextChapter();
            }
        } else {
            if (state.currentPage < totalPages) {
                const nextPage = state.currentPage + 1;
                state.currentPage = nextPage;
                scrollVerticalToPage(nextPage, true);
            } else {
                goNextChapter();
            }
        }
    }

    function getActiveImageRect() {
        if (settings.readingMode === 'horizontal') {
            return currentImage.getBoundingClientRect();
        }

        const active = state.activeVerticalImage || getBestVisibleImage();
        if (!active) return null;
        return active.getBoundingClientRect();
    }

    function handleTapAction(clientX, clientY) {
        const rect = getActiveImageRect();
        if (!rect || rect.width <= 0 || rect.height <= 0) return;

        if (
            clientX < rect.left ||
            clientX > rect.right ||
            clientY < rect.top ||
            clientY > rect.bottom
        ) {
            return;
        }

        const relX = (clientX - rect.left) / rect.width;
        const relY = (clientY - rect.top) / rect.height;

        let goPrev = false;

        if (settings.clickZones === 'top-bottom') {
            goPrev = relY < 0.5;
        } else {
            goPrev = relX < 0.5;
        }

        if (settings.invertClicks) {
            goPrev = !goPrev;
        }

        if (goPrev) {
            prevPage();
        } else {
            nextPage();
        }
    }

    function toggleSettings(force) {
        const shouldOpen = typeof force === 'boolean'
            ? force
            : settingsPanel.classList.contains('hidden');

        settingsPanel.classList.toggle('hidden', !shouldOpen);
        settingsPanel.setAttribute('aria-hidden', shouldOpen ? 'false' : 'true');
    }

    function applyHorizontalWidthModeSize() {
        if (settings.readingMode !== 'horizontal' || settings.fitMode !== 'width') return;

        const vw = window.innerWidth;
        const ratio = settings.imageWidth / 100;
        const targetWidth = Math.max(1, Math.round(vw * ratio));

        currentImage.style.width = `${targetWidth}px`;
        currentImage.style.height = 'auto';
        currentImage.style.maxWidth = 'none';
        currentImage.style.maxHeight = 'none';
    }

    function clearInlineImageSize() {
        currentImage.style.width = '';
        currentImage.style.height = '';
        currentImage.style.maxWidth = '';
        currentImage.style.maxHeight = '';
    }

    function updateReaderPointerCursor(clientX, clientY) {
        const rect = getActiveImageRect();
        const isOverImage =
            rect &&
            rect.width > 0 &&
            rect.height > 0 &&
            clientX >= rect.left &&
            clientX <= rect.right &&
            clientY >= rect.top &&
            clientY <= rect.bottom;

        if (settings.readingMode === 'horizontal') {
            currentImage.style.cursor = isOverImage ? 'pointer' : 'default';
        } else {
            const images = verticalBox.querySelectorAll('.reader-vertical-image');
            images.forEach((img) => {
                img.style.cursor = 'default';
            });

            if (isOverImage && state.activeVerticalImage) {
                state.activeVerticalImage.style.cursor = 'pointer';
            }
        }
    }

    function applyReaderLayout() {
        clearTimeout(state.verticalScrollEndTimer);
        state.suppressVerticalAutoPageSync = false;

        app.style.setProperty('--reader-image-scale', `${settings.imageWidth}vw`);
        app.style.setProperty('--reader-vertical-gap', `${settings.verticalGap}px`);

        horizontalBox.classList.remove('fit-height', 'fit-width');
        verticalBox.classList.remove('fit-height', 'fit-width');

        horizontalBox.classList.add(settings.fitMode === 'width' ? 'fit-width' : 'fit-height');
        verticalBox.classList.add(settings.fitMode === 'width' ? 'fit-width' : 'fit-height');

        topbar.style.display = settings.topbarVisible ? 'flex' : 'none';
        syncTopbarOffset();

        if (settings.readingMode === 'horizontal') {
            horizontalBox.classList.remove('hidden');
            verticalBox.classList.add('hidden');
            renderHorizontalPage();
        } else {
            verticalBox.classList.remove('hidden');
            horizontalBox.classList.add('hidden');
            renderVerticalPages();
            scrollVerticalToPage(state.currentPage, false);
        }

        syncSettingsInputs();
        updatePageIndicator();

        if (settings.readingMode === 'horizontal' && settings.fitMode === 'width') {
            applyHorizontalWidthModeSize();
        } else {
            clearInlineImageSize();
        }

        requestAnimationFrame(() => {
            if (settings.readingMode === 'vertical') {
                updateCurrentVerticalPage();
            }
        });
    }

    function hasDuplicateKey(settingName, code) {
        const used = Object.entries({
            prevKey: settings.prevKey,
            nextKey: settings.nextKey,
            settingsKey: settings.settingsKey
        });

        return used.some(([key, value]) => key !== settingName && value === code);
    }

    function bindKeyCapture(input, settingName) {
        input.addEventListener('keydown', (e) => {
            e.preventDefault();
            if (hasDuplicateKey(settingName, e.code)) {
                showToast('Эта горячая клавиша уже используется.');
                return;
            }
            settings[settingName] = e.code;
            input.value = e.code;
            saveSettings();
        });
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        const next = current === 'light' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
    }

    const settings = loadSettings();
    syncSettingsInputs();
    applyReaderLayout();

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            state.intersectionMap.set(entry.target, entry.intersectionRatio);
        });

        if (settings.readingMode === 'vertical' && !state.suppressVerticalAutoPageSync) {
            updateCurrentVerticalPage();
        }
    }, {
        threshold: [0, 0.15, 0.25, 0.4, 0.55, 0.7, 0.85, 1]
    });

    verticalBox.querySelectorAll('.reader-vertical-image').forEach((img) => observer.observe(img));

    let pointerStartX = 0;
    let pointerStartY = 0;
    let pointerMoved = false;
    let pointerStartScrollTop = 0;

    function onPointerDown(e) {
        pointerMoved = false;
        pointerStartX = e.clientX;
        pointerStartY = e.clientY;
        pointerStartScrollTop = verticalBox.scrollTop;
    }

    function onPointerMove(e) {
        const dx = Math.abs(e.clientX - pointerStartX);
        const dy = Math.abs(e.clientY - pointerStartY);

        if (dx > 8 || dy > 8) {
            pointerMoved = true;
        }

        if (settings.readingMode === 'vertical') {
            if (Math.abs(verticalBox.scrollTop - pointerStartScrollTop) > 6) {
                pointerMoved = true;
            }
        }

        updateReaderPointerCursor(e.clientX, e.clientY);
    }

    function onPointerUp(e) {
        if (pointerMoved) return;
        handleTapAction(e.clientX, e.clientY);
    }

    horizontalBox.addEventListener('pointerdown', onPointerDown, {passive: true});
    horizontalBox.addEventListener('pointermove', onPointerMove, {passive: true});
    horizontalBox.addEventListener('pointerup', onPointerUp);
    horizontalBox.addEventListener('pointerleave', () => {
        currentImage.style.cursor = 'default';
    });
    verticalBox.addEventListener('pointerleave', () => {
        verticalBox.querySelectorAll('.reader-vertical-image').forEach((img) => {
            img.style.cursor = 'default';
        });
    });
    verticalBox.addEventListener('pointerdown', onPointerDown, {passive: true});
    verticalBox.addEventListener('pointermove', onPointerMove, {passive: true});
    verticalBox.addEventListener('pointerup', onPointerUp);

    settingsBtn.addEventListener('click', () => toggleSettings(true));
    settingsClose.addEventListener('click', () => toggleSettings(false));
    settingsPanel.addEventListener('click', (e) => {
        if (e.target === settingsPanel) toggleSettings(false);
    });

    themeBtn.addEventListener('click', toggleTheme);

    complaintBtn.addEventListener('click', () => {
        showToast('Функция жалобы будет подключена отдельно.');
    });

    if (prevChapterBtn) {
        prevChapterBtn.addEventListener('click', (e) => {
            e.preventDefault();
            goPrevChapter();
        });
    }

    if (nextChapterBtn) {
        nextChapterBtn.addEventListener('click', (e) => {
            e.preventDefault();
            goNextChapter();
        });
    }

    warningClose.addEventListener('click', closeLanguageWarning);
    warningModal.addEventListener('click', (e) => {
        if (e.target === warningModal) closeLanguageWarning();
    });
    warningContinue.addEventListener('click', () => {
        if (state.pendingChapterUrl) {
            window.location.href = state.pendingChapterUrl;
        }
    });

    settingReadingMode.addEventListener('change', () => {
        settings.readingMode = settingReadingMode.value;
        saveSettings();
        applyReaderLayout();
    });

    settingFitMode.addEventListener('change', () => {
        settings.fitMode = settingFitMode.value;
        saveSettings();
        applyReaderLayout();
    });

    settingImageWidth.addEventListener('input', () => {
        settings.imageWidth = Number(settingImageWidth.value);
        saveSettings();
        applyReaderLayout();
    });

    settingVerticalGap.addEventListener('input', () => {
        settings.verticalGap = Number(settingVerticalGap.value);
        saveSettings();
        applyReaderLayout();
    });

    settingClickZones.addEventListener('change', () => {
        settings.clickZones = settingClickZones.value;
        saveSettings();
        applyReaderLayout();
    });

    settingInvertClicks.addEventListener('change', () => {
        settings.invertClicks = settingInvertClicks.checked;
        saveSettings();
    });

    settingTopbarVisible.addEventListener('change', () => {
        settings.topbarVisible = settingTopbarVisible.checked;
        saveSettings();
        applyReaderLayout();
    });

    settingCounterVisible.addEventListener('change', () => {
        settings.counterVisible = settingCounterVisible.checked;
        saveSettings();
        applyReaderLayout();
    });

    bindKeyCapture(settingPrevKey, 'prevKey');
    bindKeyCapture(settingNextKey, 'nextKey');
    bindKeyCapture(settingSettingsKey, 'settingsKey');

    document.addEventListener('keydown', (e) => {
        if (document.activeElement &&
            (document.activeElement.tagName === 'INPUT' ||
                document.activeElement.tagName === 'TEXTAREA' ||
                document.activeElement.tagName === 'SELECT')) {
            return;
        }

        if (e.code === settings.settingsKey) {
            e.preventDefault();
            toggleSettings();
            return;
        }

        if (e.code === settings.prevKey) {
            e.preventDefault();
            prevPage();
            return;
        }

        if (e.code === settings.nextKey) {
            e.preventDefault();
            nextPage();
        }
    });

    verticalBox.addEventListener('scroll', () => {
        if (settings.readingMode === 'vertical' && !state.suppressVerticalAutoPageSync) {
            window.requestAnimationFrame(() => {
                updateCurrentVerticalPage();
            });
        }
    }, {passive: true});

    window.addEventListener('resize', () => {
        window.requestAnimationFrame(() => {
            syncTopbarOffset();
            if (settings.readingMode === 'vertical') {
                updateCurrentVerticalPage();
            }
            if (settings.readingMode === 'horizontal' && settings.fitMode === 'width') {
                applyHorizontalWidthModeSize();
            }
        });
    });

    currentImage.addEventListener('load', () => {
        if (settings.readingMode === 'horizontal' && settings.fitMode === 'width') {
            applyHorizontalWidthModeSize();
        } else {
            clearInlineImageSize();
        }
    });

    syncTopbarOffset();
})();