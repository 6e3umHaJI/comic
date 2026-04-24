<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Редактирование перевода</title>
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
        <div class="chapter-upload-shell">
            <div class="chapter-upload-head">
                <div class="chapter-upload-head-text">
                    <div class="chapter-upload-kicker">Администрирование</div>
                    <h1 class="chapter-upload-title">Редактирование перевода</h1>
                    <div class="chapter-upload-subtitle">
                        Можно менять порядок страниц, вставлять новые, заменять существующие и удалять лишние.
                    </div>
                </div>

                <div class="chapter-upload-head-actions">
                    <a href="<c:url value='/translations/${translation.id}/preview'/>" class="btn btn-outline">
                        К просмотру
                    </a>
                    <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="btn btn-outline">
                        На страницу комикса
                    </a>
                </div>
            </div>

            <c:if test="${not empty uploadMessage}">
                <div class="status-banner status-banner-success">
                    <c:out value="${uploadMessage}"/>
                </div>
            </c:if>

            <c:if test="${not empty errorMessage}">
                <div class="status-banner status-banner-error">
                    <c:out value="${errorMessage}"/>
                </div>
            </c:if>

            <form id="adminTranslationEditForm"
                  action="<c:url value='/admin/translations/${translation.id}/save'/>"
                  method="post"
                  enctype="multipart/form-data"
                  class="chapter-upload-card">

                <input type="hidden" id="pagesPayload" name="pagesPayload">

                <div class="chapter-upload-grid">
                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label">Комикс</label>
                        <input type="text"
                               class="chapter-upload-input"
                               value="<c:out value='${comic.title}'/>"
                               readonly>
                    </div>

                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label">Глава</label>
                        <input type="text"
                               class="chapter-upload-input"
                               value="Глава ${translation.chapter.chapterNumber}"
                               readonly>
                    </div>

                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label">Язык</label>
                        <input type="text"
                               class="chapter-upload-input"
                               value="<c:out value='${translation.language.name}'/>"
                               readonly>
                    </div>

                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label">Тип перевода</label>
                        <input type="text"
                               class="chapter-upload-input"
                               value="<c:out value='${translation.translationType.name}'/>"
                               readonly>
                    </div>

                    <div class="chapter-upload-field chapter-upload-field-wide">
                        <label class="chapter-upload-label" for="title">Название перевода</label>
                        <input id="title"
                               name="title"
                               type="text"
                               class="chapter-upload-input"
                               maxlength="255"
                               value="<c:out value='${form.title}'/>"
                               placeholder="Введите название перевода"
                               required>
                    </div>
                </div>

                <div id="adminTranslationEditStatus" class="status-banner status-banner-error hidden"></div>

                <div class="admin-pages-toolbar">
                    <div class="chapter-upload-hint">
                        У перевода должна остаться хотя бы одна страница.
                    </div>

                    <button type="button" id="adminAppendPageBtn" class="btn btn-outline">
                        Добавить страницу в конец
                    </button>
                </div>

                <div id="adminPagesEditor" class="admin-pages-editor">
                    <c:forEach var="page" items="${pages}" varStatus="st">
                        <article class="admin-page-card" data-page-id="${page.id}">
                            <div class="admin-page-card-media">
                                <img class="admin-page-card-image"
                                     src="<c:url value='/assets/pages/${page.imagePath}'/>"
                                     alt="Страница ${page.pageNumber}">
                            </div>

                            <div class="admin-page-card-body">
                                <div class="admin-page-card-title">
                                    Страница <span class="js-admin-page-order">${st.index + 1}</span>
                                </div>

                                <div class="admin-page-card-file js-admin-page-file-label">
                                    Текущая страница сохранена
                                </div>

                                <div class="admin-page-card-actions">
                                    <button type="button" class="btn btn-outline js-page-insert-before">Вставить перед</button>
                                    <button type="button" class="btn btn-outline js-page-replace">Заменить</button>
                                    <button type="button" class="btn btn-outline js-page-delete">Удалить</button>
                                    <button type="button" class="btn btn-outline js-page-insert-after">Вставить после</button>
                                    <button type="button" class="btn btn-outline js-page-move-up">Выше</button>
                                    <button type="button" class="btn btn-outline js-page-move-down">Ниже</button>
                                </div>

                                <div class="hidden">
                                    <input type="file"
                                           name="pageFile_existing_${page.id}"
                                           data-role="page-file"
                                           accept="image/*">
                                </div>
                            </div>
                        </article>
                    </c:forEach>
                </div>

                <div class="chapter-upload-actions">
                    <button type="submit" class="btn">Сохранить</button>
                    <button type="button" id="openDeleteTranslationModalBtn" class="btn btn-outline">Удалить перевод</button>
                    <a href="<c:url value='/translations/${translation.id}/preview'/>" class="btn btn-outline">Отмена</a>
                </div>
            </form>
        </div>
    </main>

    <div id="adminDeleteTranslationModal" class="modal hidden admin-confirm-modal" aria-modal="true" role="dialog">
        <div class="modal-content admin-confirm-modal-content">
            <button type="button"
                    class="close-button js-close-admin-delete-modal"
                    aria-label="Закрыть">
                &times;
            </button>

            <h3 class="admin-confirm-title">Удалить перевод</h3>

            <p class="admin-confirm-text">
                Перевод будет удалён без возможности восстановления.
                Если это последний перевод главы, сама глава тоже будет удалена.
            </p>

            <div class="admin-confirm-actions">
                <button type="button" class="btn btn-outline js-close-admin-delete-modal">Отмена</button>

                <form id="adminDeleteTranslationForm"
                      action="<c:url value='/admin/translations/${translation.id}/delete'/>"
                      method="post">
                    <button type="submit" class="btn">Удалить</button>
                </form>
            </div>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/admin-translation-edit.js'/>"></script>
</body>
</html>
