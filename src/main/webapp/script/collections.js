const COLLECTIONS_LIMITS = {
    searchQuery: 255
};

function trimCollectionValue(value, maxLength = COLLECTIONS_LIMITS.searchQuery) {
    return String(value ?? '').trim().slice(0, maxLength);
}


function showInlineNotice(containerId, message, type = 'error') {
    const el = document.getElementById(containerId);
    if (!el) return;

    el.hidden = false;
    el.textContent = message;
    el.className = 'collections-inline-notice ' +
        (type === 'success'
            ? 'collections-inline-notice-success'
            : 'collections-inline-notice-error');
}

function hideInlineNotice(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;

    el.hidden = true;
    el.textContent = '';
}

function postFormUrlEncoded(url, data) {
    const params = new URLSearchParams();

    Object.entries(data).forEach(([key, value]) => {
        if (Array.isArray(value)) {
            value.forEach(v => params.append(key, v));
        } else if (value !== undefined && value !== null && value !== '') {
            params.append(key, value);
        }
    });

    return fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: params.toString()
    }).then(async r => await r.json());
}

function loadCollectionSection(sectionId = null, page = 0, viewMode = null, q = null, sortField = null, sortDirection = null) {
    const root = document.getElementById('collectionsRoot');
    const main = root?.querySelector('.collections-main');

    const resolvedViewMode = viewMode || main?.dataset.viewMode || 'card';
    const resolvedQuery = q !== null ? q : (main?.dataset.searchQuery || '');
    const resolvedSortField = sortField || main?.dataset.sortField || 'addedAt';
    const resolvedSortDirection = sortDirection || main?.dataset.sortDirection || 'desc';

    const params = new URLSearchParams();
    params.append('page', page);
    params.append('viewMode', resolvedViewMode);
    params.append('q', resolvedQuery);
    params.append('sortField', resolvedSortField);
    params.append('sortDirection', resolvedSortDirection);

    if (sectionId !== null && sectionId !== undefined && sectionId !== '') {
        params.append('sectionId', sectionId);
    }

    const url = `/collections?${params.toString()}`;
    window.history.replaceState({}, '', url);

    fetch(url, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
        .then(r => r.text())
        .then(html => {
            const rootNode = document.getElementById('collectionsRoot');
            if (!rootNode) return;

            rootNode.innerHTML = html;
            bindCollectionsPageEvents();
        });
}

function reloadCollectionsIfOpen(sectionId = null) {
    const root = document.getElementById('collectionsRoot');
    if (!root) return;

    const main = root.querySelector('.collections-main');
    const viewMode = main?.dataset.viewMode || 'card';
    const q = main?.dataset.searchQuery || '';
    const sortField = main?.dataset.sortField || 'addedAt';
    const sortDirection = main?.dataset.sortDirection || 'desc';
    const resolvedSectionId = sectionId ?? main?.dataset.activeSectionId ?? null;

    loadCollectionSection(resolvedSectionId, 0, viewMode, q, sortField, sortDirection);
}


window.reloadCollectionsIfOpen = reloadCollectionsIfOpen;
window.loadCollectionSection = loadCollectionSection;

function showTransferNotice(message) {
    const notice = document.getElementById('collectionTransferNotice');
    if (!notice) return;

    notice.hidden = false;
    notice.textContent = message;
    notice.className = 'collection-inline-notice collection-inline-notice-error';
}

function hideTransferNotice() {
    const notice = document.getElementById('collectionTransferNotice');
    if (!notice) return;

    notice.hidden = true;
    notice.textContent = '';
    notice.className = 'collection-inline-notice';
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function getSelectedComicIds() {
    return Array.from(document.querySelectorAll('.collection-comic-check:checked'))
        .map(i => Number(i.value))
        .filter(Number.isFinite);
}

function setCheckedState(checked) {
    document.querySelectorAll('.collection-comic-check').forEach(input => {
        input.checked = checked;
    });
}

function openTransferModal(title, action, sectionId, sections) {
    const modal = document.getElementById('collectionTransferModal');
    const body = document.getElementById('collectionTransferModalBody');

    if (!modal || !body) {
        showInlineNotice('collectionsMainNotice', 'Модальное окно не найдено');
        return;
    }

    const allowDeleteWithoutTransfer = action === 'delete-section';

    const deleteWithoutTransferItem = allowDeleteWithoutTransfer
        ? `
            <button type="button"
                    class="collection-choice-btn collection-choice-btn-danger"
                    data-delete-comics="true">
                <span class="collection-choice-name">Удалить категорию вместе с комиксами</span>
            </button>
        `
        : '';

    const items = deleteWithoutTransferItem + sections.map(section => `
        <button type="button"
                class="collection-choice-btn"
                data-transfer-target-id="${escapeHtml(section.id)}">
            <span class="collection-choice-name">${escapeHtml(section.name)}</span>
        </button>
    `).join('');

    body.innerHTML = `
        <div class="collection-picker">
            <h3>${escapeHtml(title)}</h3>

            <div id="collectionTransferNotice" class="collection-inline-notice" hidden></div>

            <div class="collection-picker-scroll">
                <div class="collection-picker-list">${items}</div>
            </div>

            <div class="collection-modal-actions">
                <button type="button" class="btn collection-save-btn" id="confirmTransferActionBtn">
                    Подтвердить
                </button>
            </div>
        </div>
    `;

    if (action === 'delete-section') {
        const deleteBtn = body.querySelector('[data-delete-comics="true"]');
        if (deleteBtn) {
            deleteBtn.classList.add('selected');
        }
    }


    let deleteComics = action === 'delete-section';
    let selectedTargetIds = [];


    body.querySelectorAll('[data-transfer-target-id], [data-delete-comics]').forEach(btn => {
        btn.addEventListener('click', () => {
            hideTransferNotice();

            if (btn.dataset.deleteComics === 'true') {
                body.querySelectorAll('[data-transfer-target-id]').forEach(b => b.classList.remove('selected'));
                btn.classList.toggle('selected');

                deleteComics = btn.classList.contains('selected');
                selectedTargetIds = [];
                return;
            }

            body.querySelectorAll('[data-delete-comics]').forEach(b => b.classList.remove('selected'));
            deleteComics = false;

            btn.classList.toggle('selected');

            selectedTargetIds = Array.from(body.querySelectorAll('[data-transfer-target-id].selected'))
                .map(b => Number(b.dataset.transferTargetId))
                .filter(Number.isFinite);
        });
    });

    document.getElementById('confirmTransferActionBtn')?.addEventListener('click', () => {
        if (action === 'delete-section') {
            if (!deleteComics && !selectedTargetIds.length) {
                showTransferNotice('Выберите хотя бы одну категорию или удаление без переноса');
                return;
            }

            postFormUrlEncoded('/collections/delete', {
                sectionId,
                targetSectionIds: selectedTargetIds,
                deleteComics
            }).then(json => {
                if (!json.success) {
                    showTransferNotice(json.message || 'Не удалось удалить категорию');
                    return;
                }

                closeTransferModal();
                reloadCollectionsIfOpen(null);
            }).catch(() => showTransferNotice('Не удалось удалить категорию'));

            return;
        }

        if (!selectedTargetIds.length) {
            showTransferNotice('Выберите хотя бы одну категорию');
            return;
        }

        const main = document.querySelector('.collections-main');
        const fromSectionId = Number(main?.dataset.activeSectionId);
        const checkedComicIds = getSelectedComicIds();

        postFormUrlEncoded('/collections/move', {
            fromSectionId,
            toSectionIds: selectedTargetIds,
            comicIds: checkedComicIds
        }).then(json => {
            if (!json.success) {
                showTransferNotice(json.message || 'Не удалось перенести тайтлы');
                return;
            }

            closeTransferModal();
            reloadCollectionsIfOpen(fromSectionId);
        }).catch(() => showTransferNotice('Не удалось перенести тайтлы'));
    });

    modal.hidden = false;
    document.body.style.overflow = 'hidden';
}

function closeTransferModal() {
    const modal = document.getElementById('collectionTransferModal');
    const body = document.getElementById('collectionTransferModalBody');

    if (modal) modal.hidden = true;
    if (body) body.innerHTML = '';

    document.body.style.overflow = '';
}

function bindCollectionsPageEvents() {
    hideInlineNotice('collectionsMainNotice');
    hideInlineNotice('collectionsSidebarNotice');

    document.querySelectorAll('.collection-tab').forEach(btn => {
        btn.addEventListener('click', () => {
            loadCollectionSection(btn.dataset.sectionId, 0);
        });
    });

    const collectionsSearchForm = document.getElementById('collectionsSearchForm');
    const collectionsSearchInput = document.getElementById('collectionsSearchInput');
    const collectionsSortField = document.getElementById('collectionsSortField');
    const collectionsSortDirection = document.getElementById('collectionsSortDirection');

    if (collectionsSearchInput) {
        collectionsSearchInput.maxLength = COLLECTIONS_LIMITS.searchQuery;
        collectionsSearchInput.value = trimCollectionValue(collectionsSearchInput.value);

        collectionsSearchInput.addEventListener('input', () => {
            collectionsSearchInput.value = trimCollectionValue(collectionsSearchInput.value);
        });

        collectionsSearchInput.addEventListener('blur', () => {
            collectionsSearchInput.value = trimCollectionValue(collectionsSearchInput.value);
        });
    }

    function applyCollectionsFilters() {
        const main = document.querySelector('.collections-main');
        const activeSectionId = main?.dataset.activeSectionId ?? null;
        const currentViewMode = main?.dataset.viewMode || 'card';

        loadCollectionSection(
            activeSectionId,
            0,
            currentViewMode,
            trimCollectionValue(collectionsSearchInput?.value || ''),
            collectionsSortField?.value || 'addedAt',
            collectionsSortDirection?.value || 'desc'
        );
    }

    collectionsSearchForm?.addEventListener('submit', (e) => {
        e.preventDefault();

        if (collectionsSearchInput) {
            collectionsSearchInput.value = trimCollectionValue(collectionsSearchInput.value);
        }

        applyCollectionsFilters();
    });

    collectionsSortField?.addEventListener('change', applyCollectionsFilters);
    collectionsSortDirection?.addEventListener('change', applyCollectionsFilters);


    const createBtn = document.getElementById('createSectionBtn');
    const createInline = document.getElementById('createSectionInline');
    const createInput = document.getElementById('createSectionInput');
    const confirmCreateBtn = document.getElementById('confirmCreateSectionBtn');
    const cancelCreateBtn = document.getElementById('cancelCreateSectionBtn');

    createBtn?.addEventListener('click', () => {
        createInline.hidden = false;
        createInput?.focus();
    });

    cancelCreateBtn?.addEventListener('click', () => {
        if (createInline) createInline.hidden = true;
        if (createInput) createInput.value = '';
        hideInlineNotice('collectionsSidebarNotice');
    });

    confirmCreateBtn?.addEventListener('click', () => {
        const name = createInput?.value?.trim() || '';

        postFormUrlEncoded('/collections/create', { name }).then(json => {
            if (!json.success) {
                showInlineNotice('collectionsSidebarNotice', json.message || 'Не удалось создать категорию');
                return;
            }

            reloadCollectionsIfOpen(null);
        });
    });

    const renameToggleBtn = document.getElementById('renameSectionToggleBtn');
    const renameInline = document.getElementById('renameSectionInline');
    const renameInput = document.getElementById('renameSectionInput');
    const confirmRenameBtn = document.getElementById('confirmRenameSectionBtn');
    const cancelRenameBtn = document.getElementById('cancelRenameSectionBtn');

    renameToggleBtn?.addEventListener('click', () => {
        renameInline.hidden = false;
        renameInput?.focus();
    });

    cancelRenameBtn?.addEventListener('click', () => {
        if (renameInline) renameInline.hidden = true;
        hideInlineNotice('collectionsMainNotice');
    });

    confirmRenameBtn?.addEventListener('click', () => {
        const sectionId = confirmRenameBtn.dataset.sectionId;
        const name = renameInput?.value?.trim() || '';

        postFormUrlEncoded('/collections/rename', { sectionId, name }).then(json => {
            if (!json.success) {
                showInlineNotice('collectionsMainNotice', json.message || 'Не удалось переименовать категорию');
                return;
            }

            reloadCollectionsIfOpen(sectionId);
        });
    });

    const deleteToggleBtn = document.getElementById('deleteSectionToggleBtn');

    deleteToggleBtn?.addEventListener('click', () => {
        const sectionId = deleteToggleBtn.dataset.sectionId;
        const hasComics = deleteToggleBtn.dataset.hasComics === 'true';

        if (!hasComics) {
            postFormUrlEncoded('/collections/delete', { sectionId }).then(json => {
                if (!json.success) {
                    showInlineNotice('collectionsMainNotice', json.message || 'Не удалось удалить категорию');
                    return;
                }

                reloadCollectionsIfOpen(null);
            });
            return;
        }

        const tabs = Array.from(document.querySelectorAll('.collection-tab'))
            .filter(tab => tab.dataset.sectionId !== sectionId)
            .map(tab => ({
                id: tab.dataset.sectionId,
                name: tab.querySelector('.collection-tab-name')?.textContent?.trim() || ''
            }));

        openTransferModal(
            'Куда перенести тайтлы перед удалением?',
            'delete-section',
            sectionId,
            tabs
        );
    });

    document.getElementById('selectAllBtn')?.addEventListener('click', () => {
        setCheckedState(true);
    });

    document.getElementById('clearSelectionBtn')?.addEventListener('click', () => {
        setCheckedState(false);
    });

    document.getElementById('moveSelectedBtn')?.addEventListener('click', () => {
        const checked = getSelectedComicIds();

        if (!checked.length) {
            showInlineNotice('collectionsMainNotice', 'Сначала выберите тайтлы');
            return;
        }

        const main = document.querySelector('.collections-main');
        const activeSectionId = main?.dataset.activeSectionId;

        const tabs = Array.from(document.querySelectorAll('.collection-tab'))
            .filter(tab => tab.dataset.sectionId !== activeSectionId)
            .map(tab => ({
                id: tab.dataset.sectionId,
                name: tab.querySelector('.collection-tab-name')?.textContent?.trim() || ''
            }));

        if (!tabs.length) {
            showInlineNotice('collectionsMainNotice', 'Нет категорий для переноса');
            return;
        }

        openTransferModal(
            'Куда перенести?',
            'move-comics',
            activeSectionId,
            tabs
        );
    });

    document.getElementById('removeSelectedBtn')?.addEventListener('click', () => {
        const checked = getSelectedComicIds();

        if (!checked.length) {
            showInlineNotice('collectionsMainNotice', 'Сначала выберите тайтлы');
            return;
        }

        const main = document.querySelector('.collections-main');
        const sectionId = main?.dataset.activeSectionId;

        postFormUrlEncoded('/collections/remove', {
            sectionId,
            comicIds: checked
        }).then(json => {
            if (!json.success) {
                showInlineNotice('collectionsMainNotice', json.message || 'Не удалось удалить тайтлы из категории');
                return;
            }

            reloadCollectionsIfOpen(sectionId);
        });
    });

    document.getElementById('collectionTransferModalClose')?.addEventListener('click', closeTransferModal);

    document.getElementById('collectionTransferModal')?.addEventListener('click', e => {
        if (e.target.id === 'collectionTransferModal') {
            closeTransferModal();
        }
    });
}

document.addEventListener('DOMContentLoaded', bindCollectionsPageEvents);
