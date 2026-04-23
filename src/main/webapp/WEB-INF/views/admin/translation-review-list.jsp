<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Проверка глав</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="<c:url value='/style/common.css'/>" rel="stylesheet">
    <link href="<c:url value='/style/translation-cabinet.css'/>" rel="stylesheet">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container translation-cabinet-page" id="translationCabinetPage">
        <div class="translation-cabinet-shell">
            <div class="translation-cabinet-head">
                <div>
                    <h1 class="translation-cabinet-title">Проверка загруженных глав</h1>
                    <div class="translation-cabinet-subtitle">
                        На этой странице отображаются переводы со статусом «На проверке».
                    </div>
                </div>
            </div>

            <div id="translationCabinetContent">
                <jsp:include page="translation-review-content.jsp"/>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/translation-cabinet.js'/>"></script>
</body>
</html>
