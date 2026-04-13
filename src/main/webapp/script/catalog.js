const filterContainer = document.getElementById('filterContainer');
const filterOverlay = document.getElementById('filterOverlay');
const openFiltersBtn = document.getElementById('openFiltersBtn');
const closeFiltersBtn = document.getElementById('closeFiltersBtn');

function openFilters() {
    filterContainer?.classList.add('active');
    filterOverlay?.classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeFilters() {
    filterContainer?.classList.remove('active');
    filterOverlay?.classList.remove('active');
    document.body.style.overflow = '';
}

openFiltersBtn?.addEventListener('click', openFilters);
closeFiltersBtn?.addEventListener('click', closeFilters);
filterOverlay?.addEventListener('click', closeFilters);

function toggleFilter() {
    const filter = document.getElementById('filterContainer');
    const overlayId = 'filterOverlay';
    let overlay = document.getElementById(overlayId);

    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = overlayId;
        overlay.classList.add('filter-overlay');
        document.body.appendChild(overlay);
        overlay.addEventListener('click', () => toggleFilter());
    }

    const active = filter.classList.toggle('active');
    overlay.classList.toggle('active', active);
}

function toggleSort() {
    const sortDropdown = document.getElementById('sortDropdown');
    sortDropdown.classList.toggle('active');
}

document.getElementById('sortField').addEventListener('change', applySort);
document.getElementById('sortDirection').addEventListener('change', applySort);

function applySort() {
    const form = document.getElementById('sortForm');
    const data = new FormData(form);
    data.set('pageNumber', 0);

    fetch('/catalog', {
        method: 'POST',
        body: data,
        headers: {'X-Requested-With': 'XMLHttpRequest'}
    })
        .then(resp => {
            if (!resp.ok) throw new Error();
            return resp.text();
        })
        .then(html => {
            document.getElementById('catalogContent').innerHTML = html;
        })
        .catch(() => {
            window.location.href = '/error';
        });
}

function loadCatalog(formData, resetPage = false) {
    if (resetPage) formData.set('pageNumber', 0);
    const params = new URLSearchParams(formData);
    window.history.replaceState({}, "", "/catalog?" + params.toString());

    fetch('/catalog', {
        method: 'POST',
        body: formData,
        headers: {'X-Requested-With': 'XMLHttpRequest'}
    })
        .then(resp => {
            if (!resp.ok) throw new Error();
            return resp.text();
        })
        .then(html => {
            document.getElementById('catalogContent').innerHTML = html;
            document.getElementById('filterContainer').scrollIntoView({behavior: "smooth"});
        })
        .catch(() => {
            window.location.href = '/error';
        });
}

document.querySelector('.filter-panel')?.addEventListener('submit', e => {
    e.preventDefault();
    const data = new FormData(e.target);

    const checkboxGroups = [
        'selectedTypes',
        'selectedComicStatuses',
        'selectedAgeRatings',
        'selectedGenres',
        'selectedTags',
        'selectedLanguages'
    ];

    checkboxGroups.forEach(name => {
        if (!data.has(name)) data.append(name, "");
    });

    loadCatalog(data, true);
});

function resetFilters() {
    const form = document.querySelector('.filter-panel');
    if (!form) return;

    form.reset();
    form.querySelectorAll('input[type=checkbox]').forEach(ch => ch.checked = false);
    form.querySelectorAll('input[type=text], input[type=date], input[type=number]').forEach(inp => inp.value = "");

    const data = new FormData();
    data.append('reset', 'true');
    loadCatalog(data, true);
}

document.querySelector('.search-form')?.addEventListener('submit', e => {
    e.preventDefault();
    const data = new FormData(e.target);
    loadCatalog(data, true);
});

document.querySelectorAll('.filter-search').forEach(input => {
    input.addEventListener('input', e => {
        const value = e.target.value.toLowerCase();
        const options = e.target.parentElement.querySelectorAll('.filter-options label');
        options.forEach(opt => {
            const text = opt.textContent.toLowerCase();
            opt.style.display = text.includes(value) ? '' : 'none';
        });
    });
});

function changePage(pageNumber) {
    const data = new FormData();
    data.append('pageNumber', pageNumber - 1);
    loadCatalog(data);
}

function switchView(mode) {
    const data = new FormData();
    data.append('viewMode', mode);
    loadCatalog(data);
}

document.addEventListener('click', e => {
    const item = e.target.closest('.card-comic, .list-comic');
    if (item && !e.target.closest('a, button, input, label')) {
        const link = item.querySelector('a');
        if (link) window.location = link.href;
    }
});
