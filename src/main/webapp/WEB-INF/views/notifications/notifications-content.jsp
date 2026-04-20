<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:url var="notificationIconOnUrl" value="/assets/icons/notification-on.svg"/>
<c:url var="notificationIconOffUrl" value="/assets/icons/notification-off.svg"/>
<c:url var="deleteIconUrl" value="/assets/icons/trash.svg"/>

<div class="notifications-panel js-notifications-state"
     data-tab="${tab}"
     data-notification-count="${notificationCount}"
     data-unread-notification-count="${unreadNotificationCount}"
     data-has-unread-notifications="${hasUnreadNotifications}">


    <c:choose>
        <c:when test="${tab == 'subscriptions'}">
            <div class="notifications-toolbar-row notifications-search-row">
                <form method="get"
                      action="<c:url value='/notifications'/>"
                      class="search-form notifications-ajax-form notifications-search-form">
                    <input type="hidden" name="tab" value="subscriptions">
                    <input type="text"
                           name="q"
                           value="${q}"
                           maxlength="255"
                           placeholder="Поиск по названию или оригиналу">
                    <button type="submit" class="btn">Найти</button>
                </form>
            </div>

            <c:choose>
                <c:when test="${empty subscriptionItems}">
                    <div class="notification-empty">
                        <c:choose>
                            <c:when test="${not empty q}">
                                По вашему запросу ничего не найдено.
                            </c:when>
                            <c:otherwise>
                                Вы пока не подписаны ни на один тайтл.
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="notifications-subscriptions-grid">
                        <c:forEach var="item" items="${subscriptionItems}">
                            <article class="notification-sub-card">
                                <div class="notification-sub-cover-wrap">
                                    <a href="<c:url value='/comics/${item.comicId}'/>" class="notification-sub-cover-link">
                                        <img src="<c:url value='/assets/covers/${item.cover}'/>" alt="${item.title}">
                                    </a>

                                    <button type="button"
                                            class="btn btn-outline icon-only-btn notification-action-btn notification-sub-toggle js-notification-toggle is-active"
                                            data-comic-id="${item.comicId}"
                                            data-authenticated="true"
                                            data-icon-only="true"
                                            data-toggle-url="<c:url value='/notifications/toggle'/>"
                                            data-icon-on-url="${notificationIconOnUrl}"
                                            data-icon-off-url="${notificationIconOffUrl}"
                                            data-subscribed="true"
                                            title="Отключить оповещения"
                                            aria-label="Отключить оповещения">
                                        <span class="btn-icon js-notification-toggle-icon"
                                              style="-webkit-mask-image:url('${notificationIconOnUrl}'); mask-image:url('${notificationIconOnUrl}');"></span>
                                    </button>

                                </div>

                                <div class="notification-sub-body">
                                    <h3 class="notification-sub-title">
                                        <a href="<c:url value='/comics/${item.comicId}'/>">${item.title}</a>
                                    </h3>

                                    <c:if test="${not empty item.originalTitle}">
                                        <p class="notification-sub-original">(${item.originalTitle})</p>
                                    </c:if>

                                    <div class="notification-sub-meta">
                                        <c:if test="${item.releaseYear != null}">
                                            <span>${item.releaseYear}</span>
                                        </c:if>

                                        <c:if test="${item.avgRating != null}">
                                            <span>★ <fmt:formatNumber value="${item.avgRating}" pattern="0.00"/></span>
                                        </c:if>
                                    </div>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:when>

        <c:otherwise>
            <div class="notifications-toolbar-row notifications-sort-row">
                <form method="get"
                      action="<c:url value='/notifications'/>"
                      class="notifications-ajax-form notifications-sort-form">
                    <input type="hidden" name="tab" value="feed">

                    <label for="notificationSortField">Сортировка:</label>
                    <select id="notificationSortField" name="sortField">
                        <option value="createdAt" ${sortField == 'createdAt' ? 'selected' : ''}>По дате добавления</option>
                        <option value="type" ${sortField == 'type' ? 'selected' : ''}>По типу уведомления</option>
                    </select>

                    <select name="sortDirection">
                        <option value="desc" ${sortDirection == 'desc' ? 'selected' : ''}>По убыванию</option>
                        <option value="asc" ${sortDirection == 'asc' ? 'selected' : ''}>По возрастанию</option>
                    </select>

                    <button type="submit" class="btn">Применить</button>
                </form>
            </div>

            <c:choose>
                <c:when test="${empty feedItems}">
                    <div class="notification-empty">
                        У вас пока нет оповещений.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="notifications-feed">
                        <c:forEach var="item" items="${feedItems}">
                            <article class="notification-card ${item.read ? '' : 'is-unread'}">
                                <c:if test="${not empty item.cover}">
                                    <div class="notification-cover-wrap">
                                        <img class="notification-cover"
                                             src="<c:url value='/assets/covers/${item.cover}'/>"
                                             alt="${item.subject}">
                                    </div>
                                </c:if>

                                <div class="notification-body">
                                    <div class="notification-title-row">
                                        <div class="notification-item-title-wrap">
                                            <div class="notification-item-title">${item.title}</div>

                                            <c:if test="${not item.read}">
                                                <span class="notification-unread-badge">Новое</span>
                                            </c:if>
                                        </div>

                                        <div class="notification-head-actions">
                                            <time class="notification-item-time" datetime="${item.createdAtIso}">
                                                ${item.createdAtFormatted}
                                            </time>

                                            <button type="button"
                                                    class="notification-delete-btn js-notification-delete"
                                                    data-notification-id="${item.id}"
                                                    data-delete-url="<c:url value='/notifications/delete'/>"
                                                    title="Удалить оповещение"
                                                    aria-label="Удалить оповещение">
                                                <span class="profile-menu-icon"
                                                      style="-webkit-mask-image:url('${deleteIconUrl}'); mask-image:url('${deleteIconUrl}');"></span>
                                            </button>
                                        </div>
                                    </div>

                                    <c:choose>
                                        <c:when test="${item.clickable}">
                                            <a href="${pageContext.request.contextPath}${item.linkPath}" class="notification-link-block">
                                                <c:if test="${not empty item.subject}">
                                                    <div class="notification-item-subject">${item.subject}</div>
                                                </c:if>

                                                <c:if test="${not empty item.details}">
                                                    <div class="notification-item-details">${item.details}</div>
                                                </c:if>
                                            </a>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="notification-static-block">
                                                <c:if test="${not empty item.subject}">
                                                    <div class="notification-item-subject">${item.subject}</div>
                                                </c:if>

                                                <c:if test="${not empty item.details}">
                                                    <div class="notification-item-details">${item.details}</div>
                                                </c:if>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>

    <c:if test="${totalPages > 1}">
        <div class="pagination-management">
            <ul class="pagination">
                <c:if test="${currentPage > 1}">
                    <c:url var="notificationsPrevUrl" value="/notifications">
                        <c:param name="tab" value="${tab}"/>
                        <c:param name="page" value="${currentPage - 2}"/>

                        <c:if test="${tab == 'subscriptions' and not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>

                        <c:if test="${tab == 'feed'}">
                            <c:param name="sortField" value="${sortField}"/>
                            <c:param name="sortDirection" value="${sortDirection}"/>
                        </c:if>
                    </c:url>

                    <li>
                        <a href="${notificationsPrevUrl}" class="pagination-icon-link">
                            <span class="nav-icon"
                                  style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span>
                        </a>
                    </li>
                </c:if>

                <c:if test="${beginPage > 1}">
                    <c:url var="notificationsFirstUrl" value="/notifications">
                        <c:param name="tab" value="${tab}"/>
                        <c:param name="page" value="0"/>

                        <c:if test="${tab == 'subscriptions' and not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>

                        <c:if test="${tab == 'feed'}">
                            <c:param name="sortField" value="${sortField}"/>
                            <c:param name="sortDirection" value="${sortDirection}"/>
                        </c:if>
                    </c:url>

                    <li><a href="${notificationsFirstUrl}">1</a></li>
                </c:if>

                <c:if test="${showLeftDots}">
                    <li class="disabled"><span>...</span></li>
                </c:if>

                <c:forEach var="i" begin="${beginPage}" end="${endPage}">
                    <c:url var="notificationsPageUrl" value="/notifications">
                        <c:param name="tab" value="${tab}"/>
                        <c:param name="page" value="${i - 1}"/>

                        <c:if test="${tab == 'subscriptions' and not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>

                        <c:if test="${tab == 'feed'}">
                            <c:param name="sortField" value="${sortField}"/>
                            <c:param name="sortDirection" value="${sortDirection}"/>
                        </c:if>
                    </c:url>

                    <li>
                        <a href="${notificationsPageUrl}" class="${i == currentPage ? 'active-page' : ''}">
                            ${i}
                        </a>
                    </li>
                </c:forEach>

                <c:if test="${showRightDots}">
                    <li class="disabled"><span>...</span></li>
                </c:if>

                <c:if test="${endPage < totalPages}">
                    <c:url var="notificationsLastUrl" value="/notifications">
                        <c:param name="tab" value="${tab}"/>
                        <c:param name="page" value="${totalPages - 1}"/>

                        <c:if test="${tab == 'subscriptions' and not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>

                        <c:if test="${tab == 'feed'}">
                            <c:param name="sortField" value="${sortField}"/>
                            <c:param name="sortDirection" value="${sortDirection}"/>
                        </c:if>
                    </c:url>

                    <li><a href="${notificationsLastUrl}">${totalPages}</a></li>
                </c:if>

                <c:if test="${currentPage < totalPages}">
                    <c:url var="notificationsNextUrl" value="/notifications">
                        <c:param name="tab" value="${tab}"/>
                        <c:param name="page" value="${currentPage}"/>

                        <c:if test="${tab == 'subscriptions' and not empty q}">
                            <c:param name="q" value="${q}"/>
                        </c:if>

                        <c:if test="${tab == 'feed'}">
                            <c:param name="sortField" value="${sortField}"/>
                            <c:param name="sortDirection" value="${sortDirection}"/>
                        </c:if>
                    </c:url>

                    <li>
                        <a href="${notificationsNextUrl}" class="pagination-icon-link">
                            <span class="nav-icon"
                                  style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>');"></span>
                        </a>
                    </li>
                </c:if>
            </ul>
        </div>
    </c:if>
</div>
