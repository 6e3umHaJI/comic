(() => {
    if (window.__comicNotificationsScriptInitialized) {
        return;
    }
    window.__comicNotificationsScriptInitialized = true;

    const LIMITS = {
        searchQuery: 255
    };

    function trimToMax(value, maxLength) {
        return String(value ?? '').trim().slice(0, maxLength);
    }

    function openAuthRequiredModal() {
        const modal = document.getElementById('authRequiredModal');
        if (modal) {
            modal.classList.add('visible');
        }
    }

    function setMaskIcon(icon, url) {
        if (!icon || !url) {
            return;
        }

        icon.style.webkitMaskImage = `url('${url}')`;
        icon.style.maskImage = `url('${url}')`;
    }

    function formatCount(count) {
        const value = Number.isFinite(Number(count)) ? Number(count) : 0;
        return value > 99 ? '99+' : String(Math.max(value, 0));
    }

    function updateHeaderNotificationState(totalCount, unreadCount) {
        const numericTotalCount = Number.isFinite(Number(totalCount)) ? Number(totalCount) : 0;
        const numericUnreadCount = Number.isFinite(Number(unreadCount)) ? Number(unreadCount) : 0;

        const icon = document.querySelector('.js-header-notification-icon');
        if (icon) {
            const onUrl = icon.dataset.onIconUrl;
            const offUrl = icon.dataset.offIconUrl;
            setMaskIcon(icon, numericUnreadCount > 0 ? onUrl : offUrl);
        }

        const badge = document.querySelector('.js-profile-notification-count');
        if (badge) {
            badge.textContent = formatCount(numericTotalCount);
        }
    }

    function updateNotificationButton(button, subscribed) {
        button.classList.toggle('is-active', subscribed);
        button.dataset.subscribed = String(subscribed);

        const icon = button.querySelector('.js-notification-toggle-icon');
        const onUrl = button.dataset.iconOnUrl;
        const offUrl = button.dataset.iconOffUrl;

        setMaskIcon(icon, subscribed ? onUrl : offUrl);

        const title = subscribed ? 'Отключить оповещения' : 'Включить оповещения';
        button.setAttribute('title', title);
        button.setAttribute('aria-label', title);
    }

    function updateAllNotificationButtons(comicId, subscribed) {
        document.querySelectorAll(`.js-notification-toggle[data-comic-id="${comicId}"]`)
            .forEach((button) => updateNotificationButton(button, subscribed));
    }

    function setButtonsPending(comicId, pending) {
        document.querySelectorAll(`.js-notification-toggle[data-comic-id="${comicId}"]`)
            .forEach((button) => {
                button.disabled = pending;
                button.dataset.notificationRequestPending = pending ? 'true' : 'false';
            });
    }

    function getNotificationsPageNodes() {
        return {
            page: document.getElementById('notificationsPage'),
            content: document.getElementById('notificationsContent')
        };
    }

    function applyNotificationsSearchLimits(scope = document) {
        const input = scope.querySelector('.notifications-search-form input[name="q"]');
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

    function setActiveTab(tab) {
        document.querySelectorAll('.notifications-tab-btn').forEach((button) => {
            button.classList.toggle('active', button.dataset.tab === tab);
        });
    }

    function syncHeaderFromContent() {
        const { content, page } = getNotificationsPageNodes();
        if (!content || !page) {
            return;
        }

        const stateNode = content.querySelector('.js-notifications-state');
        if (!stateNode) {
            return;
        }

        const totalCount = Number(stateNode.dataset.notificationCount || 0);
        const unreadCount = Number(stateNode.dataset.unreadNotificationCount || 0);
        const tab = stateNode.dataset.tab || 'feed';

        updateHeaderNotificationState(totalCount, unreadCount);
        setActiveTab(tab);
        page.dataset.tab = tab;
        applyNotificationsSearchLimits(content);
    }

    function resolveCurrentPageIndexFromContent(content) {
        if (!content) {
            return 0;
        }

        const activePageLink = content.querySelector('.pagination a.active-page');
        if (activePageLink) {
            const activePageNumber = parseInt(activePageLink.textContent.trim(), 10);
            if (Number.isFinite(activePageNumber) && activePageNumber > 0) {
                return activePageNumber - 1;
            }
        }

        const url = new URL(window.location.href);
        const raw = parseInt(url.searchParams.get('page') || '0', 10);
        return Number.isFinite(raw) && raw >= 0 ? raw : 0;
    }

    function loadNotificationsPartial(url, pushState) {
        const { content } = getNotificationsPageNodes();
        if (!content) {
            return;
        }

        fetch(url.toString(), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.text();
            })
            .then((html) => {
                content.innerHTML = html;
                syncHeaderFromContent();

                if (pushState) {
                    window.history.pushState({}, '', url);
                }
            })
            .catch(() => {
                content.innerHTML = '<div class="notification-empty">Ошибка загрузки.</div>';
            });
    }

    document.addEventListener('click', (event) => {
        const toggleButton = event.target.closest('.js-notification-toggle');
        if (toggleButton) {
            event.preventDefault();

            if (toggleButton.dataset.notificationRequestPending === 'true') {
                return;
            }

            const isAuthenticated = toggleButton.dataset.authenticated === 'true';
            if (!isAuthenticated) {
                openAuthRequiredModal();
                return;
            }

            const comicId = toggleButton.dataset.comicId;
            const toggleUrl = toggleButton.dataset.toggleUrl || '/notifications/toggle';

            setButtonsPending(comicId, true);

            fetch(toggleUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams({ comicId }).toString()
            })
                .then((response) => {
                    if (response.status === 401 || response.status === 403) {
                        openAuthRequiredModal();
                        throw new Error('unauthorized');
                    }

                    return response.json();
                })
                .then((json) => {
                    if (!json || !json.success) {
                        throw new Error(json?.message || 'Не удалось обновить оповещения');
                    }

                    updateAllNotificationButtons(String(json.comicId), Boolean(json.subscribed));
                })
                .catch(() => {})
                .finally(() => {
                    setButtonsPending(comicId, false);
                });

            return;
        }

        const { page, content } = getNotificationsPageNodes();
        if (!page || !content) {
            return;
        }

        const tabButton = event.target.closest('.notifications-tab-btn');
        if (tabButton) {
            event.preventDefault();

            const url = new URL(window.location.href);
            const nextTab = tabButton.dataset.tab;

            url.searchParams.set('tab', nextTab);
            url.searchParams.set('page', '0');

            if (nextTab === 'feed') {
                url.searchParams.delete('q');
            }

            loadNotificationsPartial(url, true);
            return;
        }

        const paginationLink = event.target.closest('.pagination a');
        if (paginationLink && paginationLink.href) {
            event.preventDefault();
            loadNotificationsPartial(new URL(paginationLink.href, window.location.origin), true);
            return;
        }

        const deleteButton = event.target.closest('.js-notification-delete');
        if (deleteButton) {
            event.preventDefault();

            const deleteUrl = deleteButton.dataset.deleteUrl;
            const notificationId = deleteButton.dataset.notificationId;

            const currentPageIndex = resolveCurrentPageIndexFromContent(content);
            const currentUrl = new URL(window.location.href);
            const cardsOnPage = content.querySelectorAll('.notification-card').length;

            currentUrl.searchParams.set('page', String(currentPageIndex));

            if (cardsOnPage === 1 && currentPageIndex > 0) {
                currentUrl.searchParams.set('page', String(currentPageIndex - 1));
            }

            fetch(deleteUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams({ notificationId }).toString()
            })
                .then((response) => response.json())
                .then((json) => {
                    if (!json || !json.success) {
                        throw new Error('delete failed');
                    }

                    updateHeaderNotificationState(
                        Number(json.notificationCount || 0),
                        Number(json.unreadNotificationCount || 0)
                    );

                    loadNotificationsPartial(currentUrl, true);
                })
                .catch(() => {});

            return;
        }
    });

    document.addEventListener('submit', (event) => {
        const form = event.target.closest('.notifications-ajax-form');
        if (!form) {
            return;
        }

        const { page } = getNotificationsPageNodes();
        if (!page) {
            return;
        }

        event.preventDefault();

        const searchInput = form.querySelector('input[name="q"]');
        if (searchInput) {
            searchInput.value = trimToMax(searchInput.value, LIMITS.searchQuery);
        }

        const url = new URL(form.action || window.location.pathname, window.location.origin);
        const formData = new FormData(form);

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

        loadNotificationsPartial(url, true);
    });

    window.addEventListener('popstate', () => {
        const { page } = getNotificationsPageNodes();
        if (!page) {
            return;
        }

        loadNotificationsPartial(new URL(window.location.href), false);
    });

    applyNotificationsSearchLimits(document);
    syncHeaderFromContent();
})();