(() => {
    const LIMITS = {
        searchQuery: 255
    };

    function getNodes() {
        return {
            page: document.getElementById('translationCabinetPage'),
            content: document.getElementById('translationCabinetContent')
        };
    }

    function cutToMax(value, maxLength) {
        return String(value ?? '').slice(0, maxLength);
    }

    function normalizeForSubmit(value, maxLength) {
        return String(value ?? '').trim().slice(0, maxLength);
    }

    function applySearchLimits(scope = document) {
        const input = scope.querySelector('#translationCabinetQ');
        if (!input) {
            return;
        }

        input.maxLength = LIMITS.searchQuery;
        input.value = cutToMax(input.value, LIMITS.searchQuery);

        if (input.dataset.limitBound === 'true') {
            return;
        }

        input.dataset.limitBound = 'true';

        input.addEventListener('input', () => {
            input.value = cutToMax(input.value, LIMITS.searchQuery);
        });

        input.addEventListener('blur', () => {
            input.value = normalizeForSubmit(input.value, LIMITS.searchQuery);
        });
    }

    function loadPartial(url, pushState) {
        const { content } = getNodes();
        if (!content) {
            return;
        }

        fetch(url.toString(), {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.text();
            })
            .then((html) => {
                content.innerHTML = html;
                applySearchLimits(content);
                if (pushState) {
                    window.history.pushState({}, '', url);
                }
            })
            .catch(() => {
                content.innerHTML = '<div class="translation-cabinet-empty">Ошибка загрузки.</div>';
            });
    }

    document.addEventListener('click', (event) => {
        const { page } = getNodes();
        if (!page) {
            return;
        }

        const paginationLink = event.target.closest('.pagination a');
        if (paginationLink && paginationLink.href) {
            event.preventDefault();
            loadPartial(new URL(paginationLink.href, window.location.origin), true);
        }
    });

    document.addEventListener('submit', (event) => {
        const { page } = getNodes();
        if (!page) {
            return;
        }

        const filterForm = event.target.closest('.translation-cabinet-ajax-form');
        if (!filterForm) {
            return;
        }

        event.preventDefault();

        const searchInput = filterForm.querySelector('input[name="q"]');
        if (searchInput) {
            searchInput.value = normalizeForSubmit(searchInput.value, LIMITS.searchQuery);
        }

        const url = new URL(filterForm.action || window.location.pathname, window.location.origin);
        const formData = new FormData(filterForm);

        formData.forEach((value, key) => {
            const normalized = key === 'q'
                ? normalizeForSubmit(value, LIMITS.searchQuery)
                : String(value ?? '').trim();

            if (normalized.length > 0) {
                url.searchParams.set(key, normalized);
            } else {
                url.searchParams.delete(key);
            }
        });

        url.searchParams.set('page', '0');
        loadPartial(url, true);
    });

    window.addEventListener('popstate', () => {
        const { page } = getNodes();
        if (!page) {
            return;
        }
        loadPartial(new URL(window.location.href), false);
    });

    applySearchLimits(document);
})();
