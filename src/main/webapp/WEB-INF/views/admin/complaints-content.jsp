<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="admin-complaints-state"
     data-scope="${scope}"
     data-q="${q}"
     data-sort-direction="${sortDirection}"
     data-selected-type-id="${selectedTypeId}">

    <form method="get"
          action="<c:url value='/admin/complaints'/>"
          class="admin-complaints-filters admin-complaints-ajax-form">
        <input type="hidden" name="scope" value="${scope}"/>

        <div class="admin-complaints-filter-group admin-complaints-search-group">
            <label for="complaintSearchQ">Поиск по названию комикса</label>
            <input type="text"
                   id="complaintSearchQ"
                   name="q"
                   value="${q}"
                   placeholder="Введите название">
        </div>

        <div class="admin-complaints-filter-group">
            <label for="typeId">Тип жалобы</label>
            <select id="typeId" name="typeId">
                <option value="">Все типы</option>
                <c:forEach var="type" items="${complaintTypes}">
                    <option value="${type.id}" ${selectedTypeId != null && selectedTypeId == type.id ? 'selected' : ''}>
                        ${type.name}
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="admin-complaints-filter-group">
            <label for="sortDirection">Дата добавления</label>
            <select id="sortDirection" name="sortDirection">
                <option value="desc" ${sortDirection == 'desc' ? 'selected' : ''}>Сначала новые</option>
                <option value="asc" ${sortDirection == 'asc' ? 'selected' : ''}>Сначала старые</option>
            </select>
        </div>

        <button type="submit" class="btn">Применить</button>
    </form>

    <c:choose>
        <c:when test="${empty complaints}">
            <div class="admin-complaints-empty">
                По выбранным параметрам жалоб не найдено.
            </div>
        </c:when>

        <c:otherwise>
            <div class="admin-complaints-list">
                <c:forEach var="item" items="${complaints}">
                    <article class="admin-complaint-card">
                        <c:choose>
                            <c:when test="${not empty item.targetUrl}">
                                <a href="<c:url value='${item.targetUrl}'/>" class="admin-complaint-main">
                                    <c:if test="${not empty item.cover}">
                                        <div class="admin-complaint-cover-wrap">
                                            <img class="admin-complaint-cover"
                                                 src="<c:url value='/assets/covers/${item.cover}'/>"
                                                 alt="${item.targetTitle}">
                                        </div>
                                    </c:if>

                                    <div class="admin-complaint-body">
                                        <div class="admin-complaint-top">
                                            <div class="admin-complaint-title-wrap">
                                                <div class="admin-complaint-title">${item.targetTitle}</div>
                                                <span class="admin-complaint-type-badge">${item.typeName}</span>
                                            </div>

                                            <div class="admin-complaint-date">${item.createdAtFormatted}</div>
                                        </div>

                                        <c:if test="${not empty item.targetSubtitle}">
                                            <div class="admin-complaint-subtitle">${item.targetSubtitle}</div>
                                        </c:if>

                                        <div class="admin-complaint-meta">
                                            <span>Пользователь: ${item.username}</span>
                                            <span>${item.email}</span>
                                        </div>

                                        <div class="admin-complaint-description-title">Описание</div>
                                        <div class="admin-complaint-description">${item.description}</div>
                                    </div>
                                </a>
                            </c:when>

                            <c:otherwise>
                                <div class="admin-complaint-main admin-complaint-main-static">
                                    <c:if test="${not empty item.cover}">
                                        <div class="admin-complaint-cover-wrap">
                                            <img class="admin-complaint-cover"
                                                 src="<c:url value='/assets/covers/${item.cover}'/>"
                                                 alt="${item.targetTitle}">
                                        </div>
                                    </c:if>

                                    <div class="admin-complaint-body">
                                        <div class="admin-complaint-top">
                                            <div class="admin-complaint-title-wrap">
                                                <div class="admin-complaint-title">${item.targetTitle}</div>
                                                <span class="admin-complaint-type-badge">${item.typeName}</span>
                                            </div>

                                            <div class="admin-complaint-date">${item.createdAtFormatted}</div>
                                        </div>

                                        <c:if test="${not empty item.targetSubtitle}">
                                            <div class="admin-complaint-subtitle">${item.targetSubtitle}</div>
                                        </c:if>

                                        <div class="admin-complaint-meta">
                                            <span>Пользователь: ${item.username}</span>
                                            <span>${item.email}</span>
                                        </div>

                                        <div class="admin-complaint-description-title">Описание</div>
                                        <div class="admin-complaint-description">${item.description}</div>
                                    </div>
                                </div>
                            </c:otherwise>
                        </c:choose>

                        <div class="admin-complaint-side">
                            <div class="admin-complaint-status-badge
                                        ${item.statusName == 'Ожидание' ? 'is-pending' : ''}
                                        ${item.statusName == 'На рассмотрении' ? 'is-review' : ''}
                                        ${item.statusName == 'Решена' ? 'is-success' : ''}
                                        ${item.statusName == 'Отклонена' ? 'is-rejected' : ''}">
                                ${item.statusName}
                            </div>

                            <form method="post"
                                  action="<c:url value='/admin/complaints/status'/>"
                                  class="admin-complaint-status-form">
                                <input type="hidden" name="complaintId" value="${item.id}"/>
                                <input type="hidden" name="scope" value="${scope}"/>
                                <input type="hidden" name="sortDirection" value="${sortDirection}"/>
                                <input type="hidden" name="page" value="${currentPage > 0 ? currentPage - 1 : 0}"/>
                                <input type="hidden" name="q" value="${q}"/>

                                <c:if test="${selectedTypeId != null}">
                                    <input type="hidden" name="typeId" value="${selectedTypeId}"/>
                                </c:if>

                                <label for="status-${item.id}" class="admin-complaint-status-label">Статус</label>
                                <select id="status-${item.id}"
                                        name="statusId"
                                        class="admin-complaint-status-select js-admin-complaint-status-select"
                                        data-initial-value="${item.statusId}">
                                    <c:forEach var="status" items="${statusOptions}">
                                        <option value="${status.id}" ${item.statusId == status.id ? 'selected' : ''}>
                                            ${status.name}
                                        </option>
                                    </c:forEach>
                                </select>

                                <button type="submit"
                                        class="btn btn-outline admin-complaint-confirm-btn js-admin-complaint-confirm-btn hidden">
                                    Подтвердить
                                </button>
                            </form>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${totalPages > 1}">
        <div class="pagination-management">
            <ul class="pagination">
                <c:if test="${currentPage > 1}">
                    <c:url var="prevUrl" value="/admin/complaints">
                        <c:param name="scope" value="${scope}"/>
                        <c:param name="page" value="${currentPage - 2}"/>
                        <c:param name="sortDirection" value="${sortDirection}"/>
                        <c:if test="${selectedTypeId != null}">
                            <c:param name="typeId" value="${selectedTypeId}"/>
                        </c:if>
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
                    <c:url var="firstUrl" value="/admin/complaints">
                        <c:param name="scope" value="${scope}"/>
                        <c:param name="page" value="0"/>
                        <c:param name="sortDirection" value="${sortDirection}"/>
                        <c:if test="${selectedTypeId != null}">
                            <c:param name="typeId" value="${selectedTypeId}"/>
                        </c:if>
                        <c:if test="${not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>
                    </c:url>

                    <li><a href="${firstUrl}">1</a></li>
                </c:if>

                <c:if test="${showLeftDots}">
                    <li class="disabled"><span>...</span></li>
                </c:if>

                <c:forEach var="i" begin="${beginPage}" end="${endPage}">
                    <c:url var="pageUrl" value="/admin/complaints">
                        <c:param name="scope" value="${scope}"/>
                        <c:param name="page" value="${i - 1}"/>
                        <c:param name="sortDirection" value="${sortDirection}"/>
                        <c:if test="${selectedTypeId != null}">
                            <c:param name="typeId" value="${selectedTypeId}"/>
                        </c:if>
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

                <c:if test="${showRightDots}">
                    <li class="disabled"><span>...</span></li>
                </c:if>

                <c:if test="${endPage < totalPages}">
                    <c:url var="lastUrl" value="/admin/complaints">
                        <c:param name="scope" value="${scope}"/>
                        <c:param name="page" value="${totalPages - 1}"/>
                        <c:param name="sortDirection" value="${sortDirection}"/>
                        <c:if test="${selectedTypeId != null}">
                            <c:param name="typeId" value="${selectedTypeId}"/>
                        </c:if>
                        <c:if test="${not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>
                    </c:url>

                    <li><a href="${lastUrl}">${totalPages}</a></li>
                </c:if>

                <c:if test="${currentPage < totalPages}">
                    <c:url var="nextUrl" value="/admin/complaints">
                        <c:param name="scope" value="${scope}"/>
                        <c:param name="page" value="${currentPage}"/>
                        <c:param name="sortDirection" value="${sortDirection}"/>
                        <c:if test="${selectedTypeId != null}">
                            <c:param name="typeId" value="${selectedTypeId}"/>
                        </c:if>
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
</div>
