<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Просмотр главы</title>
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
                <div class="chapter-upload-kicker">Просмотр главы</div>
                <h1 class="chapter-upload-title"><c:out value="${comic.title}"/></h1>
                <div class="chapter-upload-subtitle">Только просмотр. Редактирование недоступно.</div>
            </div>

            <div class="chapter-upload-head-actions">
                <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="btn btn-outline">На страницу комикса</a>
                <c:if test="${translation.reviewStatus.name == 'Одобрено'}">
                    <a href="<c:url value='/read/${translation.id}'/>" class="btn">Открыть в ридере</a>
                </c:if>
            </div>
        </div>

        <c:if test="${not empty successMessage}">
            <div class="status-banner status-banner-success"><c:out value="${successMessage}"/></div>
        </c:if>

        <c:if test="${not empty errorMessage}">
            <div class="status-banner status-banner-error"><c:out value="${errorMessage}"/></div>
        </c:if>

        <div class="chapter-preview-card">
            <div class="chapter-preview-grid">
                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Глава</span>
                    <span class="chapter-preview-value">#${translation.chapter.chapterNumber}</span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Язык</span>
                    <span class="chapter-preview-value"><c:out value="${translation.language.name}"/></span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Название перевода</span>
                    <span class="chapter-preview-value"><c:out value="${translation.title}"/></span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Тип перевода</span>
                    <span class="chapter-preview-value"><c:out value="${translation.translationType.name}"/></span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Статус</span>
                    <span class="chapter-preview-value">
                        <span class="chapter-status-badge
                            ${translation.reviewStatus.name == 'Одобрено' ? 'is-approved' : ''}
                            ${translation.reviewStatus.name == 'На проверке' ? 'is-pending' : ''}
                            ${translation.reviewStatus.name == 'Отклонено' ? 'is-rejected' : ''}">
                            <c:out value="${translation.reviewStatus.name}"/>
                        </span>
                    </span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Автор</span>
                    <span class="chapter-preview-value">
                        <c:out value="${translation.user != null ? translation.user.username : 'Не указан'}"/>
                    </span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Дата загрузки</span>
                    <span class="chapter-preview-value"><c:out value="${translation.createdAtFormatted}"/></span>
                </div>

                <div class="chapter-preview-item">
                    <span class="chapter-preview-label">Страниц</span>
                    <span class="chapter-preview-value">${pages.size()}</span>
                </div>
            </div>
        </div>

        <c:if test="${canModerate}">
            <div class="chapter-moderation-card">
                <div class="chapter-moderation-title">Рассмотрение перевода</div>

                <div class="chapter-moderation-actions">
                    <c:url var="readerPreviewUrl" value="/read/${translation.id}">
                        <c:param name="preview" value="true"/>
                    </c:url>

                    <a href="${readerPreviewUrl}" class="btn btn-outline">
                        Посмотреть в ридере
                    </a>

                    <form action="<c:url value='/admin/translations/${translation.id}/approve'/>" method="post">
                        <button type="submit" class="btn">Подтвердить</button>
                    </form>

                    <form action="<c:url value='/admin/translations/${translation.id}/reject'/>" method="post" class="chapter-reject-form">
                        <label class="chapter-upload-label" for="rejectReason">Причина отклонения</label>
                        <textarea id="rejectReason"
                                  name="reason"
                                  class="chapter-upload-textarea"
                                  maxlength="300"
                                  placeholder="Опишите причину отклонения"></textarea>

                        <label class="chapter-upload-check">
                            <input type="checkbox" name="revokeRights" value="true" class="check-ui">
                            <span>Лишить пользователя права на добавление глав</span>
                        </label>

                        <button type="submit" class="btn btn-outline">Отклонить</button>
                    </form>
                </div>
            </div>
        </c:if>

        <div class="chapter-pages-card">
            <div class="chapter-pages-title">Страницы</div>

            <div class="chapter-pages-grid">
                <c:forEach var="page" items="${pages}">
                    <figure class="chapter-page-item">
                        <img src="<c:url value='/assets/pages/${page.imagePath}'/>"
                             alt="Страница ${page.pageNumber}"
                             loading="lazy">
                        <figcaption>Страница ${page.pageNumber}</figcaption>
                    </figure>
                </c:forEach>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>
</body>
</html>
