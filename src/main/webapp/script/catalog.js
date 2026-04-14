const filterContainer = document.getElementById('filterContainer');
const filterOverlay = document.getElementById('filterOverlay');
const openFiltersBtn = document.getElementById('openFiltersBtn');
const closeFiltersBtn = document.getElementById('closeFiltersBtn');
const catalogContent = document.getElementById('catalogContent');

const MULTI_VALUE_FIELDS = [
    'selectedTypes',
    'selectedComicStatuses',
    'selectedAgeRatings',
    'selectedGenres',
    'selectedTags',
    'selectedLanguages'
];

const BOOLEAN_FIELDS = [
    'strictGenreMatch',
    'strictTagMatch',
    'strictLanguageMatch'
];

function setScrollLock(locked) {
    document.body.classList.toggle('filters-open', locked);
    document.documentElement.classList.toggle('filters-open', locked);
}

function openFilters() {
    if (!filterContainer || !filterOverlay) return;

    filterContainer.classList.add('active');
    filterOverlay.classList.add('active');
    setScrollLock(true);
}

function closeFilters() {
    if (!filterContainer || !filterOverlay) return;

    filterContainer.classList.remove('active');
    filterOverlay.classList.remove('active');
    setScrollLock(false);
}

function toggleFilter(forceState) {
    const shouldOpen = typeof forceState === 'boolean'
        ? forceState
        : !filterContainer?.classList.contains('active');

    if (shouldOpen) {
        openFilters();
    } else {
        closeFilters();
    }
}

openFiltersBtn?.addEventListener('click', () => toggleFilter(true));
closeFiltersBtn?.addEventListener('click', () => toggleFilter(false));
filterOverlay?.addEventListener('click', () => toggleFilter(false));

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && filterContainer?.classList.contains('active')) {
        closeFilters();
    }
});

function updateCatalogContent(html) {
    if (!catalogContent) return;
    catalogContent.innerHTML = html;
    closeFilters();
}

function ensureMultiValueFields(formData) {
    MULTI_VALUE_FIELDS.forEach((name) => {
        if (!formData.has(name)) {
            formData.append(name, '');
        }
    });
}

function ensureBooleanFields(formData) {
    BOOLEAN_FIELDS.forEach((name) => {
        if (!formData.has(name)) {
            formData.append(name, 'false');
        }
    });
}

function sendCatalogRequest(formData, resetPage = false) {
    if (resetPage) {
        formData.set('pageNumber', '0');
    }

    const params = new URLSearchParams(formData);
    window.history.replaceState({}, '', `/catalog?${params.toString()}`);

    fetch('/catalog', {
        method: 'POST',
        body: formData,
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
        .then((resp) => {
            if (!resp.ok) throw new Error();
            return resp.text();
        })
        .then(updateCatalogContent)
        .catch(() => {
            window.location.href = '/error';
        });
}

document.getElementById('sortField')?.addEventListener('change', applySort);
document.getElementById('sortDirection')?.addEventListener('change', applySort);

function applySort() {
    const form = document.getElementById('sortForm');
    if (!form) return;

    const data = new FormData(form);
    data.set('pageNumber', '0');
    sendCatalogRequest(data, false);
}

document.querySelector('.filter-panel')?.addEventListener('submit', (e) => {
    e.preventDefault();

    const data = new FormData(e.target);
    ensureMultiValueFields(data);
    ensureBooleanFields(data);

    sendCatalogRequest(data, true);
});

function resetFilters() {
    const form = document.querySelector('.filter-panel');
    if (!form) return;

    form.reset();

    form.querySelectorAll('input[type="checkbox"]').forEach((checkbox) => {
        checkbox.checked = false;
    });

    form.querySelectorAll('input[type="text"], input[type="date"], input[type="number"]').forEach((input) => {
        input.value = '';
    });

    const searchInput = document.querySelector('.search-form input[name="keyWords"]');
    if (searchInput) {
        searchInput.value = '';
    }

    const data = new FormData();
    data.append('reset', 'true');

    sendCatalogRequest(data, true);
}

document.querySelector('.search-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const data = new FormData(e.target);
    sendCatalogRequest(data, true);
});

document.querySelectorAll('.filter-search').forEach((input) => {
    input.addEventListener('input', (e) => {
        const value = e.target.value.toLowerCase();
        const options = e.target.parentElement.querySelectorAll('.filter-options label');

        options.forEach((option) => {
            const text = option.textContent.toLowerCase();
            option.style.display = text.includes(value) ? '' : 'none';
        });
    });
});

function changePage(pageNumber) {
    const data = new FormData();
    data.append('pageNumber', String(pageNumber - 1));
    sendCatalogRequest(data, false);
}

function switchView(mode) {
    const data = new FormData();
    data.append('viewMode', mode);
    sendCatalogRequest(data, false);
}

document.addEventListener('click', (e) => {
    const item = e.target.closest('.card-comic, .list-comic');

    if (item && !e.target.closest('a, button, input, label')) {
        const link = item.querySelector('a');
        if (link) {
            window.location = link.href;
        }
    }
});
