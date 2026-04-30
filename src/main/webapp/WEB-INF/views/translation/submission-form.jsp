<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Добавление перевода</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="<c:url value='/style/common.css'/>" rel="stylesheet">
    <link href="<c:url value='/style/chapter-upload.css'/>" rel="stylesheet">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container chapter-upload-page"
          id="chapterSubmissionPage"
          data-options-url="<c:url value='/comics/${comic.id}/chapters/options'/>">
        <div class="chapter-upload-shell">
            <div class="chapter-upload-head">
                <div class="chapter-upload-head-text">
                    <div class="chapter-upload-kicker">Добавление перевода</div>
                    <h1 class="chapter-upload-title"><c:out value="${comic.title}"/></h1>
                    <div class="chapter-upload-subtitle">
                        Заполните информацию о переводе и загрузите страницы главы.
                    </div>
                </div>

                <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="btn btn-outline">
                    Назад
                </a>
            </div>

            <c:if test="${!isAdmin}">
                <div class="status-banner status-banner-muted">
                    Переводов на проверке: <strong>${pendingCount}</strong> / <strong>${pendingLimit}</strong>
                </div>
            </c:if>

            <form id="chapterSubmissionForm"
                  class="chapter-upload-card"
                  action="<c:url value='/comics/${comic.id}/chapters/new'/>"
                  method="post"
                  enctype="multipart/form-data">
                <input type="hidden" name="comicId" value="${comic.id}">
                <div id="selectedPageNumbersHolder"></div>

                <div class="chapter-upload-grid">
                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label" for="languageSearch">Поиск языка</label>
                        <input id="languageSearch"
                               type="text"
                               class="chapter-upload-input"
                               maxlength="35"
                               placeholder="Введите название языка">
                    </div>

                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label" for="languageId">Язык перевода</label>
                        <select id="languageId" name="languageId" class="chapter-upload-select" required>
                            <c:forEach var="language" items="${languages}">
                                <option value="${language.id}"
                                        data-auto-translate-supported="${not empty language.code}"
                                        ${form.languageId == language.id ? 'selected' : ''}>
                                    <c:out value="${language.name}"/>
                                </option>
                            </c:forEach>
                        </select>
                        <c:if test="${isAdmin}">
                            <div class="chapter-upload-hint">
                                При автоматическом переводе в списке остаются только языки с заполненным кодом перевода.
                            </div>
                        </c:if>
                    </div>

                    <div class="chapter-upload-field">
                        <label class="chapter-upload-label" for="chapterNumber">Номер главы</label>
                        <select id="chapterNumber" name="chapterNumber" class="chapter-upload-select" required>
                            <c:forEach var="number" items="${chapterOptions}">
                                <option value="${number}" ${form.chapterNumber == number ? 'selected' : ''}>
                                    Глава ${number}
                                </option>
                            </c:forEach>
                        </select>

                        <div class="chapter-upload-hint">
                            Доступны главы от 1 до следующей после последней главы с одобренным переводом на выбранном языке.
                        </div>
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

                    <c:if test="${isAdmin}">
                        <div class="chapter-upload-field chapter-upload-field-wide">
                            <div class="chapter-auto-settings">
                                <label class="chapter-upload-toggle" for="autoTranslate">
                                    <input
                                            type="checkbox"
                                            id="autoTranslate"
                                            name="autoTranslate"
                                            value="true"
                                            <c:if test="${form.autoTranslate}">checked</c:if>
                                    >
                                    <span>Запросить автоматический перевод</span>
                                </label>
                                <input type="hidden" name="_autoTranslate" value="false">

                                <div id="sourceLanguageField"
                                     class="chapter-auto-panel <c:if test='${not form.autoTranslate}'>hidden</c:if>">
                                    <div class="chapter-upload-field">
                                        <label class="chapter-upload-label" for="sourceLanguageId">Язык исходного текста</label>
                                        <select id="sourceLanguageId" name="sourceLanguageId" class="chapter-upload-select">
                                            <option value="">Выберите язык</option>
                                            <c:forEach var="language" items="${sourceLanguages}">
                                                <option value="${language.id}"
                                                        ${form.sourceLanguageId == language.id ? 'selected' : ''}>
                                                    <c:out value="${language.name}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </div>

                                    <div class="chapter-upload-hint">
                                        Отметьте в списке ниже только те страницы, которые нужно переводить автоматически.
                                        Не допускается одинаковый язык исходного текста и язык перевода.
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:if>

                    <div class="chapter-upload-field chapter-upload-field-wide">
                        <label class="chapter-upload-label" for="pageFiles">Страницы перевода</label>
                        <input id="pageFiles"
                               name="pageFiles"
                               type="file"
                               class="chapter-upload-file"
                               accept=".jpg,.webp,image/jpeg"
                               multiple
                               required>

                        <div class="chapter-upload-hint">
                            До 200 изображений, до 1 МБ каждое. Разрешены только JPG.
                            Имена файлов: 001.jpg, 002.jpg, 003.jpg и так далее.
                        </div>
                    </div>
                </div>

                <div class="chapter-upload-files">
                    <div class="chapter-upload-files-title">Выбранные файлы</div>
                    <div id="chapterSubmissionFilesList" class="chapter-upload-files-list chapter-upload-files-empty">
                        Файлы ещё не выбраны.
                    </div>
                </div>

                <c:if test="${isAdmin}">
                    <div id="autoProcessingLoading" class="chapter-auto-loading hidden">
                        <div class="chapter-auto-spinner"></div>
                        <div class="chapter-auto-loading-text">
                            Автоматический перевод выполняется. Страницы обрабатываются, пожалуйста, подождите…
                        </div>
                    </div>
                </c:if>

                <div class="chapter-upload-actions">
                    <button type="submit" class="btn">Сохранить</button>
                    <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="btn btn-outline">Отмена</a>
                </div>

                <div id="chapterSubmissionClientStatus"
                     class="status-banner status-banner-error chapter-upload-form-status ${not empty errorMessage ? '' : 'hidden'}">
                    <c:out value="${errorMessage}"/>
                </div>
            </form>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/chapter-upload.js'/>"></script>
</body>
</html>
