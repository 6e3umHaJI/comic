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
                <input type="text"
                       name="keyWords"
                       placeholder="Поиск по названию или оригиналу"
                       value="${searchCriteria.keyWords}">
                <button type="submit" class="btn">Найти</button>
            </form>
        </div>

        <div class="top-controls">
            <div class="filters-control">
                <button class="btn btn-outline"
                        type="button"
                        id="openFiltersBtn">
                    Фильтры
                </button>
            </div>

            <div class="sort-inline">
                <form id="sortForm" class="sort-form">
                    <label for="sortField">Сортировать по:</label>
                    <select name="sortField" id="sortField">
                        <option value="popularityScore" ${searchCriteria.sortField == 'popularityScore' ? 'selected' : ''}>Популярности</option>
                        <option value="avgRating" ${searchCriteria.sortField == 'avgRating' ? 'selected' : ''}>Рейтингу</option>
                        <option value="title" ${searchCriteria.sortField == 'title' ? 'selected' : ''}>Названию</option>
                        <option value="releaseYear" ${searchCriteria.sortField == 'releaseYear' ? 'selected' : ''}>Году релиза</option>
                        <option value="createdAt" ${searchCriteria.sortField == 'createdAt' ? 'selected' : ''}>Дате добавления</option>
                        <option value="updatedAt" ${searchCriteria.sortField == 'updatedAt' ? 'selected' : ''}>Обновлению</option>
                    </select>

                    <select name="sortDirection" id="sortDirection">
                        <option value="asc" ${searchCriteria.sortDirection == 'asc' ? 'selected' : ''}>По возрастанию</option>
                        <option value="desc" ${searchCriteria.sortDirection == 'desc' ? 'selected' : ''}>По убыванию</option>
                    </select>
                </form>
            </div>

            <div class="view-toggle">
                <button type="button"
                        class="btn icon-only-btn"
                        onclick="switchView('card'); return false;"
                        title="Карточками"
                        aria-label="Карточками">
                    <span class="btn-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/card.svg"/>'); mask-image:url('<c:url value="/assets/icons/card.svg"/>');"></span>
                </button>
                <button type="button"
                        class="btn icon-only-btn"
                        onclick="switchView('list'); return false;"
                        title="Списком"
                        aria-label="Списком">
                    <span class="btn-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/list.svg"/>'); mask-image:url('<c:url value="/assets/icons/list.svg"/>');"></span>
                </button>
            </div>
        </div>

        <div class="catalog-layout">
            <div class="filter-overlay" id="filterOverlay"></div>

            <aside class="filter-container" id="filterContainer">
                <div class="filter-panel-header">
                    <h3>Фильтры</h3>
                    <button type="button"
                            class="filter-close-btn"
                            id="closeFiltersBtn"
                            aria-label="Закрыть">
                        &times;
                    </button>
                </div>

                <form method="POST" class="filter-panel">
                    <jsp:include page="filter-section.jsp"/>

                    <div class="filter-buttons">
                        <button type="submit" class="btn">Применить</button>
                        <button type="button" class="btn btn-outline" onclick="resetFilters()">Сбросить</button>
                    </div>
                </form>
            </aside>

            <div class="content">
                <div id="catalogContent">
                    <jsp:include page="catalog-content.jsp"/>
                </div>
            </div>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/catalog.js'/>"></script>
</body>
</html>
