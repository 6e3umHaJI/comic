<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="translation-cabinet-state"
     data-q="${q}"
     data-sort-direction="${sortDirection}"
     data-current-page="${currentPageZeroBased}">
</div>

<div class="translation-cabinet-panel">
    <c:if test="${not empty uploadMessage}">
        <div class="translation-cabinet-empty"><c:out value="${uploadMessage}"/></div>
    </c:if>

    <c:if test="${not empty uploadError}">
        <div class="translation-cabinet-empty"><c:out value="${uploadError}"/></div>
    </c:if>

    <div class="translation-cabinet-toolbar-row translation-cabinet-search-row">
        <form method="get"
              action="<c:url value='/admin/translations/review'/>"
              class="translation-cabinet-search-form translation-cabinet-ajax-form">
            <input type="hidden" name="sortDirection" value="${sortDirection}">
            <label class="visually-hidden" for="translationCabinetQ">Поиск по названию</label>
            <input type="text"
                   id="translationCabinetQ"
                   name="q"
                   value="${q}"
                   maxlength="255"
                   placeholder="Поиск по названию, переводу или автору">
            <button type="submit" class="btn">Найти</button>
        </form>
    </div>

    <div class="translation-cabinet-toolbar-row translation-cabinet-sort-row">
        <form method="get"
              action="<c:url value='/admin/translations/review'/>"
              class="translation-cabinet-sort-form translation-cabinet-ajax-form">
            <input type="hidden" name="q" value="${q}">
            <label for="translationCabinetSortDirection">Сортировка:</label>
            <select id="translationCabinetSortDirection" name="sortDirection">
                <option value="desc" ${sortDirection == 'desc' ? 'selected' : ''}>Сначала новые</option>
                <option value="asc" ${sortDirection == 'asc' ? 'selected' : ''}>Сначала старые</option>
            </select>
            <button type="submit" class="btn">Применить</button>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty translations}">
            <div class="translation-cabinet-empty">
                <c:choose>
                    <c:when test="${not empty q}">
                        По вашему запросу ничего не найдено.
                    </c:when>
                    <c:otherwise>
                        На проверке сейчас нет ни одного перевода.
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>

        <c:otherwise>
            <div class="translation-cabinet-list">
                <c:forEach var="item" items="${translations}">
                    <article class="translation-cabinet-item">
                        <div class="translation-cabinet-cover-wrap">
                            <a href="<c:url value='/comics/${item.chapter.comic.id}'/>" class="translation-cabinet-cover-link">
                                <img class="translation-cabinet-cover"
                                     src="<c:url value='/assets/covers/${item.chapter.comic.cover}'/>"
                                     alt="<c:out value='${item.chapter.comic.title}'/>">
                            </a>
                        </div>

                        <div class="translation-cabinet-body">
                            <div class="translation-cabinet-title-row">
                                <div class="translation-cabinet-item-title-wrap">
                                    <a href="<c:url value='/translations/${item.id}/preview'/>"
                                       class="translation-cabinet-item-title">
                                        <c:out value="${item.chapter.comic.title}"/>
                                    </a>
                                </div>

                                <div class="translation-cabinet-item-time">
                                    <c:out value="${item.createdAtFormatted}"/>
                                </div>
                            </div>

                            <div class="translation-cabinet-static-block">
                                <div class="translation-cabinet-item-subject">
                                    <c:out value="${item.title}"/>
                                </div>

                                <div class="translation-cabinet-item-details">
                                    Глава <c:out value="${item.chapter.chapterNumber}"/>
                                    <br>
                                    Язык: <c:out value="${item.language.name}"/>
                                    <br>
                                    Автор: <c:out value="${item.user != null ? item.user.username : 'Не указан'}"/>
                                    <br>
                                    Страниц: ${pageCounts[item.id]}
                                    <br>
                                    Статус: <c:out value="${item.reviewStatus.name}"/>
                                </div>
                            </div>

                            <div class="translation-cabinet-actions">
                                <a href="<c:url value='/translations/${item.id}/preview'/>" class="btn btn-outline">
                                    Открыть
                                </a>
                                <a href="<c:url value='/comics/${item.chapter.comic.id}'/>" class="btn btn-outline">
                                    Комикс
                                </a>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>

            <c:if test="${totalPages > 1}">
                <div class="pagination-management">
                    <ul class="pagination">
                        <c:if test="${currentPage > 1}">
                            <c:url var="prevUrl" value="/admin/translations/review">
                                <c:param name="page" value="${currentPage - 2}"/>
                                <c:param name="sortDirection" value="${sortDirection}"/>
                                <c:if test="${not empty q}">
                                    <c:param name="q" value="${q}"/>
                                </c:if>
                            </c:url>
                            <li>
                                <a href="${prevUrl}" class="pagination-icon-link">
                                    <span class="nav-icon"
                                          style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span>
                                </a>
                            </li>
                        </c:if>

                        <c:if test="${beginPage > 1}">
                            <c:url var="firstUrl" value="/admin/translations/review">
                                <c:param name="page" value="0"/>
                                <c:param name="sortDirection" value="${sortDirection}"/>
                                <c:if test="${not empty q}">
                                    <c:param name="q" value="${q}"/>
                                </c:if>
                            </c:url>
                            <li><a href="${firstUrl}">1</a></li>
                            <c:if test="${beginPage > 2}">
                                <li><span>...</span></li>
                            </c:if>
                        </c:if>

                        <c:forEach var="i" begin="${beginPage}" end="${endPage}">
                            <c:url var="pageUrl" value="/admin/translations/review">
                                <c:param name="page" value="${i - 1}"/>
                                <c:param name="sortDirection" value="${sortDirection}"/>
                                <c:if test="${not empty q}">
                                    <c:param name="q" value="${q}"/>
                                </c:if>
                            </c:url>
                            <li>
                                <a href="${pageUrl}" class="${i == currentPage ? 'active-page' : ''}">
                                    ${i}
                                </a>
                            </li>
                        </c:forEach>

                        <c:if test="${endPage < totalPages}">
                            <c:if test="${endPage < totalPages - 1}">
                                <li><span>...</span></li>
                            </c:if>
                            <c:url var="lastUrl" value="/admin/translations/review">
                                <c:param name="page" value="${totalPages - 1}"/>
                                <c:param name="sortDirection" value="${sortDirection}"/>
                                <c:if test="${not empty q}">
                                    <c:param name="q" value="${q}"/>
                                </c:if>
                            </c:url>
                            <li><a href="${lastUrl}">${totalPages}</a></li>
                        </c:if>

                        <c:if test="${currentPage < totalPages}">
                            <c:url var="nextUrl" value="/admin/translations/review">
                                <c:param name="page" value="${currentPage}"/>
                                <c:param name="sortDirection" value="${sortDirection}"/>
                                <c:if test="${not empty q}">
                                    <c:param name="q" value="${q}"/>
                                </c:if>
                            </c:url>
                            <li>
                                <a href="${nextUrl}" class="pagination-icon-link">
                                    <span class="nav-icon"
                                          style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>');"></span>
                                </a>
                            </li>
                        </c:if>
                    </ul>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>
