<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:choose>
    <c:when test="${hasNotifications}">
        <c:url var="profileNotificationIconUrl" value="/assets/icons/notification-on.svg"/>
    </c:when>
    <c:otherwise>
        <c:url var="profileNotificationIconUrl" value="/assets/icons/notification-off.svg"/>
    </c:otherwise>
</c:choose>

<div id="profileDropdown" class="profile-dropdown" hidden>
    <a href="<c:url value='/collections'/>" class="profile-dropdown-item">
        <span class="profile-menu-icon"
              style="-webkit-mask-image:url('<c:url value="/assets/icons/collection.svg"/>'); mask-image:url('<c:url value="/assets/icons/collection.svg"/>');"></span>
        <span class="profile-dropdown-item-main">
            <span>Коллекция</span>
        </span>
    </a>

    <a href="<c:url value='/notifications'/>" class="profile-dropdown-item">
        <span class="profile-menu-icon js-header-notification-icon"
              data-on-icon-url="<c:url value='/assets/icons/notification-on.svg'/>"
              data-off-icon-url="<c:url value='/assets/icons/notification-off.svg'/>"
              style="-webkit-mask-image:url('${profileNotificationIconUrl}'); mask-image:url('${profileNotificationIconUrl}');"></span>
        <span class="profile-dropdown-item-main">
            <span>Оповещения</span>
            <span class="profile-dropdown-item-count js-profile-notification-count">${notificationCountLabel}</span>
        </span>
    </a>

    <a href="#" class="profile-dropdown-item">
        <span class="profile-menu-icon"
              style="-webkit-mask-image:url('<c:url value="/assets/icons/chapter.svg"/>'); mask-image:url('<c:url value="/assets/icons/chapter.svg"/>');"></span>
        <span class="profile-dropdown-item-main">
            <span>Мои главы</span>
        </span>
    </a>
</div>
