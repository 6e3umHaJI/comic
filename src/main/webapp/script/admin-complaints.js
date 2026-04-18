(() => {
    function getNodes() {
        return {
            page: document.getElementById('adminComplaintsPage'),
            content: document.getElementById('adminComplaintsContent')
        };
    }

    function setActiveScope(scope) {
        document.querySelectorAll('.admin-complaints-tab-btn').forEach((button) => {
            button.classList.toggle('active', button.dataset.scope === scope);
        });
    }

    function syncScopeFromContent() {
        const { page, content } = getNodes();
        if (!page || !content) {
            return;
        }

        const state = content.querySelector('.admin-complaints-state');
        if (!state) {
            return;
        }

        const scope = state.dataset.scope || 'TRANSLATION';
        page.dataset.scope = scope;
        setActiveScope(scope);
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
                syncScopeFromContent();

                if (pushState) {
                    window.history.pushState({}, '', url);
                }
            })
            .catch(() => {
                content.innerHTML = '<div class="admin-complaints-empty">Ошибка загрузки.</div>';
            });
    }

    document.addEventListener('click', (event) => {
        const { page } = getNodes();
        if (!page) {
            return;
        }

        const tabButton = event.target.closest('.admin-complaints-tab-btn');
        if (tabButton) {
            event.preventDefault();

            const currentUrl = new URL(window.location.href);
            currentUrl.searchParams.set('scope', tabButton.dataset.scope);
            currentUrl.searchParams.set('page', '0');
            currentUrl.searchParams.delete('typeId');

            loadPartial(currentUrl, true);
            return;
        }

        const paginationLink = event.target.closest('.pagination a');
        if (paginationLink && paginationLink.href) {
            event.preventDefault();
            loadPartial(new URL(paginationLink.href, window.location.origin), true);
            return;
        }
    });

    document.addEventListener('submit', (event) => {
        const { page } = getNodes();
        if (!page) {
            return;
        }

        const filterForm = event.target.closest('.admin-complaints-ajax-form');
        if (filterForm) {
            event.preventDefault();

            const url = new URL(filterForm.action || window.location.pathname, window.location.origin);
            const formData = new FormData(filterForm);

            formData.forEach((value, key) => {
                const stringValue = String(value).trim();

                if (stringValue.length > 0) {
                    url.searchParams.set(key, stringValue);
                } else {
                    url.searchParams.delete(key);
                }
            });

            url.searchParams.set('page', '0');
            loadPartial(url, true);
            return;
        }
    });

    document.addEventListener('change', (event) => {
        const select = event.target.closest('.js-admin-complaint-status-select');
        if (!select) {
            return;
        }

        const form = select.closest('.admin-complaint-status-form');
        const confirmBtn = form ? form.querySelector('.js-admin-complaint-confirm-btn') : null;
        if (!form || !confirmBtn) {
            return;
        }

        const initialValue = String(select.dataset.initialValue || '');
        const currentValue = String(select.value || '');

        confirmBtn.classList.toggle('hidden', initialValue === currentValue);
    });

    window.addEventListener('popstate', () => {
        const { page } = getNodes();
        if (!page) {
            return;
        }

        loadPartial(new URL(window.location.href), false);
    });

    syncScopeFromContent();
})();
