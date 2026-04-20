(() => {
    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function trimEdges(value) {
        return String(value ?? '').trim();
    }

    function readLookupState(sourceId, selectable) {
        const source = document.getElementById(sourceId);
        if (!source) {
            return [];
        }

        return [...source.querySelectorAll('.lookup-source-row')].map((row) => ({
            id: row.dataset.id ? Number(row.dataset.id) : null,
            name: trimEdges(row.dataset.name || ''),
            selected: selectable ? row.dataset.selected === 'true' : false,
            delete: false,
            editing: false
        }));
    }

    function renderSelectedChips(items, chipsId, hiddenContainerId, fieldName) {
        const chips = document.getElementById(chipsId);
        const hiddenContainer = document.getElementById(hiddenContainerId);

        if (!chips || !hiddenContainer) {
            return;
        }

        const selectedItems = items.filter((item) => item.selected && !item.delete && trimEdges(item.name).length > 0);

        chips.innerHTML = selectedItems.length > 0
            ? selectedItems.map((item) => `<span class="selected-value-chip">${escapeHtml(item.name)}</span>`).join('')
            : '<span class="field-hint">Ничего не выбрано</span>';

        hiddenContainer.innerHTML = selectedItems
            .filter((item) => Number.isFinite(item.id) && item.id > 0)
            .map((item) => `<input type="hidden" name="${fieldName}" value="${item.id}">`)
            .join('');
    }

    function serializeLookupOperations(items, selectable) {
        return JSON.stringify(
            items
                .map((item) => ({
                    id: Number.isFinite(item.id) ? item.id : null,
                    name: trimEdges(item.name),
                    selected: selectable ? Boolean(item.selected) : false,
                    delete: Boolean(item.delete)
                }))
                .filter((item) => item.id || item.name.length > 0 || item.delete)
        );
    }

    function buildRelationTypeDatalist(relationTypeItems) {
        const datalist = document.getElementById('relationTypeNames');
        if (!datalist) {
            return;
        }

        const names = relationTypeItems
            .filter((item) => !item.delete && trimEdges(item.name).length > 0)
            .map((item) => trimEdges(item.name));

        datalist.innerHTML = [...new Set(names)]
            .map((name) => `<option value="${escapeHtml(name)}"></option>`)
            .join('');
    }

    function renderLookupModalRows(kind, items, query) {
        const rowsWrap = document.getElementById('lookupModalRows');
        if (!rowsWrap) {
            return;
        }

        const normalizedQuery = trimEdges(query).toLowerCase();

        rowsWrap.innerHTML = items.map((item, index) => {
            const matches = normalizedQuery.length === 0 || trimEdges(item.name).toLowerCase().includes(normalizedQuery);
            const selectable = kind === 'genre' || kind === 'tag';

            return `
                <div class="lookup-modal-row ${matches ? '' : 'is-hidden'}" data-index="${index}">
                    ${selectable ? `
                        <label class="lookup-row-select">
                            <input type="checkbox" class="js-lookup-selected" ${item.selected ? 'checked' : ''} ${item.delete ? 'disabled' : ''}>
                            <span></span>
                        </label>
                    ` : '<span></span>'}

                    <input type="text"
                           class="js-lookup-name"
                           value="${escapeHtml(item.name)}"
                           maxlength="${kind === 'relationType' ? '50' : '100'}"
                           ${item.editing ? '' : 'readonly'}>

                    <button type="button" class="btn btn-outline js-toggle-lookup-edit">
                        ${item.editing ? 'Готово' : 'Редактировать'}
                    </button>

                    <label class="lookup-row-delete">
                        <input type="checkbox" class="js-lookup-delete" ${item.delete ? 'checked' : ''}>
                        <span>Удалить</span>
                    </label>
                </div>
            `;
        }).join('');
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
                       class="relation-type-name manual-trim-input"
                       value="${escapeHtml(item.relationTypeName || '')}"
                       list="relationTypeNames"
                       maxlength="50"
                       placeholder="Метка связи *">
            </div>
            <button type="button" class="btn btn-outline relation-remove-btn js-remove-relation">Удалить</button>
        `;
        return relationRow;
    }

    function serializeRelations() {
        const relationsHidden = document.getElementById('relationsJson');
        const relationsContainer = document.getElementById('relatedComicRelations');

        if (!relationsHidden || !relationsContainer) {
            return { ok: true };
        }

        const relations = [];
        const seenComicIds = new Set();

        for (const row of relationsContainer.querySelectorAll('.relation-item')) {
            const relatedComicId = Number(row.dataset.relatedComicId);
            const relatedComicTitle = trimEdges(row.querySelector('.relation-item-title')?.textContent || '');
            const relationTypeInput = row.querySelector('.relation-type-name');
            const relationTypeName = trimEdges(relationTypeInput?.value || '');

            if (!Number.isFinite(relatedComicId) || relatedComicId <= 0) {
                continue;
            }

            if (seenComicIds.has(relatedComicId)) {
                continue;
            }

            if (!relationTypeName) {
                return { ok: false, message: 'Укажите метку связи для каждого связанного комикса.' };
            }

            seenComicIds.add(relatedComicId);

            relations.push({
                relatedComicId,
                relatedComicTitle,
                relationTypeName
            });
        }

        relationsHidden.value = JSON.stringify(relations);
        return { ok: true };
    }

    document.addEventListener('DOMContentLoaded', () => {
        const form = document.getElementById('adminComicForm');
        const coverInput = document.getElementById('coverFile');
        const coverPreview = document.getElementById('coverPreview');
        const coverPlaceholder = document.getElementById('coverPreviewPlaceholder');
        const relatedSearchInput = document.getElementById('relatedComicSearch');
        const relatedResults = document.getElementById('relatedComicSearchResults');
        const clientFormError = document.getElementById('clientFormError');

        const lookupModal = document.getElementById('lookupModal');
        const lookupModalTitle = document.getElementById('lookupModalTitle');
        const lookupModalSearch = document.getElementById('lookupModalSearch');
        const lookupModalCloseBtn = document.getElementById('lookupModalCloseBtn');
        const lookupModalAddBtn = document.getElementById('lookupModalAddBtn');
        const lookupModalApplyBtn = document.getElementById('lookupModalApplyBtn');

        const state = {
            currentModalKind: null,
            genre: readLookupState('genreSourceRows', true),
            tag: readLookupState('tagSourceRows', true),
            relationType: readLookupState('relationTypeSourceRows', false)
        };

        function getStateByKind(kind) {
            if (kind === 'genre') return state.genre;
            if (kind === 'tag') return state.tag;
            return state.relationType;
        }

        function refreshRenderedState() {
            renderSelectedChips(state.genre, 'selectedGenresChips', 'selectedGenreInputs', 'genreIds');
            renderSelectedChips(state.tag, 'selectedTagsChips', 'selectedTagInputs', 'tagIds');
            buildRelationTypeDatalist(state.relationType);
        }

        function openLookupModal(kind) {
            state.currentModalKind = kind;

            if (lookupModalTitle) {
                lookupModalTitle.textContent = kind === 'genre'
                    ? 'Жанры'
                    : kind === 'tag'
                        ? 'Теги'
                        : 'Метки связей';
            }

            if (lookupModalSearch) {
                lookupModalSearch.value = '';
            }

            renderLookupModalRows(kind, getStateByKind(kind), '');
            lookupModal.classList.remove('hidden');
            lookupModal.classList.add('visible');
            document.body.style.overflow = 'hidden';
        }

        function closeLookupModal() {
            if (!lookupModal) {
                return;
            }

            lookupModal.classList.add('hidden');
            lookupModal.classList.remove('visible');
            document.body.style.overflow = '';
            state.currentModalKind = null;
        }

        function showClientError(message) {
            if (!clientFormError) {
                return;
            }

            clientFormError.textContent = message;
            clientFormError.classList.remove('hidden');
            clientFormError.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        function hideClientError() {
            if (!clientFormError) {
                return;
            }

            clientFormError.textContent = '';
            clientFormError.classList.add('hidden');
        }

        refreshRenderedState();

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

        document.addEventListener('click', (event) => {
            const openModalBtn = event.target.closest('.js-open-lookup-modal');
            if (openModalBtn) {
                event.preventDefault();
                openLookupModal(openModalBtn.dataset.kind);
                return;
            }

            if (event.target === lookupModal) {
                closeLookupModal();
                return;
            }

            const removeRelationBtn = event.target.closest('.js-remove-relation');
            if (removeRelationBtn) {
                event.preventDefault();
                removeRelationBtn.closest('.relation-item')?.remove();
                return;
            }

            const searchItem = event.target.closest('.related-search-item');
            if (searchItem) {
                event.preventDefault();

                const relatedComicId = Number(searchItem.dataset.comicId);
                const relatedComicTitle = searchItem.dataset.comicTitle || '';

                const exists = document.querySelector(`.relation-item[data-related-comic-id="${relatedComicId}"]`);
                if (!exists) {
                    document.getElementById('relatedComicRelations')?.appendChild(
                        createRelationItem({
                            relatedComicId,
                            relatedComicTitle,
                            relationTypeName: ''
                        })
                    );
                }

                relatedResults.innerHTML = '';
                return;
            }

            const editLookupBtn = event.target.closest('.js-toggle-lookup-edit');
            if (editLookupBtn && state.currentModalKind) {
                event.preventDefault();

                const row = editLookupBtn.closest('.lookup-modal-row');
                const index = Number(row?.dataset.index);
                const items = getStateByKind(state.currentModalKind);
                const item = items[index];

                if (!item) {
                    return;
                }

                item.editing = !item.editing;
                renderLookupModalRows(state.currentModalKind, items, lookupModalSearch?.value || '');

                const renderedRow = document.querySelector(`.lookup-modal-row[data-index="${index}"] .js-lookup-name`);
                if (item.editing && renderedRow) {
                    renderedRow.focus();
                    renderedRow.select();
                }

                if (state.currentModalKind === 'relationType') {
                    buildRelationTypeDatalist(state.relationType);
                }
            }
        });

        if (lookupModalCloseBtn) {
            lookupModalCloseBtn.addEventListener('click', closeLookupModal);
        }

        if (lookupModalApplyBtn) {
            lookupModalApplyBtn.addEventListener('click', () => {
                refreshRenderedState();
                closeLookupModal();
            });
        }

        if (lookupModalAddBtn) {
            lookupModalAddBtn.addEventListener('click', () => {
                if (!state.currentModalKind) {
                    return;
                }

                const items = getStateByKind(state.currentModalKind);
                items.push({
                    id: null,
                    name: '',
                    selected: state.currentModalKind !== 'relationType',
                    delete: false,
                    editing: true
                });

                renderLookupModalRows(state.currentModalKind, items, lookupModalSearch?.value || '');
            });
        }

        if (lookupModalSearch) {
            lookupModalSearch.addEventListener('input', () => {
                if (!state.currentModalKind) {
                    return;
                }

                renderLookupModalRows(state.currentModalKind, getStateByKind(state.currentModalKind), lookupModalSearch.value);
            });
        }

        document.addEventListener('input', (event) => {
            const row = event.target.closest('.lookup-modal-row');
            if (row && state.currentModalKind) {
                const index = Number(row.dataset.index);
                const items = getStateByKind(state.currentModalKind);
                const item = items[index];

                if (!item) {
                    return;
                }

                if (event.target.classList.contains('js-lookup-name')) {
                    item.name = event.target.value;
                    if (state.currentModalKind === 'relationType') {
                        buildRelationTypeDatalist(state.relationType);
                    }
                }
            }

            if (event.target.id === 'releaseYear') {
                event.target.value = event.target.value.replace(/\D/g, '').slice(0, 4);
            }

            if (event.target.classList.contains('manual-trim-input')) {
                event.target.value = event.target.value.replace(/^\s+/, '');
            }
        });

        document.addEventListener('change', (event) => {
            const row = event.target.closest('.lookup-modal-row');
            if (row && state.currentModalKind) {
                const index = Number(row.dataset.index);
                const items = getStateByKind(state.currentModalKind);
                const item = items[index];

                if (!item) {
                    return;
                }

                if (event.target.classList.contains('js-lookup-selected')) {
                    item.selected = event.target.checked;
                    refreshRenderedState();
                }

                if (event.target.classList.contains('js-lookup-delete')) {
                    item.delete = event.target.checked;
                    if (item.delete) {
                        item.selected = false;
                    }
                    renderLookupModalRows(state.currentModalKind, items, lookupModalSearch?.value || '');
                    refreshRenderedState();
                }
            }
        });

        window.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && lookupModal && !lookupModal.classList.contains('hidden')) {
                closeLookupModal();
            }
        });

        let searchTimeoutId = null;

        if (relatedSearchInput && relatedResults) {
            relatedSearchInput.addEventListener('input', () => {
                const query = trimEdges(relatedSearchInput.value);
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

        document.querySelectorAll('input[type="text"], textarea').forEach((field) => {
            field.addEventListener('blur', () => {
                field.value = trimEdges(field.value);
            });
        });

        if (form) {
            form.addEventListener('submit', (event) => {
                hideClientError();

                document.querySelectorAll('input[type="text"], textarea').forEach((field) => {
                    field.value = trimEdges(field.value);
                });

                const releaseYear = document.getElementById('releaseYear');
                if (releaseYear) {
                    releaseYear.value = releaseYear.value.replace(/\D/g, '').slice(0, 4);

                    if (!/^\d{4}$/.test(releaseYear.value)) {
                        event.preventDefault();
                        showClientError('Год релиза должен быть в формате XXXX.');
                        return;
                    }

                    const yearValue = Number(releaseYear.value);
                    if (yearValue < 1900 || yearValue > 2100) {
                        event.preventDefault();
                        showClientError('Год релиза должен быть в диапазоне 1900–2100.');
                        return;
                    }
                }

                renderSelectedChips(state.genre, 'selectedGenresChips', 'selectedGenreInputs', 'genreIds');
                renderSelectedChips(state.tag, 'selectedTagsChips', 'selectedTagInputs', 'tagIds');

                const genreOperationsJson = document.getElementById('genreOperationsJson');
                const tagOperationsJson = document.getElementById('tagOperationsJson');
                const relationTypeOperationsJson = document.getElementById('relationTypeOperationsJson');

                if (genreOperationsJson) {
                    genreOperationsJson.value = serializeLookupOperations(state.genre, true);
                }
                if (tagOperationsJson) {
                    tagOperationsJson.value = serializeLookupOperations(state.tag, true);
                }
                if (relationTypeOperationsJson) {
                    relationTypeOperationsJson.value = serializeLookupOperations(state.relationType, false);
                }

                const relationsSerialization = serializeRelations();
                if (!relationsSerialization.ok) {
                    event.preventDefault();
                    showClientError(relationsSerialization.message);
                }
            });
        }
    });
})();
