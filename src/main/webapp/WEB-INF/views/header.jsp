<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<header class="main-header">
    <div class="container">
        <div class="logo">
            <a href="<c:url value='/home'/>">
                <span>Comix<span class="accent">Universe</span></span>
            </a>
        </div>

        <nav class="nav-links">
            <a href="<c:url value='/home'/>">
                <span class="nav-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/home.svg"/>'); mask-image:url('<c:url value="/assets/icons/home.svg"/>');"></span>
                Главная
            </a>
            <a href="<c:url value='/catalog'/>">
                <span class="nav-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/catalog.svg"/>'); mask-image:url('<c:url value="/assets/icons/catalog.svg"/>');"></span>
                Каталог
            </a>
            <a href="<c:url value='/search'/>">
                <span class="nav-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/search.svg"/>'); mask-image:url('<c:url value="/assets/icons/search.svg"/>');"></span>
                Поиск
            </a>

            <sec:authorize access="hasRole('ADMIN')">
                <a href="<c:url value='/complaints'/>">
                    <span class="nav-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/warning.svg"/>'); mask-image:url('<c:url value="/assets/icons/warning.svg"/>');"></span>
                    Жалобы
                </a>
                <a href="<c:url value='/admin/uploads'/>">
                    <span class="nav-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/admin.svg"/>'); mask-image:url('<c:url value="/assets/icons/admin.svg"/>');"></span>
                    Загруженные главы
                </a>
            </sec:authorize>
        </nav>

        <div class="nav-actions">
            <button class="theme-toggle" id="theme-toggle" title="Переключить тему" type="button">
                <span class="theme-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/theme.svg"/>'); mask-image:url('<c:url value="/assets/icons/theme.svg"/>');"></span>
            </button>

            <sec:authorize access="!isAuthenticated()">
                <a href="<c:url value='/auth/login'/>" class="btn">Войти</a>
            </sec:authorize>

           <sec:authorize access="isAuthenticated()">
               <div class="profile-menu-wrap">
                   <button class="btn" id="profileMenuToggle" type="button" aria-expanded="false">
                       <span class="btn-icon"
                             style="-webkit-mask-image:url('<c:url value="/assets/icons/user.svg"/>'); mask-image:url('<c:url value="/assets/icons/user.svg"/>');"></span>
                       Профиль
                   </button>

                   <div id="profileDropdown" class="profile-dropdown" hidden>
                       <a href="<c:url value='/collections'/>" class="profile-dropdown-item">
                           <span class="profile-menu-icon"
                                 style="-webkit-mask-image:url('<c:url value="/assets/icons/collection.svg"/>'); mask-image:url('<c:url value="/assets/icons/collection.svg"/>');"></span>
                           <span>Коллекция</span>
                       </a>

                       <a href="<c:url value='/notifications'/>" class="profile-dropdown-item">
                           <span class="profile-menu-icon"
                                 style="-webkit-mask-image:url('<c:url value="/assets/icons/notification.svg"/>'); mask-image:url('<c:url value="/assets/icons/notification.svg"/>');"></span>
                           <span>Оповещения</span>
                       </a>

                       <a href="#" class="profile-dropdown-item">
                           <span class="profile-menu-icon"
                                 style="-webkit-mask-image:url('<c:url value="/assets/icons/upload.svg"/>'); mask-image:url('<c:url value="/assets/icons/upload.svg"/>');"></span>
                           <span>Мои главы</span>
                       </a>
                   </div>
               </div>

               <a href="<c:url value='/auth/logout'/>" class="btn btn-outline">
                   <span class="btn-icon"
                         style="-webkit-mask-image:url('<c:url value="/assets/icons/logout.svg"/>'); mask-image:url('<c:url value="/assets/icons/logout.svg"/>');"></span>
                   Выйти
               </a>
           </sec:authorize>


        </div>
    </div>
</header>

<script src="<c:url value='/script/theme-toggle.js'/>"></script>
<script src="<c:url value='/script/profile-dropdown.js'/>"></script>
<jsp:include page="/WEB-INF/views/auth/auth-required-modal.jsp"/>
<script src="<c:url value='/script/auth-required-modal.js'/>"></script>