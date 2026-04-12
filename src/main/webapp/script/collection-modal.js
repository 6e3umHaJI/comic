(function () {
    function authRequiredModalOpen() {
        const modal = document.getElementById('authRequiredModal');
        if (modal) modal.classList.add('visible');
    }

    function showModalNotice(message, type = 'error') {
        const el = document.getElementById('collectionModalNotice');
        if (!el) return;

        el.hidden = false;
        el.textContent = message;
        el.className = 'collection-inline-notice ' + (type === 'success'
            ? 'collection-inline-notice-success'
            : 'collection-inline-notice-error');
    }

    function hideModalNotice() {
        const el = document.getElementById('collectionModalNotice');
        if (!el) return;
        el.hidden = true;
        el.textContent = '';
    }

    function setCollectionButtonsState(comicId, inCollections) {
        document.querySelectorAll(`.js-collection-toggle[data-comic-id="${comicId}"]`).forEach(btn => {
            btn.classList.toggle('is-active', inCollections);

            const text = btn.querySelector('.js-collection-toggle-text');
            if (text) {
                text.textContent = inCollections ? 'В коллекции' : 'Добавить в коллекцию';
            }

            if (btn.dataset.iconOnly === 'true') {
                btn.classList.toggle('is-bookmarked', inCollections);
            }
        });
    }

    function buildParams(obj) {
        const params = new URLSearchParams();
        Object.entries(obj).forEach(([key, value]) => {
            if (Array.isArray(value)) {
                value.forEach(v => params.append(key, v));
            } else if (value !== undefined && value !== null && value !== '') {
                params.append(key, value);
            }
        });
        return params;
    }

    function openCollectionModal(comicId) {
        const modal = document.getElementById('collectionModal');
        const body = document.getElementById('collectionModalBody');
        if (!modal || !body) return;

        fetch(`/collections/comic-modal?comicId=${comicId}`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(r => {
                if (r.status === 401) {
                    authRequiredModalOpen();
                    throw new Error('unauthorized');
                }
                return r.text();
            })
            .then(html => {
                body.innerHTML = html;
                modal.hidden = false;
                document.body.style.overflow = 'hidden';
                bindCollectionModalInner();
            })
            .catch(() => {});
    }

    function closeCollectionModal() {
        const modal = document.getElementById('collectionModal');
        const body = document.getElementById('collectionModalBody');
        if (modal) modal.hidden = true;
        if (body) body.innerHTML = '';

        document.body.style.overflow = '';
    }

    function updateSelectedState(button) {
        button.classList.toggle('selected');
        button.setAttribute('aria-pressed', button.classList.contains('selected') ? 'true' : 'false');
    }

    function getSelectedSectionIds() {
        return Array.from(document.querySelectorAll('.collection-choice-btn.selected'))
            .map(btn => btn.dataset.sectionId);
    }

    function bindCollectionModalInner() {
        const root = document.querySelector('.collection-picker');
        if (!root) return;

        const comicId = root.dataset.comicId;
        const createBtn = document.getElementById('createCollectionFromModalBtn');
        const saveBtn = document.getElementById('saveComicCollectionsBtn');
        const removeBtn = document.getElementById('removeComicFromAllBtn');
        const nameInput = document.getElementById('newCollectionName');

        hideModalNotice();

        document.querySelectorAll('.collection-choice-btn').forEach(btn => {
            btn.addEventListener('click', () => updateSelectedState(btn));
        });

        createBtn?.addEventListener('click', () => {
            const name = (nameInput?.value || '').trim();
            if (!name) {
                showModalNotice('Введите название категории');
                return;
            }

            fetch('/collections/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: buildParams({ name }).toString()
            })
                .then(r => r.json())
                .then(json => {
                    if (!json.success) {
                        showModalNotice(json.message || 'Не удалось создать категорию');
                        return;
                    }
                    openCollectionModal(comicId);
                })
                .catch(() => showModalNotice('Не удалось создать категорию'));
        });

        saveBtn?.addEventListener('click', () => {
            const sectionIds = getSelectedSectionIds();

            fetch('/collections/comic-sync', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: buildParams({
                    comicId,
                    sectionIds
                }).toString()
            })
                .then(r => {
                    if (r.status === 401) {
                        authRequiredModalOpen();
                        throw new Error('unauthorized');
                    }
                    return r.json();
                })
                .then(json => {
                    if (!json.success) {
                        showModalNotice(json.message || 'Не удалось сохранить коллекцию');
                        return;
                    }

                    setCollectionButtonsState(comicId, json.inCollections);
                    closeCollectionModal();

                    if (typeof window.reloadCollectionsIfOpen === 'function') {
                        window.reloadCollectionsIfOpen();
                    }
                })
                .catch(() => showModalNotice('Не удалось сохранить коллекцию'));
        });

        removeBtn?.addEventListener('click', () => {
            fetch('/collections/comic-sync', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: buildParams({ comicId }).toString()
            })
                .then(r => {
                    if (r.status === 401) {
                        authRequiredModalOpen();
                        throw new Error('unauthorized');
                    }
                    return r.json();
                })
                .then(json => {
                    if (!json.success) {
                        showModalNotice(json.message || 'Не удалось удалить комикс из коллекций');
                        return;
                    }

                    setCollectionButtonsState(comicId, false);
                    closeCollectionModal();

                    if (typeof window.reloadCollectionsIfOpen === 'function') {
                        window.reloadCollectionsIfOpen();
                    }
                })
                .catch(() => showModalNotice('Не удалось удалить комикс из коллекций'));
        });
    }

    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.js-collection-toggle');
        if (!btn) return;

        e.preventDefault();

        const isAuthenticated = btn.dataset.authenticated === 'true';
        if (!isAuthenticated) {
            authRequiredModalOpen();
            return;
        }

        openCollectionModal(btn.dataset.comicId);
    });

    document.getElementById('collectionModalClose')?.addEventListener('click', closeCollectionModal);

    document.getElementById('collectionModal')?.addEventListener('click', (e) => {
        if (e.target.id === 'collectionModal') closeCollectionModal();
    });

    window.setCollectionButtonsState = setCollectionButtonsState;
})();
