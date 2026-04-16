(function () {
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

    function updateHeaderNotificationState(count) {
        const icon = document.querySelector('.js-header-notification-icon');
        if (icon) {
            const onUrl = icon.dataset.onIconUrl;
            const offUrl = icon.dataset.offIconUrl;
            setMaskIcon(icon, Number(count) > 0 ? onUrl : offUrl);
        }

        const badge = document.querySelector('.js-profile-notification-count');
        if (badge) {
            badge.textContent = formatCount(count);
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

    document.addEventListener('click', (event) => {
        const button = event.target.closest('.js-notification-toggle');
        if (!button) {
            return;
        }

        event.preventDefault();

        const isAuthenticated = button.dataset.authenticated === 'true';
        if (!isAuthenticated) {
            openAuthRequiredModal();
            return;
        }

        const comicId = button.dataset.comicId;
        const toggleUrl = button.dataset.toggleUrl || '/notifications/toggle';

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
            .catch(() => {});
    });

    const page = document.getElementById('notificationsPage');
    const content = document.getElementById('notificationsContent');

    if (!page || !content) {
        return;
    }

    function setActiveTab(tab) {
        document.querySelectorAll('.notifications-tab-btn').forEach((button) => {
            button.classList.toggle('active', button.dataset.tab === tab);
        });
        page.dataset.tab = tab;
    }

    function syncHeaderFromContent() {
        const stateNode = content.querySelector('.js-notifications-state');
        if (!stateNode) {
            return;
        }

        const count = Number(stateNode.dataset.notificationCount || 0);
        const tab = stateNode.dataset.tab || 'feed';

        updateHeaderNotificationState(count);
        setActiveTab(tab);
    }

    function loadPartial(url, pushState) {
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

    document.querySelectorAll('.notifications-tab-btn').forEach((button) => {
        button.addEventListener('click', () => {
            const url = new URL(window.location.href);
            const nextTab = button.dataset.tab;

            url.searchParams.set('tab', nextTab);
            url.searchParams.set('page', '0');

            if (nextTab === 'feed') {
                url.searchParams.delete('q');
            }

            loadPartial(url, true);
        });
    });

    content.addEventListener('submit', (event) => {
        const form = event.target.closest('.notifications-ajax-form');
        if (!form) {
            return;
        }

        event.preventDefault();

        const url = new URL(form.action || window.location.pathname, window.location.origin);
        const formData = new FormData(form);

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
    });

    content.addEventListener('click', (event) => {
        const deleteButton = event.target.closest('.js-notification-delete');
        if (deleteButton) {
            event.preventDefault();

            const deleteUrl = deleteButton.dataset.deleteUrl;
            const notificationId = deleteButton.dataset.notificationId;
            const currentPageUrl = new URL(window.location.href);
            const cardsOnPage = content.querySelectorAll('.notification-card').length;
            const currentPage = parseInt(currentPageUrl.searchParams.get('page') || '0', 10);

            if (cardsOnPage === 1 && currentPage > 0) {
                currentPageUrl.searchParams.set('page', String(currentPage - 1));
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

                    updateHeaderNotificationState(Number(json.notificationCount || 0));
                    loadPartial(currentPageUrl, false);
                })
                .catch(() => {});

            return;
        }

        const paginationLink = event.target.closest('.pagination a');
        if (paginationLink && paginationLink.href) {
            event.preventDefault();
            loadPartial(new URL(paginationLink.href, window.location.origin), true);
        }
    });

    window.addEventListener('popstate', () => {
        loadPartial(new URL(window.location.href), false);
    });

    syncHeaderFromContent();
});