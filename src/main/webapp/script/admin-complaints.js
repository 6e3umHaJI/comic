(() => {
    const LIMITS = {
        searchQuery: 255
    };

    function trimToMax(value, maxLength) {
        return String(value ?? '').trim().slice(0, maxLength);
    }

    function getNodes() {
        return {
            page: document.getElementById('adminComplaintsPage'),
            content: document.getElementById('adminComplaintsContent')
        };
    }

    function applyComplaintSearchLimits(scope = document) {
        const input = scope.querySelector('#complaintSearchQ');
        if (!input) {
            return;
        }

        input.maxLength = LIMITS.searchQuery;
        input.value = trimToMax(input.value, LIMITS.searchQuery);

        if (input.dataset.limitBound === 'true') {
            return;
        }

        input.dataset.limitBound = 'true';

        input.addEventListener('input', () => {
            input.value = trimToMax(input.value, LIMITS.searchQuery);
        });

        input.addEventListener('blur', () => {
            input.value = trimToMax(input.value, LIMITS.searchQuery);
        });
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
        applyComplaintSearchLimits(content);

        content.querySelectorAll('.js-admin-complaint-status-select').forEach((select) => {
            const form = select.closest('.admin-complaint-status-form');
            const confirmBtn = form ? form.querySelector('.js-admin-complaint-confirm-btn') : null;
            if (!confirmBtn) {
                return;
            }

            const initialValue = String(select.dataset.initialValue || '');
            const currentValue = String(select.value || '');
            confirmBtn.classList.toggle('hidden', initialValue === currentValue);
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
                syncScopeFromContent();

                if (pushState) {
                    window.history.pushState({}, '', url);
                }
            })
            .catch(() => {
                content.innerHTML = '<div class="admin-complaints-empty">Ошибка загрузки.</div>';
            });
    }

    function updateStatusBadge(card, statusName) {
        const badge = card.querySelector('.admin-complaint-status-badge');
        if (!badge) {
            return;
        }

        badge.textContent = statusName;
        badge.classList.remove('is-pending', 'is-review', 'is-success', 'is-rejected');

        if (statusName === 'Ожидание') {
            badge.classList.add('is-pending');
        } else if (statusName === 'На рассмотрении') {
            badge.classList.add('is-review');
        } else if (statusName === 'Решена') {
            badge.classList.add('is-success');
        } else if (statusName === 'Отклонена') {
            badge.classList.add('is-rejected');
        }
    }

    function showEmptyStateIfNeeded() {
        const { content } = getNodes();
        if (!content) {
            return;
        }

        const list = content.querySelector('#adminComplaintsList');
        if (!list) {
            return;
        }

        if (list.querySelector('.admin-complaint-card')) {
            return;
        }

        const emptyState = document.createElement('div');
        emptyState.className = 'admin-complaints-empty';
        emptyState.textContent = 'По выбранным параметрам жалоб не найдено.';
        list.replaceWith(emptyState);
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

            const searchInput = filterForm.querySelector('input[name="q"]');
            if (searchInput) {
                searchInput.value = trimToMax(searchInput.value, LIMITS.searchQuery);
            }

            const url = new URL(filterForm.action || window.location.pathname, window.location.origin);
            const formData = new FormData(filterForm);

            formData.forEach((value, key) => {
                const stringValue = key === 'q'
                    ? trimToMax(value, LIMITS.searchQuery)
                    : String(value).trim();

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

        const statusForm = event.target.closest('.admin-complaint-status-form');
        if (statusForm) {
            event.preventDefault();

            const select = statusForm.querySelector('.js-admin-complaint-status-select');
            const confirmBtn = statusForm.querySelector('.js-admin-complaint-confirm-btn');
            const card = statusForm.closest('.admin-complaint-card');
            if (!select || !confirmBtn || !card) {
                return;
            }

            confirmBtn.disabled = true;

            fetch(statusForm.action, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams({
                    complaintId: statusForm.querySelector('input[name="complaintId"]').value,
                    statusId: select.value
                }).toString()
            })
                .then((response) => response.json())
                .then((json) => {
                    if (!json || !json.success) {
                        throw new Error(json?.message || 'Не удалось обновить статус.');
                    }

                    if (json.removed) {
                        card.remove();
                        showEmptyStateIfNeeded();
                        return;
                    }

                    updateStatusBadge(card, json.statusName);
                    select.dataset.initialValue = String(json.statusId);
                    confirmBtn.classList.add('hidden');
                })
                .catch(() => {})
                .finally(() => {
                    confirmBtn.disabled = false;
                });
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

    applyComplaintSearchLimits(document);
    syncScopeFromContent();
})();
