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
        <div class="chapter-upload-shell">
            <div class="chapter-upload-head">
                <div class="chapter-upload-head-text">
                    <div class="chapter-upload-kicker">Просмотр главы</div>
                    <h1 class="chapter-upload-title"><c:out value="${comic.title}"/></h1>
                    <div class="chapter-upload-subtitle">Только просмотр. Редактирование недоступно.</div>
                </div>

                <div class="chapter-upload-head-actions" id="chapterPreviewHeadActions">
                    <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="btn btn-outline">
                        На страницу комикса
                    </a>
                    <c:if test="${translation.reviewStatus.name == 'Одобрено'}">
                        <a href="<c:url value='/read/${translation.id}'/>" class="btn" id="approvedReaderLink">
                            Открыть в ридере
                        </a>
                    </c:if>
                </div>
            </div>

            <c:if test="${not empty successMessage}">
                <div class="chapter-preview-result chapter-preview-result-success">
                    <div class="chapter-preview-result-title">Изменения сохранены</div>
                    <div class="chapter-preview-result-text">
                        <c:out value="${successMessage}"/>
                    </div>
                </div>
            </c:if>

            <c:if test="${not empty errorMessage}">
                <div class="chapter-preview-result chapter-preview-result-error">
                    <div class="chapter-preview-result-title">Не удалось выполнить действие</div>
                    <div class="chapter-preview-result-text">
                        <c:out value="${errorMessage}"/>
                    </div>
                </div>
            </c:if>

            <div id="previewModerationStatus" class="chapter-preview-result hidden">
                <div class="chapter-preview-result-title"></div>
                <div class="chapter-preview-result-text"></div>
            </div>

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
                            <span id="translationReviewStatusBadge"
                                  class="chapter-status-badge
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
                <div class="chapter-moderation-card" id="chapterModerationCard" data-review-url="<c:url value='/admin/translations/review'/>">
                    <div class="chapter-moderation-title">Рассмотрение перевода</div>

                    <c:url var="readerPreviewUrl" value="/read/${translation.id}">
                        <c:param name="preview" value="true"/>
                    </c:url>

                    <form id="chapterApproveForm"
                          action="<c:url value='/admin/translations/${translation.id}/approve'/>"
                          method="post"
                          class="chapter-inline-form"></form>

                    <form id="chapterRejectForm"
                          action="<c:url value='/admin/translations/${translation.id}/reject'/>"
                          method="post"
                          class="chapter-inline-form"></form>

                    <div class="chapter-moderation-top">
                        <label class="chapter-upload-label" for="rejectReason">Причина отклонения</label>
                        <textarea id="rejectReason"
                                  name="reason"
                                  form="chapterRejectForm"
                                  class="chapter-upload-textarea"
                                  maxlength="300"
                                  placeholder="Опишите причину отклонения"></textarea>

                        <c:set var="userAlreadyRevoked" value="${translation.user != null and translation.user.canPropose == false}"/>

                        <label class="chapter-upload-check">
                            <input type="checkbox"
                                   name="revokeRights"
                                   value="true"
                                   form="chapterRejectForm"
                                   class="check-ui"
                                   ${userAlreadyRevoked ? 'checked disabled' : ''}>
                            <span>Лишить пользователя права на добавление глав</span>
                        </label>

                        <c:if test="${userAlreadyRevoked}">
                            <div class="chapter-upload-hint">
                                Пользователь уже не может добавлять новые переводы.
                            </div>
                        </c:if>
                    </div>

                    <div class="chapter-moderation-buttons">
                        <a href="${readerPreviewUrl}" class="btn btn-outline">
                            Посмотреть в ридере
                        </a>
                        <button type="submit" form="chapterApproveForm" id="approveTranslationBtn" class="btn">
                            Подтвердить
                        </button>
                        <button type="submit" form="chapterRejectForm" id="rejectTranslationBtn" class="btn btn-outline">
                            Отклонить
                        </button>
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
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<c:if test="${canModerate}">
<script>
(() => {
    const approveForm = document.getElementById("chapterApproveForm");
    const rejectForm = document.getElementById("chapterRejectForm");
    const approveBtn = document.getElementById("approveTranslationBtn");
    const rejectBtn = document.getElementById("rejectTranslationBtn");
    const moderationCard = document.getElementById("chapterModerationCard");
    const resultBox = document.getElementById("previewModerationStatus");
    const statusBadge = document.getElementById("translationReviewStatusBadge");
    const headActions = document.getElementById("chapterPreviewHeadActions");
    const reviewUrl = moderationCard?.dataset.reviewUrl || "<c:url value='/admin/translations/review'/>";

    if (!approveForm || !rejectForm || !approveBtn || !rejectBtn || !moderationCard || !resultBox || !statusBadge) {
        return;
    }

    function setButtonsDisabled(disabled) {
        approveBtn.disabled = disabled;
        rejectBtn.disabled = disabled;
    }

    function showModerationResult(message, isError) {
        const title = resultBox.querySelector(".chapter-preview-result-title");
        const text = resultBox.querySelector(".chapter-preview-result-text");

        resultBox.classList.remove("hidden", "chapter-preview-result-success", "chapter-preview-result-error");
        resultBox.classList.add(isError ? "chapter-preview-result-error" : "chapter-preview-result-success");

        if (title) {
            title.textContent = isError ? "Не удалось выполнить действие" : "Изменения сохранены";
        }

        if (text) {
            text.textContent = (message || "").trim();
        }
    }

    function updateStatusBadge(statusName) {
        statusBadge.textContent = statusName;
        statusBadge.classList.remove("is-approved", "is-pending", "is-rejected");

        if (statusName === "Одобрено") {
            statusBadge.classList.add("is-approved");
        } else if (statusName === "На проверке") {
            statusBadge.classList.add("is-pending");
        } else if (statusName === "Отклонено") {
            statusBadge.classList.add("is-rejected");
        }
    }

    function ensureReaderLink(readerUrl) {
        if (!readerUrl || !headActions) {
            return;
        }

        if (document.getElementById("approvedReaderLink")) {
            return;
        }

        const link = document.createElement("a");
        link.id = "approvedReaderLink";
        link.href = readerUrl;
        link.className = "btn";
        link.textContent = "Открыть в ридере";
        headActions.appendChild(link);
    }

    async function submitModerationForm(form, afterSuccess) {
        setButtonsDisabled(true);

        try {
            const response = await fetch(form.action, {
                method: "POST",
                headers: {
                    "X-Requested-With": "XMLHttpRequest",
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
                },
                body: new URLSearchParams(new FormData(form)).toString()
            });

            const payload = await response.json();

            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || "Не удалось выполнить действие.");
            }

            afterSuccess(payload);
        } catch (error) {
            showModerationResult(error.message || "Не удалось выполнить действие.", true);
        } finally {
            setButtonsDisabled(false);
        }
    }

    approveForm.addEventListener("submit", (event) => {
        event.preventDefault();

        submitModerationForm(approveForm, (payload) => {
            updateStatusBadge(payload.statusName || "Одобрено");
            ensureReaderLink(payload.readerUrl);
            moderationCard.remove();
            showModerationResult(payload.message || "Перевод подтверждён.", false);
        });
    });

    rejectForm.addEventListener("submit", (event) => {
        event.preventDefault();

        submitModerationForm(rejectForm, (payload) => {
            window.location.replace(payload.redirectUrl || reviewUrl);
        });
    });
})();
</script>
</c:if>
</body>
</html>