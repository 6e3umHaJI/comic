<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Проверка глав</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="<c:url value='/style/common.css'/>" rel="stylesheet">
    <link href="<c:url value='/style/chapter-upload.css'/>" rel="stylesheet">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container chapter-upload-page">
        <div class="chapter-upload-head">
            <div>
                <div class="chapter-upload-kicker">Администрирование</div>
                <h1 class="chapter-upload-title">Проверка загруженных глав</h1>
                <div class="chapter-upload-subtitle">На этой странице отображаются переводы со статусом «На проверке».</div>
            </div>
        </div>

        <c:choose>
            <c:when test="${pendingPage.totalElements == 0}">
                <div class="status-banner status-banner-muted">На проверке сейчас нет ни одного перевода.</div>
            </c:when>
            <c:otherwise>
                <div class="chapter-review-list">
                    <c:forEach var="translation" items="${pendingPage.content}">
                        <a href="<c:url value='/translations/${translation.id}/preview'/>" class="chapter-review-card">
                            <div class="chapter-review-main">
                                <div class="chapter-review-title"><c:out value="${translation.chapter.comic.title}"/></div>
                                <div class="chapter-review-meta">
                                    <span>Глава: ${translation.chapter.chapterNumber}</span>
                                    <span>Язык: <c:out value="${translation.language.name}"/></span>
                                    <span>Автор: <c:out value="${translation.user != null ? translation.user.username : 'Не указан'}"/></span>
                                </div>
                                <div class="chapter-review-meta">
                                    <span>Название перевода: <c:out value="${translation.title}"/></span>
                                    <span>Страниц: ${pageCounts[translation.id]}</span>
                                    <span>Добавлено: <c:out value="${translation.createdAtFormatted}"/></span>
                                </div>
                            </div>

                            <div class="chapter-review-actions">
                                <span class="chapter-status-badge is-pending">На проверке</span>
                                <span class="btn btn-outline">Открыть</span>
                            </div>
                        </a>
                    </c:forEach>
                </div>

                <c:if test="${pendingPage.totalPages > 1}">
                    <div class="pagination chapter-review-pagination">
                        <c:if test="${!pendingPage.first}">
                            <a href="<c:url value='/admin/translations/review?page=${pendingPage.number - 1}'/>">←</a>
                        </c:if>

                        <c:forEach begin="0" end="${pendingPage.totalPages - 1}" var="pageIndex">
                            <a href="<c:url value='/admin/translations/review?page=${pageIndex}'/>"
                               class="${pageIndex == pendingPage.number ? 'active' : ''}">
                                    ${pageIndex + 1}
                            </a>
                        </c:forEach>

                        <c:if test="${!pendingPage.last}">
                            <a href="<c:url value='/admin/translations/review?page=${pendingPage.number + 1}'/>">→</a>
                        </c:if>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>
</body>
</html>
