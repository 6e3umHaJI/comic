const LIMITS = {
    catalogSearchQuery: 255,
    filterSearchGenre: 50,
    filterSearchTag: 50,
    filterSearchLanguage: 35,
    ratingDigits: 3,
    ratingMaxIntegerPart: 5,
    releaseYearLength: 4,
    releaseYearMin: 1900,
    releaseYearMax: 2100
};

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

function cutToMax(value, maxLength) {
    return String(value ?? '').slice(0, maxLength);
}

function normalizeForSubmit(value, maxLength) {
    return String(value ?? '').trim().slice(0, maxLength);
}

function digitsOnly(value, maxLength) {
    return String(value ?? '').replace(/\D/g, '').slice(0, maxLength);
}

function normalizeRatingRaw(value) {
    const digits = digitsOnly(value, LIMITS.ratingDigits);

    if (!digits) {
        return '';
    }

    const firstDigit = Math.min(Number(digits.charAt(0)), LIMITS.ratingMaxIntegerPart);
    const fraction = digits.slice(1, LIMITS.ratingDigits);

    if (!fraction) {
        return String(firstDigit);
    }

    return `${firstDigit}.${fraction}`;
}

function finalizeRatingValue(value) {
    const normalized = normalizeRatingRaw(value);
    if (!normalized) {
        return '';
    }

    const [wholePart, fractionPart = ''] = normalized.split('.');
    return `${wholePart}.${fractionPart.padEnd(2, '0').slice(0, 2)}`;
}

function normalizeReleaseYearRaw(value) {
    return digitsOnly(value, LIMITS.releaseYearLength);
}

function finalizeReleaseYearValue(value) {
    const normalized = normalizeReleaseYearRaw(value);

    if (!normalized) {
        return '';
    }

    if (normalized.length !== LIMITS.releaseYearLength) {
        return '';
    }

    let year = Number(normalized);

    if (year < LIMITS.releaseYearMin) {
        year = LIMITS.releaseYearMin;
    }
    if (year > LIMITS.releaseYearMax) {
        year = LIMITS.releaseYearMax;
    }

    return String(year);
}

function getFilterSearchLimit(input) {
    const explicitLimit = Number(input.dataset.filterSearchLimit || 0);
    if (Number.isFinite(explicitLimit) && explicitLimit > 0) {
        return explicitLimit;
    }

    const placeholder = String(input.placeholder || '').toLowerCase();
    if (placeholder.includes('жанр')) {
        return LIMITS.filterSearchGenre;
    }
    if (placeholder.includes('тег')) {
        return LIMITS.filterSearchTag;
    }
    if (placeholder.includes('язык')) {
        return LIMITS.filterSearchLanguage;
    }

    return LIMITS.catalogSearchQuery;
}

function applyCatalogInputLimits(scope = document) {
    const catalogSearchInput = scope.querySelector('.search-form input[name="keyWords"]');
    if (catalogSearchInput) {
        catalogSearchInput.maxLength = LIMITS.catalogSearchQuery;
        catalogSearchInput.value = cutToMax(catalogSearchInput.value, LIMITS.catalogSearchQuery);
    }

    scope.querySelectorAll('.filter-search').forEach((input) => {
        const limit = getFilterSearchLimit(input);
        input.maxLength = limit;
        input.value = cutToMax(input.value, limit);
    });

    scope.querySelectorAll('input[name="avgRatingFrom"], input[name="avgRatingTo"]').forEach((input) => {
        input.type = 'text';
        input.inputMode = 'decimal';
        input.maxLength = 4;
        input.value = normalizeRatingRaw(input.value);
    });

    scope.querySelectorAll('input[name="releaseYearFrom"], input[name="releaseYearTo"]').forEach((input) => {
        input.type = 'text';
        input.inputMode = 'numeric';
        input.maxLength = LIMITS.releaseYearLength;
        input.value = normalizeReleaseYearRaw(input.value);
    });
}

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

    applyCatalogInputLimits(document);

    const form = e.target;

    const avgRatingFrom = form.querySelector('input[name="avgRatingFrom"]');
    const avgRatingTo = form.querySelector('input[name="avgRatingTo"]');
    const releaseYearFrom = form.querySelector('input[name="releaseYearFrom"]');
    const releaseYearTo = form.querySelector('input[name="releaseYearTo"]');

    const catalogSearchInput = document.querySelector('.search-form input[name="keyWords"]');
    if (catalogSearchInput) {
        catalogSearchInput.value = normalizeForSubmit(catalogSearchInput.value, LIMITS.catalogSearchQuery);
    }

    form.querySelectorAll('.filter-search').forEach((input) => {
        input.value = normalizeForSubmit(input.value, getFilterSearchLimit(input));
    });

    if (avgRatingFrom) {
        avgRatingFrom.value = finalizeRatingValue(avgRatingFrom.value);
    }
    if (avgRatingTo) {
        avgRatingTo.value = finalizeRatingValue(avgRatingTo.value);
    }
    if (releaseYearFrom) {
        releaseYearFrom.value = finalizeReleaseYearValue(releaseYearFrom.value);
    }
    if (releaseYearTo) {
        releaseYearTo.value = finalizeReleaseYearValue(releaseYearTo.value);
    }

    const data = new FormData(form);
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

    applyCatalogInputLimits(document);

    const data = new FormData();
    data.append('reset', 'true');

    sendCatalogRequest(data, true);
}

document.querySelector('.search-form')?.addEventListener('submit', (e) => {
    e.preventDefault();

    const searchInput = e.target.querySelector('input[name="keyWords"]');
    if (searchInput) {
        searchInput.value = normalizeForSubmit(searchInput.value, LIMITS.catalogSearchQuery);
    }

    const data = new FormData(e.target);
    sendCatalogRequest(data, true);
});

document.querySelectorAll('.filter-search').forEach((input) => {
    input.addEventListener('input', (e) => {
        const currentInput = e.target;
        const limit = getFilterSearchLimit(currentInput);

        currentInput.value = cutToMax(currentInput.value, limit);

        const value = currentInput.value.toLowerCase();
        const options = currentInput.parentElement.querySelectorAll('.filter-options label');

        options.forEach((option) => {
            const text = option.textContent.toLowerCase();
            option.style.display = text.includes(value) ? '' : 'none';
        });
    });

    input.addEventListener('blur', (e) => {
        const currentInput = e.target;
        const limit = getFilterSearchLimit(currentInput);
        currentInput.value = normalizeForSubmit(currentInput.value, limit);
    });
});

const catalogSearchInput = document.querySelector('.search-form input[name="keyWords"]');
if (catalogSearchInput) {
    catalogSearchInput.addEventListener('input', () => {
        catalogSearchInput.value = cutToMax(catalogSearchInput.value, LIMITS.catalogSearchQuery);
    });

    catalogSearchInput.addEventListener('blur', () => {
        catalogSearchInput.value = normalizeForSubmit(catalogSearchInput.value, LIMITS.catalogSearchQuery);
    });
}

document.querySelectorAll('input[name="avgRatingFrom"], input[name="avgRatingTo"]').forEach((input) => {
    input.addEventListener('input', () => {
        input.value = normalizeRatingRaw(input.value);
    });

    input.addEventListener('blur', () => {
        input.value = finalizeRatingValue(input.value);
    });
});

document.querySelectorAll('input[name="releaseYearFrom"], input[name="releaseYearTo"]').forEach((input) => {
    input.addEventListener('input', () => {
        input.value = normalizeReleaseYearRaw(input.value);
    });

    input.addEventListener('blur', () => {
        input.value = finalizeReleaseYearValue(input.value);
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

applyCatalogInputLimits(document);
