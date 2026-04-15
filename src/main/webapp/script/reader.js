

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
    const collectionBtn = document.getElementById('readerCollectionBtn');
    const loadingOverlay = document.getElementById('readerLoadingOverlay');
    const loadingText = document.getElementById('readerLoadingText');
    const readerSurface = document.getElementById('readerSurface');
    const imageWidthGroup = document.getElementById('imageWidthGroup');
    const settingsNotice = document.getElementById('readerSettingsNotice');

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
    const readerZonePrev = document.getElementById('readerZonePrev');
    const readerZoneNext = document.getElementById('readerZoneNext');

    const trModal = document.getElementById('translationsModal');
    const trClose = document.getElementById('trClose');
    const trTitle = document.getElementById('trTitle');
    const trNotice = document.getElementById('trNotice');
    const trLangSelect = document.getElementById('trLangSelect');
    const trList = document.getElementById('trList');
    const trMore = document.getElementById('trMore');

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
        toastTimer: null,
        suppressVerticalAutoPageSync: false,
        verticalScrollEndTimer: null,
        trChapterId: null,
        trPage: 0,
        trTotal: 0
    };

    function showToast(message) {
        if (!toast) return;

        clearTimeout(state.toastTimer);
        toast.textContent = message;
        toast.classList.remove('visible');
        void toast.offsetWidth;
        toast.classList.add('visible', 'error');

        state.toastTimer = setTimeout(() => {
            toast.classList.remove('visible');
        }, 2600);
    }

    function showSettingsNotice(message) {
        if (!settingsNotice) {
            showToast(message);
            return;
        }

        settingsNotice.hidden = false;
        settingsNotice.textContent = message;
        settingsNotice.className = 'reader-settings-notice error';
    }

    function hideSettingsNotice() {
        if (!settingsNotice) return;
        settingsNotice.hidden = true;
        settingsNotice.textContent = '';
        settingsNotice.className = 'reader-settings-notice';
    }

    function openAuthRequiredModalFallback() {
        if (typeof window.openAuthRequiredModal === 'function') {
            window.openAuthRequiredModal();
            return;
        }

        const modal = document.getElementById('authRequiredModal');
        if (modal) {
            modal.classList.add('visible');
            modal.hidden = false;
        }
    }

    function showReaderLoading(text = 'Загрузка страниц…') {
        if (loadingText) {
            loadingText.textContent = text;
        }
        if (loadingOverlay) {
            loadingOverlay.classList.add('visible');
            loadingOverlay.setAttribute('aria-hidden', 'false');
        }
    }

    function hideReaderLoading() {
        if (loadingOverlay) {
            loadingOverlay.classList.remove('visible');
            loadingOverlay.setAttribute('aria-hidden', 'true');
        }
    }

    function waitForImageReady(img, timeout = 3500) {
        return new Promise((resolve) => {
            if (!img) {
                resolve();
                return;
            }

            if (img.loading === 'lazy') {
                img.loading = 'eager';
            }

            if (img.complete) {
                resolve();
                return;
            }

            let finished = false;

            const done = () => {
                if (finished) return;
                finished = true;
                clearTimeout(timer);
                resolve();
            };

            const timer = setTimeout(done, timeout);

            img.addEventListener('load', done, { once: true });
            img.addEventListener('error', done, { once: true });
        });
    }

    function waitForCurrentReaderImage() {
        if (settings.readingMode === 'horizontal') {
            return waitForImageReady(currentImage);
        }

        const target = verticalBox.querySelector(`.reader-vertical-image[data-page-number="${state.currentPage}"]`);
        return waitForImageReady(target);
    }

    function loadSettings() {
        try {
            const raw = localStorage.getItem(SETTINGS_KEY);
            if (!raw) return { ...defaultSettings };
            return { ...defaultSettings, ...JSON.parse(raw) };
        } catch (e) {
            return { ...defaultSettings };
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
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                body
            }).catch(() => {});
        }, 450);
    }

    function saveProgressNow() {
        if (!isLogged) return;

        const body = new URLSearchParams();
        body.set('page', String(state.currentPage));

        if (navigator.sendBeacon) {
            const blob = new Blob([body.toString()], {
                type: 'application/x-www-form-urlencoded; charset=UTF-8'
            });
            navigator.sendBeacon(`${ctx}/read/${translationId}/progress`, blob);
            return;
        }

        fetch(`${ctx}/read/${translationId}/progress`, {
            method: 'POST',
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            body,
            keepalive: true
        }).catch(() => {});
    }

    function resolveImageUrl(rawPath) {
        if (!rawPath) return '';
        if (/^https?:\/\//i.test(rawPath)) return rawPath;
        if (rawPath.startsWith('/')) return ctx + rawPath;
        return `${ctx}/assets/pages/${rawPath}`;
    }

    function syncReaderZones() {
        if (!readerZonePrev || !readerZoneNext) return;

        const topBottom = settings.clickZones === 'top-bottom';

        readerZonePrev.classList.toggle('zone-top-bottom-prev', topBottom);
        readerZoneNext.classList.toggle('zone-top-bottom-next', topBottom);
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

        if (verticalGapGroup) {
            verticalGapGroup.style.display = settings.readingMode === 'vertical' ? 'block' : 'none';
        }

        if (imageWidthGroup) {
            imageWidthGroup.style.display = settings.fitMode === 'width' ? 'block' : 'none';
        }

        syncReaderZones();
    }

    function getTopbarHeight() {
        return topbar && settings.topbarVisible ? Math.round(topbar.getBoundingClientRect().height) : 0;
    }

    function syncReaderSurfaceHeight() {
        if (!readerSurface) return;
        app.style.setProperty('--reader-surface-height', `${readerSurface.clientHeight}px`);
    }

    function syncTopbarOffset() {
        const topbarOffset = settings.topbarVisible ? `${getTopbarHeight()}px` : '0px';
        app.style.setProperty('--reader-topbar-offset', topbarOffset);
        syncReaderSurfaceHeight();
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

        if (state.verticalScrollEndTimer) {
            cancelAnimationFrame(state.verticalScrollEndTimer);
            state.verticalScrollEndTimer = null;
        }

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

    function switchReadingMode(nextMode) {
        if (settings.readingMode === nextMode) return;

        const anchorPage = state.currentPage;
        settings.readingMode = nextMode;
        saveSettings();

        showReaderLoading('Переключаем режим чтения…');
        applyReaderLayout(anchorPage);
    }

    function closeTranslationsModal() {
        state.trChapterId = null;
        state.trPage = 0;
        state.trTotal = 0;

        if (trList) trList.innerHTML = '';
        if (trLangSelect) trLangSelect.innerHTML = '';
        if (trMore) trMore.style.display = 'none';

        if (trNotice) {
            trNotice.textContent = '';
            trNotice.classList.add('hidden');
        }

        if (trModal) {
            trModal.hidden = true;
            trModal.classList.remove('visible');
        }

        document.body.style.overflow = '';
    }

    function loadTranslations(append) {
        if (!state.trChapterId) return;

        const lang = trLangSelect?.value || '';
        const params = new URLSearchParams({
            page: String(state.trPage),
            size: '10',
            lang
        });

        fetch(`${ctx}/comics/chapters/${state.trChapterId}/translations?${params.toString()}`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.text();
            })
            .then(html => {
                const tmp = document.createElement('div');
                tmp.innerHTML = html;

                const chunk = tmp.querySelector('#translationsChunk');
                if (!chunk) {
                    if (!append && trList) {
                        trList.innerHTML = '<li class="translation-empty"><em>Не удалось загрузить переводы.</em></li>';
                    }
                    if (trMore) trMore.style.display = 'none';
                    return;
                }

                const total = parseInt(chunk.dataset.total || '0', 10);
                if (state.trPage === 0) state.trTotal = total;

                const listNode = chunk.querySelector('.translation-list');
                if (!append && trList) trList.innerHTML = '';

                if (listNode && trList) {
                    Array.from(listNode.children).forEach((li) => trList.appendChild(li));
                }

                const loaded = trList ? trList.querySelectorAll('.translation-item').length : 0;
                if (trMore) {
                    trMore.style.display = loaded < state.trTotal ? 'inline-flex' : 'none';
                }
            })
            .catch(() => {
                if (!append && trList) {
                    trList.innerHTML = '<li class="translation-empty"><em>Ошибка загрузки.</em></li>';
                }
                if (trMore) trMore.style.display = 'none';
            });
    }

    function openChapterTranslationsModal(chapterId, chapterNumber, currentLang, fallbackLang) {
        state.trChapterId = chapterId;
        state.trPage = 0;
        state.trTotal = 0;

        if (trTitle) {
            trTitle.textContent = `Переводы главы ${chapterNumber}`;
        }

        if (trNotice) {
            trNotice.textContent = `Для главы ${chapterNumber} перевод на языке «${currentLang}» не найден.${fallbackLang ? ` Доступен перевод на языке «${fallbackLang}».` : ''} Выберите доступный перевод ниже.`;
            trNotice.classList.remove('hidden');
        }

        fetch(`${ctx}/read/chapters/${chapterId}/languages`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                if (!trLangSelect) return;

                trLangSelect.innerHTML = '';

                const allOption = document.createElement('option');
                allOption.value = '';
                allOption.textContent = 'Все языки';
                trLangSelect.appendChild(allOption);

                (data.languages || []).forEach((lang) => {
                    const option = document.createElement('option');
                    option.value = lang;
                    option.textContent = lang;
                    trLangSelect.appendChild(option);
                });

                if (fallbackLang && (data.languages || []).includes(fallbackLang)) {
                    trLangSelect.value = fallbackLang;
                }

                if (trModal) {
                    trModal.hidden = false;
                    trModal.classList.add('visible');
                }

                document.body.style.overflow = 'hidden';
                loadTranslations(false);
            })
            .catch(() => {
                showToast('Не удалось загрузить переводы главы.');
            });
    }

    function openChapterLink(targetUrl, targetChapterId, targetLang, targetChapter) {
        const currentLang = app.dataset.languageName || '';

        if (targetChapterId && targetLang && currentLang && targetLang !== currentLang) {
            openChapterTranslationsModal(targetChapterId, targetChapter, currentLang, targetLang);
            return;
        }

        saveProgressNow();
        window.location.href = targetUrl;
    }

    function goPrevChapter() {
        const prevId = app.dataset.prevTranslationId;
        if (!prevId) {
            showToast('Вы уже на первой главе.');
            return;
        }

        openChapterLink(
            `${ctx}/read/${prevId}`,
            app.dataset.prevChapterId || '',
            app.dataset.prevLanguageName || '',
            app.dataset.prevChapterNumber || ''
        );
    }

    function goNextChapter() {
        const nextId = app.dataset.nextTranslationId;
        if (!nextId) {
            showToast('Переводов для следующей главы нет.');
            return;
        }

        openChapterLink(
            `${ctx}/read/${nextId}`,
            app.dataset.nextChapterId || '',
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
                const nextPageNumber = state.currentPage + 1;
                state.currentPage = nextPageNumber;
                scrollVerticalToPage(nextPageNumber, true);
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

    function getClickedVerticalImage(clientX, clientY) {
        const hit = document.elementFromPoint(clientX, clientY);
        if (!hit) return null;

        const direct = hit.closest('.reader-vertical-image');
        if (direct && verticalBox.contains(direct)) {
            return direct;
        }

        const images = Array.from(verticalBox.querySelectorAll('.reader-vertical-image'));
        return images.find((img) => {
            const rect = img.getBoundingClientRect();
            return clientX >= rect.left &&
                clientX <= rect.right &&
                clientY >= rect.top &&
                clientY <= rect.bottom;
        }) || null;
    }

    function handleTapAction(clientX, clientY) {
        let rect = null;

        if (settings.readingMode === 'vertical') {
            const clickedImage = getClickedVerticalImage(clientX, clientY);
            if (!clickedImage) return;

            state.activeVerticalImage = clickedImage;
            state.currentPage = Number(clickedImage.dataset.pageNumber || state.currentPage);
            rect = clickedImage.getBoundingClientRect();
        } else {
            rect = getActiveImageRect();
        }

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

        let goPrev = settings.clickZones === 'top-bottom'
            ? relY < 0.5
            : relX < 0.5;

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

    function applyReaderLayout(anchorPage = state.currentPage) {
        if (state.verticalScrollEndTimer) {
            cancelAnimationFrame(state.verticalScrollEndTimer);
            state.verticalScrollEndTimer = null;
        }

        state.suppressVerticalAutoPageSync = true;
        state.currentPage = Math.min(Math.max(anchorPage, 1), totalPages);

        app.style.setProperty('--reader-image-scale', `${settings.imageWidth}vw`);
        app.style.setProperty('--reader-vertical-gap', `${settings.verticalGap}px`);

        horizontalBox.classList.remove('fit-height', 'fit-width');
        verticalBox.classList.remove('fit-height', 'fit-width');

        horizontalBox.classList.add(settings.fitMode === 'width' ? 'fit-width' : 'fit-height');
        verticalBox.classList.add(settings.fitMode === 'width' ? 'fit-width' : 'fit-height');

        topbar.style.display = settings.topbarVisible ? 'flex' : 'none';
        syncTopbarOffset();
        syncSettingsInputs();
        updatePageIndicator();

        if (settings.readingMode === 'horizontal') {
            horizontalBox.classList.remove('hidden');
            verticalBox.classList.add('hidden');

            renderHorizontalPage();

            if (settings.fitMode === 'width') {
                applyHorizontalWidthModeSize();
            } else {
                clearInlineImageSize();
            }

            waitForCurrentReaderImage().finally(() => {
                state.suppressVerticalAutoPageSync = false;
                hideReaderLoading();
            });
        } else {
            verticalBox.classList.remove('hidden');
            horizontalBox.classList.add('hidden');
            clearInlineImageSize();

            renderVerticalPages();

            requestAnimationFrame(() => {
                scrollVerticalToPage(state.currentPage, false);

                waitForCurrentReaderImage().finally(() => {
                    state.suppressVerticalAutoPageSync = false;
                    updateCurrentVerticalPage();
                    hideReaderLoading();
                });
            });
        }
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
                showSettingsNotice('Эта горячая клавиша уже используется.');
                return;
            }

            hideSettingsNotice();
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
    showReaderLoading('Загрузка страниц…');
    syncSettingsInputs();
    applyReaderLayout(state.currentPage);

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

    horizontalBox.addEventListener('pointerdown', onPointerDown, { passive: true });
    horizontalBox.addEventListener('pointermove', onPointerMove, { passive: true });
    horizontalBox.addEventListener('pointerup', onPointerUp);

    horizontalBox.addEventListener('pointerleave', () => {
        currentImage.style.cursor = 'default';
    });

    verticalBox.addEventListener('pointerleave', () => {
        verticalBox.querySelectorAll('.reader-vertical-image').forEach((img) => {
            img.style.cursor = 'default';
        });
    });

    verticalBox.addEventListener('pointerdown', onPointerDown, { passive: true });
    verticalBox.addEventListener('pointermove', onPointerMove, { passive: true });
    verticalBox.addEventListener('pointerup', onPointerUp);

    if (readerZonePrev) {
        readerZonePrev.addEventListener('click', (e) => {
            e.preventDefault();
            if (settings.invertClicks) {
                nextPage();
            } else {
                prevPage();
            }
        });
    }

    if (readerZoneNext) {
        readerZoneNext.addEventListener('click', (e) => {
            e.preventDefault();
            if (settings.invertClicks) {
                prevPage();
            } else {
                nextPage();
            }
        });
    }

    settingsBtn.addEventListener('click', () => toggleSettings(true));
    settingsClose.addEventListener('click', () => toggleSettings(false));

    settingsPanel.addEventListener('click', (e) => {
        if (e.target === settingsPanel) toggleSettings(false);
    });

    themeBtn.addEventListener('click', toggleTheme);

    document.addEventListener('comic:collections-updated', (e) => {
        const detail = e.detail || {};
        if (Number(detail.comicId) !== Number(app.dataset.comicId)) return;

        if (collectionBtn) {
            collectionBtn.classList.toggle('is-bookmarked', !!detail.inCollections);
        }
    });

    if (collectionBtn && !isLogged) {
        collectionBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            openAuthRequiredModalFallback();
        });
    }

    complaintBtn.addEventListener('click', () => {
        if (!isLogged) {
            openAuthRequiredModalFallback();
            return;
        }

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

    if (trClose) {
        trClose.addEventListener('click', closeTranslationsModal);
    }

    if (trModal) {
        trModal.addEventListener('click', (e) => {
            if (e.target === trModal) {
                closeTranslationsModal();
            }
        });
    }

    if (trLangSelect) {
        trLangSelect.addEventListener('change', () => {
            state.trPage = 0;
            loadTranslations(false);
        });
    }

    if (trMore) {
        trMore.addEventListener('click', () => {
            state.trPage++;
            loadTranslations(true);
        });
    }

    settingReadingMode.addEventListener('change', () => {
        switchReadingMode(settingReadingMode.value);
    });

    settingFitMode.addEventListener('change', () => {
        settings.fitMode = settingFitMode.value;
        saveSettings();
        showReaderLoading('Применяем настройки…');
        applyReaderLayout(state.currentPage);
    });

    settingImageWidth.addEventListener('input', () => {
        settings.imageWidth = Number(settingImageWidth.value);
        saveSettings();
        applyReaderLayout(state.currentPage);
    });

    settingVerticalGap.addEventListener('input', () => {
        settings.verticalGap = Number(settingVerticalGap.value);
        saveSettings();
        applyReaderLayout(state.currentPage);
    });

    settingClickZones.addEventListener('change', () => {
        settings.clickZones = settingClickZones.value;
        saveSettings();
        hideSettingsNotice();
        syncReaderZones();
    });

    settingInvertClicks.addEventListener('change', () => {
        settings.invertClicks = settingInvertClicks.checked;
        saveSettings();
        hideSettingsNotice();
    });

    settingTopbarVisible.addEventListener('change', () => {
        settings.topbarVisible = settingTopbarVisible.checked;
        saveSettings();
        applyReaderLayout(state.currentPage);
    });

    settingCounterVisible.addEventListener('change', () => {
        settings.counterVisible = settingCounterVisible.checked;
        saveSettings();
        applyReaderLayout(state.currentPage);
    });

    bindKeyCapture(settingPrevKey, 'prevKey');
    bindKeyCapture(settingNextKey, 'nextKey');
    bindKeyCapture(settingSettingsKey, 'settingsKey');

    document.addEventListener('keydown', (e) => {
        if (e.code === 'Escape' && trModal && !trModal.hidden) {
            closeTranslationsModal();
            return;
        }

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
    }, { passive: true });

    window.addEventListener('resize', () => {
        window.requestAnimationFrame(() => {
            syncTopbarOffset();

            if (settings.fitMode === 'height' || settings.readingMode === 'vertical') {
                applyReaderLayout(state.currentPage);
                return;
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

    window.addEventListener('pagehide', saveProgressNow);
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') {
            saveProgressNow();
        }
    });

    syncTopbarOffset();
})();