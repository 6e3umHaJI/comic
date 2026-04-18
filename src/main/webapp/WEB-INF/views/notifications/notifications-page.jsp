<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Оповещения</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/notifications.css'/>">
</head>
<body>
<div class="wrapper" id="notificationsPage" data-tab="${tab}" data-context-path="${pageContext.request.contextPath}">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container notifications-page">
        <div class="notifications-head">
            <h1>Оповещения</h1>
        </div>

        <div class="tab-buttons notifications-tab-buttons">
            <button type="button"
                    class="notifications-tab-btn ${tab == 'feed' ? 'active' : ''}"
                    data-tab="feed">
                Оповещения
            </button>
            <button type="button"
                    class="notifications-tab-btn ${tab == 'subscriptions' ? 'active' : ''}"
                    data-tab="subscriptions">
                Подписки на тайтлы
            </button>
        </div>

        <div id="notificationsContent" class="notifications-content">
            <jsp:include page="notifications-content.jsp"/>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/auth-required-modal.js'/>"></script>
<script src="<c:url value='/script/notifications.js'/>"></script>
</body>
</html>
