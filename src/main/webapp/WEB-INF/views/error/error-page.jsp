<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Ошибка ${statusCode}</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/error-page.css'/>">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container">
        <section class="error-shell">
            <div class="error-card">
                <div class="error-code">${statusCode}</div>
                <h1 class="error-title">${title}</h1>
                <p class="error-message">${message}</p>

                <c:if test="${not empty path}">
                    <p class="error-path">Адрес: ${path}</p>
                </c:if>

                <div class="error-actions">
                    <a class="btn" href="<c:url value='/home'/>">На главную</a>
                    <button type="button" class="btn btn-outline" onclick="goBack()">Назад</button>
                </div>
            </div>
        </section>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script>
    function goBack() {
        if (window.history.length > 1) {
            window.history.back();
        } else {
            window.location.href = '<c:url value="/home"/>';
        }
    }
</script>
</body>
</html>