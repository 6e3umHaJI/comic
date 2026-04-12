<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div id="profileDropdown" class="profile-dropdown" hidden>
    <a href="<c:url value='/collections'/>" class="profile-dropdown-item">
        <span class="profile-menu-icon"
              style="-webkit-mask-image:url('<c:url value="/assets/icons/collection.svg"/>'); mask-image:url('<c:url value="/assets/icons/notification.svg"/>');"></span>
        <span>Коллекция</span>
    </a>

    <a href="<c:url value='/notifications'/>" class="profile-dropdown-item">
        <span class="profile-menu-icon"
              style="-webkit-mask-image:url('<c:url value="/assets/icons/notification.svg"/>'); mask-image:url('<c:url value="/assets/icons/notification.svg"/>');"></span>
        <span>Оповещения</span>
    </a>

    <a href="#" class="profile-dropdown-item">
        <span class="profile-menu-icon"
              style="-webkit-mask-image:url('<c:url value="/assets/icons/chapter.svg"/>'); mask-image:url('<c:url value="/assets/icons/chapter.svg"/>');"></span>
        <span>Мои главы</span>
    </a>
</div>
