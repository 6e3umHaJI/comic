<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <title>Каталог</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/catalog.css'/>">
</head>

<body data-authenticated="${pageContext.request.userPrincipal != null}">
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <div class="main container">
        <div class="search-section">
          <form class="search-form">
            <input type="text" name="keyWords" placeholder="Поиск по названию или оригиналу"
                   value="${searchCriteria.keyWords}">
            <button type="submit" class="btn">Найти</button>
          </form>
        </div>

        <div class="top-controls">

          <div class="filters-control">
            <button class="btn btn-outline-secondary" type="button" onclick="toggleFilter()">Фильтры</button>
          </div>

          <div class="sort-inline">
              <form id="sortForm" class="sort-form">
                  <label for="sortField">Сортировать по:</label>
                  <select name="sortField" id="sortField">
                      <option value="popularityScore" ${searchCriteria.sortField == 'popularityScore' ? 'selected' : ''}>Популярности</option>
                      <option value="title" ${searchCriteria.sortField == 'title' ? 'selected' : ''}>Названию</option>
                      <option value="releaseYear" ${searchCriteria.sortField == 'releaseYear' ? 'selected' : ''}>Году релиза</option>
                      <option value="createdAt" ${searchCriteria.sortField == 'createdAt' ? 'selected' : ''}>Дате добавления</option>
                      <option value="updatedAt" ${searchCriteria.sortField == 'updatedAt' ? 'selected' : ''}>Обновлению</option>
                  </select>

                  <select name="sortDirection" id="sortDirection">
                      <option value="asc" ${searchCriteria.sortDirection == 'asc' ? 'selected' : ''}>↑ По возрастанию</option>
                      <option value="desc" ${searchCriteria.sortDirection == 'desc' ? 'selected' : ''}>↓ По убыванию</option>
                  </select>
              </form>
          </div>

          <div class="view-toggle">
              <button type="button" class="btn icon-only-btn" onclick="switchView('list'); return false;" title="Списком" aria-label="Списком">
                  <span class="btn-icon"
                        style="-webkit-mask-image:url('<c:url value="/assets/icons/list.svg"/>'); mask-image:url('<c:url value="/assets/icons/list.svg"/>');"></span>
              </button>
              <button type="button" class="btn icon-only-btn" onclick="switchView('card'); return false;" title="Карточками" aria-label="Карточками">
                  <span class="btn-icon"
                        style="-webkit-mask-image:url('<c:url value="/assets/icons/card.svg"/>'); mask-image:url('<c:url value="/assets/icons/card.svg"/>');"></span>
              </button>
          </div>


        </div>

        <div class="catalog-layout">
            <div class="filter-container" id="filterContainer">
                <form method="POST" class="filter-panel">
                    <h3>Фильтры</h3>
                    <jsp:include page="filter-section.jsp"/>
                    <div class="filter-buttons">
                        <button type="submit" class="btn">Применить</button>
                        <button type="button" class="btn btn-outline" onclick="resetFilters()">Сбросить</button>
                    </div>
                </form>
            </div>

            <div class="content">
                <div id="catalogContent">
                    <jsp:include page="catalog-content.jsp"/>
                </div>
            </div>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script>
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
</script>
<script>
document.getElementById('sortField').addEventListener('change', applySort);
document.getElementById('sortDirection').addEventListener('change', applySort);

function applySort() {
  const form = document.getElementById('sortForm');
  const data = new FormData(form);
  data.set('pageNumber', 0);

  fetch('/catalog', {
    method: 'POST',
    body: data,
    headers: { 'X-Requested-With': 'XMLHttpRequest' }
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
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(resp => {
        if (!resp.ok) throw new Error();
        return resp.text();
    })
    .then(html => {
        document.getElementById('catalogContent').innerHTML = html;
        document.getElementById('filterContainer').scrollIntoView({ behavior: "smooth" });
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
      'selectedTranslationStatuses',
      'selectedAgeRatings',
      'selectedGenres',
      'selectedTags'
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
</script>
</body>
</html>