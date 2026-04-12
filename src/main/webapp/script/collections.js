function showInlineNotice(containerId, message, type = 'error') {
    const el = document.getElementById(containerId);
    if (!el) return;

    el.hidden = false;
    el.textContent = message;
    el.className = 'collection-inline-notice ' + (type === 'success'
        ? 'collection-inline-notice-success'
        : 'collection-inline-notice-error');
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
    }).then(async r => {
        const json = await r.json().catch(() => ({}));
        if (!r.ok && json.success !== false) {
            return {
                success: false,
                message: json.message || 'Ошибка запроса'
            };
        }
        return json;
    });
}

function loadCollectionSection(sectionId = null, page = 0, viewMode = 'card') {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('viewMode', viewMode);

    if (sectionId !== null && sectionId !== undefined && sectionId !== '') {
        params.append('sectionId', sectionId);
    }

    fetch(`/collections?${params.toString()}`, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
        .then(r => r.text())
        .then(html => {
            document.getElementById('collectionsRoot').innerHTML = html;
        });
}

function reloadCollectionIfOpen(sectionId = null) {
    const root = document.getElementById('collectionsRoot');
    if (!root) return;

    const main = root.querySelector('.collection-main');
    const viewMode = main?.dataset.viewMode || 'card';
    const resolvedSectionId = sectionId ?? main?.dataset.activeSectionId ?? null;

    loadCollectionSection(resolvedSectionId, 0, viewMode);
}

window.reloadCollectionsIfOpen = reloadCollectionIfOpen;

function getCheckedComicIds() {
    return Array.from(document.querySelectorAll('.collection-comic-check:checked')).map(i => i.value);
}

function openTransferModal(title, action, sectionId, sections) {
    const modal = document.getElementById('collectionTransferModal');
    const body = document.getElementById('collectionTransferModalBody');
    if (!modal || !body) return;

    const items = sections.map(section => `
        <button type="button" class="collection-choice-btn" data-transfer-target-id="${section.id}">
            <span class="collection-choice-name">${section.name}</span>
        </button>
    `).join('');

    body.innerHTML = `
        <div class="collection-picker">
            <h3>${title}</h3>
            <div class="collection-inline-notice" id="collectionTransferNotice" hidden></div>
            <div class="collection-picker-scroll">
                <div class="collection-picker-list">${items}</div>
            </div>
            <div class="collection-modal-actions">
                <button type="button"
                        class="btn collection-save-btn"
                        id="confirmTransferActionBtn"
                        data-action="${action}"
                        data-section-id="${sectionId}">
                    Подтвердить
                </button>
            </div>
        </div>
    `;

    modal.classList.add('visible');
}

function closeTransferModal() {
    const modal = document.getElementById('collectionTransferModal');
    const body = document.getElementById('collectionTransferModalBody');
    if (modal) modal.classList.remove('visible');
    if (body) body.innerHTML = '';
}

document.addEventListener('click', async (e) => {
    const tabBtn = e.target.closest('.collection-tab');
    if (tabBtn && !e.target.closest('.collection-icon-action')) {
        loadCollectionSection(tabBtn.dataset.sectionId, 0, 'card');
        return;
    }

    if (e.target.closest('#createSectionBtn')) {
        document.getElementById('createSectionInline')?.removeAttribute('hidden');
        document.getElementById('createSectionInput')?.focus();
        return;
    }

    if (e.target.closest('#cancelCreateSectionBtn')) {
        document.getElementById('createSectionInline')?.setAttribute('hidden', 'hidden');
        const input = document.getElementById('createSectionInput');
        if (input) input.value = '';
        hideInlineNotice('collectionSidebarNotice');
        return;
    }

    if (e.target.closest('#confirmCreateSectionBtn')) {
        const name = document.getElementById('createSectionInput')?.value?.trim() || '';
        const json = await postFormUrlEncoded('/collections/create', { name });

        if (!json.success) {
            showInlineNotice('collectionSidebarNotice', json.message || 'Не удалось создать категорию');
            return;
        }

        reloadCollectionIfOpen(null);
        return;
    }

    if (e.target.closest('#renameSectionToggleBtn')) {
        document.getElementById('renameSectionInline')?.removeAttribute('hidden');
        document.getElementById('renameSectionInput')?.focus();
        return;
    }

    if (e.target.closest('#cancelRenameSectionBtn')) {
        document.getElementById('renameSectionInline')?.setAttribute('hidden', 'hidden');
        hideInlineNotice('collectionMainNotice');
        return;
    }

    if (e.target.closest('#confirmRenameSectionBtn')) {
        const btn = document.getElementById('confirmRenameSectionBtn');
        const sectionId = btn?.dataset.sectionId;
        const name = document.getElementById('renameSectionInput')?.value?.trim() || '';

        const json = await postFormUrlEncoded('/collections/rename', { sectionId, name });

        if (!json.success) {
            showInlineNotice('collectionMainNotice', json.message || 'Не удалось переименовать категорию');
            return;
        }

        reloadCollectionIfOpen(sectionId);
        return;
    }

    if (e.target.closest('#deleteSectionToggleBtn')) {
        const btn = document.getElementById('deleteSectionToggleBtn');
        const sectionId = btn?.dataset.sectionId;
        const hasComics = btn?.dataset.hasComics === 'true';

        if (!hasComics) {
            const json = await postFormUrlEncoded('/collections/delete', { sectionId });

            if (!json.success) {
                showInlineNotice('collectionMainNotice', json.message || 'Не удалось удалить категорию');
                return;
            }

            reloadCollectionIfOpen(null);
            return;
        }

        const sections = Array.from(document.querySelectorAll('.collection-tab'))
            .filter(tab => tab.dataset.sectionId !== sectionId)
            .map(tab => ({
                id: tab.dataset.sectionId,
                name: tab.querySelector('.collection-tab-name')?.textContent?.trim() || ''
            }));

        if (!sections.length) {
            showInlineNotice('collectionMainNotice', 'Нет категории для переноса');
            return;
        }

        openTransferModal('Куда перенести тайтлы перед удалением?', 'delete-section', sectionId, sections);
        return;
    }

    if (e.target.closest('[data-transfer-target-id]')) {
        const button = e.target.closest('[data-transfer-target-id]');
        document.querySelectorAll('[data-transfer-target-id]').forEach(btn => btn.classList.remove('selected'));
        button.classList.add('selected');
        return;
    }

    if (e.target.closest('#confirmTransferActionBtn')) {
        const btn = document.getElementById('confirmTransferActionBtn');
        const selected = document.querySelector('[data-transfer-target-id].selected');
        const noticeId = 'collectionTransferNotice';

        if (!selected) {
            showInlineNotice(noticeId, 'Выберите категорию');
            return;
        }

        const action = btn.dataset.action;
        const sectionId = btn.dataset.sectionId;

        if (action === 'delete-section') {
            const json = await postFormUrlEncoded('/collections/delete', {
                sectionId,
                targetSectionId: selected.dataset.transferTargetId
            });

            if (!json.success) {
                showInlineNotice(noticeId, json.message || 'Не удалось удалить категорию');
                return;
            }

            closeTransferModal();
            reloadCollectionIfOpen(null);
            return;
        }

        if (action === 'move-comics') {
            const main = document.querySelector('.collection-main');
            const fromSectionId = main?.dataset.activeSectionId;
            const comicIds = getCheckedComicIds();

            const json = await postFormUrlEncoded('/collections/move', {
                fromSectionId,
                toSectionId: selected.dataset.transferTargetId,
                comicIds
            });

            if (!json.success) {
                showInlineNotice(noticeId, json.message || 'Не удалось перенести тайтлы');
                return;
            }

            closeTransferModal();
            reloadCollectionIfOpen(fromSectionId);
            return;
        }
    }

    if (e.target.closest('#moveSelectedBtn')) {
        const checked = getCheckedComicIds();

        if (!checked.length) {
            showInlineNotice('collectionMainNotice', 'Сначала выберите тайтлы');
            return;
        }

        const main = document.querySelector('.collection-main');
        const activeSectionId = main?.dataset.activeSectionId;

        const sections = Array.from(document.querySelectorAll('.collection-tab'))
            .filter(tab => tab.dataset.sectionId !== activeSectionId)
            .map(tab => ({
                id: tab.dataset.sectionId,
                name: tab.querySelector('.collection-tab-name')?.textContent?.trim() || ''
            }));

        if (!sections.length) {
            showInlineNotice('collectionMainNotice', 'Нет категории для переноса');
            return;
        }

        openTransferModal('Куда перенести выбранные тайтлы?', 'move-comics', activeSectionId, sections);
        return;
    }

    if (e.target.closest('#removeSelectedBtn')) {
        const checked = getCheckedComicIds();

        if (!checked.length) {
            showInlineNotice('collectionMainNotice', 'Сначала выберите тайтлы');
            return;
        }

        const main = document.querySelector('.collection-main');
        const sectionId = main?.dataset.activeSectionId;

        const json = await postFormUrlEncoded('/collections/remove', {
            sectionId,
            comicIds: checked
        });

        if (!json.success) {
            showInlineNotice('collectionMainNotice', json.message || 'Не удалось удалить тайтлы из категории');
            return;
        }

        reloadCollectionIfOpen(sectionId);
        return;
    }

    if (e.target.closest('#selectAllComicsBtn')) {
        document.querySelectorAll('.collection-comic-check').forEach(i => {
            i.checked = true;
        });
        return;
    }

    if (e.target.closest('#clearSelectedComicsBtn')) {
        document.querySelectorAll('.collection-comic-check').forEach(i => {
            i.checked = false;
        });
        return;
    }

    if (e.target.closest('#collectionTransferModalClose')) {
        closeTransferModal();
        return;
    }

    if (e.target.id === 'collectionTransferModal') {
        closeTransferModal();
    }
});
