(() => {
    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function createLookupRow(kind, data = {}) {
        const row = document.createElement('div');
        row.className = 'lookup-editor-row';
        if (data.id) {
            row.dataset.id = String(data.id);
        }

        const selectedPart = kind === 'genre' || kind === 'tag'
            ? `
                <label class="lookup-select-for-comic-label">
                    <input type="checkbox" class="lookup-select-for-comic" ${data.selected ? 'checked' : ''}>
                    Выбрать
                </label>
              `
            : '';

        row.innerHTML = `
            <input type="hidden" class="lookup-id" value="${escapeHtml(data.id ?? '')}">
            <input type="text" class="lookup-name" value="${escapeHtml(data.name ?? '')}" maxlength="${kind === 'relationType' ? '50' : '100'}">
            ${selectedPart}
            <label class="lookup-checkbox">
                <input type="checkbox" class="lookup-delete" ${data.delete ? 'checked' : ''}>
                Удалить
            </label>
        `;
        return row;
    }

    function serializeLookupRows(containerId, hiddenId) {
        const container = document.getElementById(containerId);
        const hiddenInput = document.getElementById(hiddenId);
        if (!container || !hiddenInput) {
            return;
        }

        const items = [...container.querySelectorAll('.lookup-editor-row')]
            .map((row) => ({
                id: row.dataset.id ? Number(row.dataset.id) : null,
                name: (row.querySelector('.lookup-name')?.value || '').trim(),
                selected: row.querySelector('.lookup-select-for-comic')?.checked || false,
                delete: row.querySelector('.lookup-delete')?.checked || false
            }))
            .filter((item) => item.id || item.name.length > 0 || item.delete);

        hiddenInput.value = JSON.stringify(items);
    }

    function buildRelationTypeDatalist() {
        const datalist = document.getElementById('relationTypeNames');
        const container = document.getElementById('relationTypeEditorRows');
        if (!datalist || !container) {
            return;
        }

        const names = [...container.querySelectorAll('.lookup-name')]
            .map((input) => input.value.trim())
            .filter((name) => name.length > 0);

        datalist.innerHTML = names
            .map((name) => `<option value="${escapeHtml(name)}"></option>`)
            .join('');
    }

    function createRelationItem(item) {
        const relationRow = document.createElement('div');
        relationRow.className = 'relation-item';
        relationRow.dataset.relatedComicId = String(item.relatedComicId);

        relationRow.innerHTML = `
            <div class="relation-item-main">
                <div class="relation-item-title">${escapeHtml(item.relatedComicTitle || '')}</div>
                <input type="hidden" class="relation-comic-id" value="${escapeHtml(item.relatedComicId)}">
                <input type="text"
                       class="relation-type-name"
                       value="${escapeHtml(item.relationTypeName || '')}"
                       list="relationTypeNames"
                       maxlength="50"
                       placeholder="Метка связи">
            </div>
            <button type="button" class="btn btn-outline relation-remove-btn js-remove-relation">Удалить</button>
        `;
        return relationRow;
    }

    function serializeRelations() {
        const relationsHidden = document.getElementById('relationsJson');
        const relationsContainer = document.getElementById('relatedComicRelations');
        if (!relationsHidden || !relationsContainer) {
            return;
        }

        const items = [...relationsContainer.querySelectorAll('.relation-item')]
            .map((row) => ({
                relatedComicId: Number(row.dataset.relatedComicId),
                relatedComicTitle: (row.querySelector('.relation-item-title')?.textContent || '').trim(),
                relationTypeName: (row.querySelector('.relation-type-name')?.value || '').trim()
            }))
            .filter((item) => Number.isFinite(item.relatedComicId) && item.relatedComicId > 0);

        relationsHidden.value = JSON.stringify(items);
    }

    function applyStoredLookupOperations(containerId, hiddenId, kind) {
        const container = document.getElementById(containerId);
        const hiddenInput = document.getElementById(hiddenId);
        if (!container || !hiddenInput || !hiddenInput.value) {
            return;
        }

        let items = [];
        try {
            items = JSON.parse(hiddenInput.value || '[]');
        } catch (e) {
            items = [];
        }

        items.forEach((item) => {
            if (item.id) {
                const row = container.querySelector(`.lookup-editor-row[data-id="${item.id}"]`);
                if (!row) {
                    return;
                }

                const nameInput = row.querySelector('.lookup-name');
                const deleteInput = row.querySelector('.lookup-delete');
                const selectForComic = row.querySelector('.lookup-select-for-comic');

                if (nameInput && item.name != null) {
                    nameInput.value = item.name;
                }
                if (deleteInput) {
                    deleteInput.checked = Boolean(item.delete);
                }
                if (selectForComic) {
                    selectForComic.checked = Boolean(item.selected);
                }
                return;
            }

            if ((item.name || '').trim().length > 0 || item.delete || item.selected) {
                container.appendChild(createLookupRow(kind, item));
            }
        });
    }

    function addRelationFromSearchItem(item) {
        const relationsContainer = document.getElementById('relatedComicRelations');
        if (!relationsContainer || !item || !item.id) {
            return;
        }

        const exists = relationsContainer.querySelector(`.relation-item[data-related-comic-id="${item.id}"]`);
        if (exists) {
            return;
        }

        relationsContainer.appendChild(createRelationItem({
            relatedComicId: item.id,
            relatedComicTitle: item.title,
            relationTypeName: ''
        }));
    }

    document.addEventListener('DOMContentLoaded', () => {
        const form = document.getElementById('adminComicForm');
        const coverInput = document.getElementById('coverFile');
        const coverPreview = document.getElementById('coverPreview');
        const coverPlaceholder = document.getElementById('coverPreviewPlaceholder');
        const relatedSearchInput = document.getElementById('relatedComicSearch');
        const relatedResults = document.getElementById('relatedComicSearchResults');

        applyStoredLookupOperations('genreEditorRows', 'genreOperationsJson', 'genre');
        applyStoredLookupOperations('tagEditorRows', 'tagOperationsJson', 'tag');
        applyStoredLookupOperations('relationTypeEditorRows', 'relationTypeOperationsJson', 'relationType');
        buildRelationTypeDatalist();

        if (coverInput && coverPreview) {
            coverInput.addEventListener('change', () => {
                const [file] = coverInput.files || [];
                if (!file) {
                    return;
                }

                const reader = new FileReader();
                reader.onload = (event) => {
                    coverPreview.src = event.target?.result || '';
                    coverPreview.classList.remove('hidden');
                    if (coverPlaceholder) {
                        coverPlaceholder.classList.add('hidden');
                    }
                };
                reader.readAsDataURL(file);
            });
        }

        document.querySelectorAll('.js-add-lookup-row').forEach((button) => {
            button.addEventListener('click', () => {
                const targetId = button.dataset.target;
                const kind = button.dataset.kind;
                const container = document.getElementById(targetId);
                if (!container) {
                    return;
                }

                container.appendChild(createLookupRow(kind, {}));
                buildRelationTypeDatalist();
            });
        });

        document.addEventListener('click', (event) => {
            const removeRelationButton = event.target.closest('.js-remove-relation');
            if (removeRelationButton) {
                event.preventDefault();
                removeRelationButton.closest('.relation-item')?.remove();
                return;
            }

            const searchItem = event.target.closest('.related-search-item');
            if (searchItem) {
                event.preventDefault();
                addRelationFromSearchItem({
                    id: Number(searchItem.dataset.comicId),
                    title: searchItem.dataset.comicTitle || ''
                });
                if (relatedResults) {
                    relatedResults.innerHTML = '';
                }
            }
        });

        document.addEventListener('input', (event) => {
            if (event.target.classList.contains('relation-type-editor-input')) {
                buildRelationTypeDatalist();
            }
        });

        let searchTimeoutId = null;

        if (relatedSearchInput && relatedResults) {
            relatedSearchInput.addEventListener('input', () => {
                const query = relatedSearchInput.value.trim();
                const searchUrl = relatedSearchInput.dataset.searchUrl;
                const excludeComicId = relatedSearchInput.dataset.excludeComicId || '';

                if (searchTimeoutId) {
                    clearTimeout(searchTimeoutId);
                }

                if (query.length < 2) {
                    relatedResults.innerHTML = '';
                    return;
                }

                searchTimeoutId = window.setTimeout(() => {
                    const url = `${searchUrl}?q=${encodeURIComponent(query)}${excludeComicId ? `&excludeComicId=${encodeURIComponent(excludeComicId)}` : ''}`;

                    fetch(url, {
                        headers: {
                            'X-Requested-With': 'XMLHttpRequest'
                        }
                    })
                        .then((response) => response.json())
                        .then((items) => {
                            relatedResults.innerHTML = '';

                            if (!Array.isArray(items) || items.length === 0) {
                                return;
                            }

                            items.forEach((item) => {
                                const node = document.createElement('button');
                                node.type = 'button';
                                node.className = 'related-search-item';
                                node.dataset.comicId = String(item.id);
                                node.dataset.comicTitle = item.title || '';

                                const coverPart = item.cover
                                    ? `<img class="related-search-cover" src="/assets/covers/${escapeHtml(item.cover)}" alt="${escapeHtml(item.title || '')}">`
                                    : `<div class="related-search-placeholder">—</div>`;

                                node.innerHTML = `
                                    ${coverPart}
                                    <div class="related-search-main">
                                        <div class="related-search-title">${escapeHtml(item.title || '')}</div>
                                        <div class="related-search-subtitle">${escapeHtml(item.originalTitle || '')}</div>
                                    </div>
                                `;

                                relatedResults.appendChild(node);
                            });
                        })
                        .catch(() => {
                            relatedResults.innerHTML = '';
                        });
                }, 250);
            });
        }

        if (form) {
            form.addEventListener('submit', () => {
                serializeLookupRows('genreEditorRows', 'genreOperationsJson');
                serializeLookupRows('tagEditorRows', 'tagOperationsJson');
                serializeLookupRows('relationTypeEditorRows', 'relationTypeOperationsJson');
                serializeRelations();
            });
        }
    });
})();
