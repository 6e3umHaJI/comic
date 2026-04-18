<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Жалобы</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/admin-complaints.css'/>">
</head>
<body>
<div class="wrapper" id="adminComplaintsPage" data-scope="${scope}">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container admin-complaints-page">
        <div class="admin-complaints-head">
            <h1>Жалобы</h1>
            <p>Отображаются только жалобы со статусами «Ожидание» и «На рассмотрении».</p>
        </div>

        <div class="admin-complaints-tabs">
            <button type="button"
                    class="admin-complaints-tab-btn ${scope == 'TRANSLATION' ? 'active' : ''}"
                    data-scope="TRANSLATION">
                Главы
            </button>

            <button type="button"
                    class="admin-complaints-tab-btn ${scope == 'COMIC' ? 'active' : ''}"
                    data-scope="COMIC">
                Комиксы
            </button>
        </div>

        <div id="adminComplaintsContent" class="admin-complaints-content">
            <jsp:include page="complaints-content.jsp"/>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/admin-complaints.js'/>"></script>
</body>
</html>
