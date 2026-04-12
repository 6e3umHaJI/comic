<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Коллекция</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/catalog.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/collections.css'/>">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container">
        <div id="collectionsRoot">
            <jsp:include page="collections-content.jsp"/>
        </div>
    </main>

    <jsp:include page="collection-global-modal.jsp"/>
    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/collections.js'/>"></script>
<script src="<c:url value='/script/collection-modal.js'/>"></script>
</body>
</html>
