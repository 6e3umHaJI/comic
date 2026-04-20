<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="collections-layout">
    <aside class="collections-sidebar">
        <div class="collections-sidebar-head">
            <div class="collections-sidebar-title-row">
                <h2>Коллекция</h2>

                <button type="button"
                        class="collection-icon-action"
                        id="createSectionBtn"
                        title="Добавить категорию"
                        aria-label="Добавить категорию">
                    <span class="collection-tool-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/plus.svg"/>'); mask-image:url('<c:url value="/assets/icons/plus.svg"/>');"></span>
                </button>
            </div>

            <div class="collections-create-inline" id="createSectionInline" hidden>
                <input type="text"
                       id="createSectionInput"
                       class="auth-input"
                       maxlength="100"
                       placeholder="Название...">
                <div class="collections-create-actions">
                    <button type="button" class="btn" id="confirmCreateSectionBtn">Создать</button>
                    <button type="button" class="btn btn-outline" id="cancelCreateSectionBtn">Отмена</button>
                </div>
            </div>

            <div class="collections-inline-notice" id="collectionsSidebarNotice" hidden></div>
        </div>

        <div class="collections-tabs">
            <c:forEach items="${sections}" var="section">
                <button type="button"
                        class="collection-tab ${section.id == activeSection.id ? 'active' : ''}"
                        data-section-id="${section.id}">
                    <span class="collection-tab-main">
                        <span class="collection-tab-name">${section.name}</span>
                        <span class="collection-tab-count">(${section.comicsCount})</span>
                    </span>
                </button>
            </c:forEach>
        </div>
    </aside>

    <section class="collections-main"
             data-active-section-id="${activeSection.id}"
             data-view-mode="${viewMode}"
             data-search-query="${q}"
             data-sort-field="${sortField}"
             data-sort-direction="${sortDirection}">

        <div class="collections-main-head">
            <div class="collections-main-title-wrap">
                <div class="collections-title-row">
                    <h3>${activeSection.name}</h3>

                    <c:if test="${!activeSection.isDefault}">
                        <div class="collections-title-tools">
                            <button type="button"
                                    class="collection-icon-action"
                                    id="renameSectionToggleBtn"
                                    title="Переименовать"
                                    aria-label="Переименовать">
                                <span class="collection-tool-icon"
                                      style="-webkit-mask-image:url('<c:url value="/assets/icons/edit.svg"/>'); mask-image:url('<c:url value="/assets/icons/edit.svg"/>');"></span>
                            </button>

                            <button type="button"
                                    class="collection-icon-action"
                                    id="deleteSectionToggleBtn"
                                    data-section-id="${activeSection.id}"
                                    data-has-comics="${hasSavedComics}"
                                    title="Удалить"
                                    aria-label="Удалить">
                                <span class="collection-tool-icon"
                                      style="-webkit-mask-image:url('<c:url value="/assets/icons/trash.svg"/>'); mask-image:url('<c:url value="/assets/icons/trash.svg"/>');"></span>
                            </button>
                        </div>
                    </c:if>
                </div>

                <div class="collections-inline-notice" id="collectionsMainNotice" hidden></div>

                <c:if test="${!activeSection.isDefault}">
                    <div class="collections-rename-inline" id="renameSectionInline" hidden>
                        <input type="text"
                               id="renameSectionInput"
                               class="auth-input"
                               value="${activeSection.name}"
                               maxlength="100">
                        <button type="button"
                                class="btn btn-outline"
                                id="confirmRenameSectionBtn"
                                data-section-id="${activeSection.id}">
                            Сохранить
                        </button>
                        <button type="button"
                                class="btn btn-outline"
                                id="cancelRenameSectionBtn">
                            Отмена
                        </button>
                    </div>
                </c:if>
            </div>
        </div>

        <c:if test="${hasSavedComics}">
        <div class="collections-toolbar-row collections-search-row">
            <form class="search-form collections-search-form" id="collectionsSearchForm">
                <input type="text"
                       id="collectionsSearchInput"
                       name="q"
                       maxlength="255"
                       placeholder="Поиск по названию или оригиналу"
                       value="${q}">
                <button type="submit" class="btn">Найти</button>
            </form>
        </div>
        <c:if test="${hasVisibleSavedComics}">
        <div class="top-controls">
            <div class="sort-inline">
                <form id="collectionsSortForm" class="sort-form">
                    <label for="collectionsSortField">Сортировать по:</label>

                    <select name="sortField" id="collectionsSortField">
                        <option value="addedAt" ${sortField == 'addedAt' ? 'selected' : ''}>Добавлению в коллекцию</option>
                        <option value="popularityScore" ${sortField == 'popularityScore' ? 'selected' : ''}>Популярности</option>
                        <option value="avgRating" ${sortField == 'avgRating' ? 'selected' : ''}>Рейтингу</option>
                        <option value="title" ${sortField == 'title' ? 'selected' : ''}>Названию</option>
                        <option value="releaseYear" ${sortField == 'releaseYear' ? 'selected' : ''}>Году релиза</option>
                        <option value="createdAt" ${sortField == 'createdAt' ? 'selected' : ''}>Дате добавления</option>
                        <option value="updatedAt" ${sortField == 'updatedAt' ? 'selected' : ''}>Обновлению</option>
                    </select>

                    <select name="sortDirection" id="collectionsSortDirection">
                        <option value="asc" ${sortDirection == 'asc' ? 'selected' : ''}>По возрастанию</option>
                        <option value="desc" ${sortDirection == 'desc' ? 'selected' : ''}>По убыванию</option>
                    </select>
                </form>
            </div>

            <div class="view-toggle">
                <button type="button"
                        class="btn ${viewMode == 'card' ? '' : 'btn-outline'}"
                        onclick="loadCollectionSection(${activeSection.id}, 0, 'card'); return false;"
                        aria-label="Карточки">
                    <span class="btn-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/card.svg"/>'); mask-image:url('<c:url value="/assets/icons/card.svg"/>');"></span>
                </button>
                <button type="button"
                        class="btn ${viewMode == 'list' ? '' : 'btn-outline'}"
                        onclick="loadCollectionSection(${activeSection.id}, 0, 'list'); return false;"
                        aria-label="Список">
                    <span class="btn-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/list.svg"/>'); mask-image:url('<c:url value="/assets/icons/list.svg"/>');"></span>
                </button>
            </div>
        </div>
            <div class="collections-actions collections-toolbar-row collections-bulk-row">
                <button type="button" class="btn btn-outline" id="moveSelectedBtn">Перенести выбранные</button>
                <button type="button" class="btn btn-outline" id="removeSelectedBtn">Удалить из категории</button>
            </div>
            <div class="collections-actions collections-toolbar-row collections-selection-row">
                <button type="button" class="collections-text-action" id="selectAllBtn">Выбрать все</button>
                <button type="button" class="collections-text-action" id="clearSelectionBtn">Снять выбор</button>
            </div>
        </c:if>
        </c:if>

        <c:choose>
            <c:when test="${empty savedComics}">
                <div class="no-results">
                    <c:choose>
                        <c:when test="${not empty q}">
                            <p>Ничего не найдено по вашему запросу</p>
                        </c:when>
                        <c:otherwise>
                            <p>В этой категории пока нет тайтлов</p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:when>


            <c:otherwise>
                <div class="comics-container ${viewMode == 'list' ? 'list-view' : 'card-view'}">
                    <c:forEach items="${savedComics}" var="saved">
                        <c:set var="comic" value="${saved.comic}"/>

                        <c:choose>
                            <c:when test="${viewMode == 'list'}">
                                <div class="list-comic">
                                    <label class="collection-select-box">
                                        <input type="checkbox" class="collection-comic-check" value="${comic.id}">
                                        <span class="collection-select-mark"></span>
                                    </label>

                                    <img src="<c:url value='/assets/covers/${comic.cover}'/>"
                                         alt="${comic.title}"
                                         class="comic-list-cover">

                                    <div class="list-info">
                                        <h3><a href="<c:url value='/comics/${comic.id}'/>">${comic.title}</a></h3>
                                        <c:if test="${not empty comic.originalTitle}">
                                            <p class="original-title">(${comic.originalTitle})</p>
                                        </c:if>
                                        <p class="release"><b>Релиз:</b> ${comic.releaseYear}</p>
                                        <p class="short-desc">${comic.shortDescription}</p>
                                        <p class="meta">
                                            <span class="rating">★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></span>
                                        </p>
                                    </div>
                                </div>
                            </c:when>

                            <c:otherwise>
                                <div class="card-comic">
                                    <label class="collection-select-box card-select-box">
                                        <input type="checkbox" class="collection-comic-check" value="${comic.id}">
                                        <span class="collection-select-mark"></span>
                                        <span class="collection-select-label">Выбрать</span>
                                    </label>

                                    <a href="<c:url value='/comics/${comic.id}'/>" class="cover-link">
                                        <img src="<c:url value='/assets/covers/${comic.cover}'/>" alt="${comic.title}">
                                    </a>
                                    <h4><a href="<c:url value='/comics/${comic.id}'/>">${comic.title}</a></h4>
                                    <c:if test="${not empty comic.originalTitle}">
                                        <p class="orig-name">(${comic.originalTitle})</p>
                                    </c:if>
                                    <p>${comic.releaseYear}</p>
                                    <p>★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></p>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>

        <c:if test="${totalPages > 1}">
            <div class="pagination-management">
                <ul class="pagination">
                    <c:if test="${currentPage > 1}">
                        <li><a href="#" onclick="loadCollectionSection(${activeSection.id}, ${currentPage - 2}, '${viewMode}'); return false;"><span class="nav-icon"
                                                                                                                                                                             style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span></a></li>
                    </c:if>

                    <c:if test="${beginPage > 1}">
                        <li><a href="#" onclick="loadCollectionSection(${activeSection.id}, 0, '${viewMode}'); return false;">1</a></li>
                    </c:if>

                    <c:if test="${showLeftDots}">
                        <li class="disabled"><span>...</span></li>
                    </c:if>

                    <c:forEach var="i" begin="${beginPage}" end="${endPage}">
                        <li>
                            <a href="#"
                               onclick="loadCollectionSection(${activeSection.id}, ${i - 1}, '${viewMode}'); return false;"
                               class="${i == currentPage ? 'active-page' : ''}">
                                ${i}
                            </a>
                        </li>
                    </c:forEach>

                    <c:if test="${showRightDots}">
                        <li class="disabled"><span>...</span></li>
                    </c:if>

                    <c:if test="${endPage < totalPages}">
                        <li><a href="#" onclick="loadCollectionSection(${activeSection.id}, ${totalPages - 1}, '${viewMode}'); return false;">${totalPages}</a></li>
                    </c:if>

                    <c:if test="${currentPage < totalPages}">
                        <li><a href="#" onclick="loadCollectionSection(${activeSection.id}, ${currentPage}, '${viewMode}'); return false;"><span class="nav-icon"
                                                                                                                                                                         style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>');"></span></a></li>
                    </c:if>
                </ul>
            </div>
        </c:if>
    </section>
</div>
