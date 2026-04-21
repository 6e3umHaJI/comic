(() => {
    const LIMITS = {
        query: 255
    };

    const modal = document.getElementById('quickSearchModal');
    if (!modal) return;

    const input = document.getElementById('quickSearchInput');
    const closeBtn = document.getElementById('quickSearchClose');
    const message = document.getElementById('quickSearchMessage');
    const results = document.getElementById('quickSearchResults');
    const footer = document.getElementById('quickSearchFooter');
    const moreBtn = document.getElementById('quickSearchMoreBtn');
    const catalogLink = document.getElementById('quickSearchCatalogLink');

    const searchUrl = modal.dataset.searchUrl;
    const catalogApplyUrl = modal.dataset.catalogApplyUrl;

    const PAGE_SIZE = 20;
    const MAX_INLINE_RESULTS = 100;

    const state = {
        query: '',
        page: 0,
        total: 0,
        loaded: 0,
        requestId: 0
    };

    function cutToMax(value, maxLength = LIMITS.query) {
        return String(value ?? '').slice(0, maxLength);
    }

    function normalizeForSubmit(value, maxLength = LIMITS.query) {
        return String(value ?? '').trim().slice(0, maxLength);
    }

    function setBodyLock(locked) {
        document.body.classList.toggle('quick-search-open', locked);
        document.documentElement.classList.toggle('quick-search-open', locked);
    }

    function resetState() {
        state.query = '';
        state.page = 0;
        state.total = 0;
        state.loaded = 0;
        state.requestId += 1;
    }

    function resetModal() {
        resetState();
        input.value = '';
        results.innerHTML = '';
        results.hidden = true;
        footer.hidden = true;
        moreBtn.hidden = true;
        catalogLink.hidden = true;
        message.hidden = false;
        message.textContent = 'Введите название или оригинальное название.';
    }

    function openModal() {
        resetModal();
        input.maxLength = LIMITS.query;
        modal.hidden = false;
        setBodyLock(true);
        requestAnimationFrame(() => input.focus());
    }

    function closeModal() {
        modal.hidden = true;
        setBodyLock(false);
        resetModal();
    }

    function setMessage(text) {
        message.hidden = false;
        message.textContent = text;
    }

    function hideMessage() {
        message.hidden = true;
    }

    function escapeText(value) {
        return value ?? '';
    }

    function buildCoverUrl(cover) {
        return `/assets/covers/${encodeURIComponent(cover ?? '')}`;
    }

    function buildComicUrl(id) {
        return `/comics/${id}`;
    }

    function renderItems(items, append) {
        if (!append) {
            results.innerHTML = '';
        }

        items.forEach((item) => {
            const link = document.createElement('a');
            link.className = 'quick-search-item';
            link.href = buildComicUrl(item.id);

            const img = document.createElement('img');
            img.className = 'quick-search-cover';
            img.src = buildCoverUrl(item.cover);
            img.alt = escapeText(item.title);

            const content = document.createElement('div');
            content.className = 'quick-search-item-content';

            const title = document.createElement('div');
            title.className = 'quick-search-item-title';
            title.textContent = escapeText(item.title);

            const original = document.createElement('div');
            original.className = 'quick-search-item-original';
            original.textContent = item.originalTitle && item.originalTitle.trim()
                ? item.originalTitle
                : 'Оригинальное название отсутствует';

            const desc = document.createElement('div');
            desc.className = 'quick-search-item-desc';
            desc.textContent = item.shortDescription && item.shortDescription.trim()
                ? item.shortDescription
                : 'Краткое описание отсутствует.';

            content.appendChild(title);
            content.appendChild(original);
            content.appendChild(desc);

            link.appendChild(img);
            link.appendChild(content);

            results.appendChild(link);
        });

        results.hidden = results.children.length === 0;
    }

    function renderFooter() {
        footer.hidden = true;
        moreBtn.hidden = true;
        catalogLink.hidden = true;

        if (!state.query) return;

        const maxInline = Math.min(state.total, MAX_INLINE_RESULTS);
        const remainingInline = maxInline - state.loaded;

        if (remainingInline > 0) {
            footer.hidden = false;
            moreBtn.hidden = false;
            moreBtn.textContent = `Показать еще ${Math.min(PAGE_SIZE, remainingInline)}`;
            return;
        }

        if (state.total > MAX_INLINE_RESULTS) {
            footer.hidden = false;
            catalogLink.hidden = false;
            catalogLink.href = `${catalogApplyUrl}?filter=search&value=${encodeURIComponent(state.query)}`;
        }
    }

    function applyResponse(data, append) {
        const items = Array.isArray(data.items) ? data.items : [];

        state.total = Number(data.total || 0);
        state.loaded = append ? state.loaded + items.length : items.length;
        state.page = Number(data.page || 0) + 1;

        if (!items.length && !append) {
            results.innerHTML = '';
            results.hidden = true;
            footer.hidden = true;
            setMessage('Ничего не найдено…');
            return;
        }

        hideMessage();
        renderItems(items, append);
        renderFooter();
    }

    function fetchSearchResults(append = false) {
        const query = normalizeForSubmit(input.value);

        if (!query) {
            results.innerHTML = '';
            results.hidden = true;
            footer.hidden = true;
            state.query = '';
            state.page = 0;
            state.total = 0;
            state.loaded = 0;
            setMessage('Введите название или оригинальное название.');
            return;
        }

        if (!append) {
            state.query = query;
            state.page = 0;
            state.total = 0;
            state.loaded = 0;
            setMessage('Ищем…');
        }

        const requestId = ++state.requestId;
        const page = append ? state.page : 0;

        fetch(`${searchUrl}?q=${encodeURIComponent(query)}&page=${page}`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then((r) => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then((data) => {
                if (requestId !== state.requestId) return;
                applyResponse(data, append);
            })
            .catch((err) => {
                console.error('Ошибка быстрого поиска:', err);
                results.innerHTML = '';
                results.hidden = true;
                footer.hidden = true;
                setMessage('Не удалось выполнить поиск.');
            });
    }

    function debounce(fn, delay) {
        let timer = null;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), delay);
        };
    }

    const debouncedSearch = debounce(() => fetchSearchResults(false), 300);

    document.addEventListener('click', (e) => {
        const openTrigger = e.target.closest('[data-quick-search-open="true"]');
        if (openTrigger) {
            e.preventDefault();
            openModal();
            return;
        }

        if (e.target === modal) {
            closeModal();
        }
    });

    closeBtn?.addEventListener('click', closeModal);

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && !modal.hidden) {
            closeModal();
        }
    });

    input?.addEventListener('input', () => {
        input.value = cutToMax(input.value);
        debouncedSearch();
    });

    input?.addEventListener('blur', () => {
        input.value = normalizeForSubmit(input.value);
    });

    moreBtn?.addEventListener('click', () => {
        fetchSearchResults(true);
    });
})();
