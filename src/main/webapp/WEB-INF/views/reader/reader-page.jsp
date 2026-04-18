<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<head>
    <title>${comic.title}, глава ${translation.chapter.chapterNumber}</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/complaint-modal.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/reader.css'/>">
</head>
<body class="reader-body" data-authenticated="${pageContext.request.userPrincipal != null}">
<div id="readerApp"
     class="reader-app"
     data-context-path="${pageContext.request.contextPath}"
     data-translation-id="${translation.id}"
     data-comic-id="${comic.id}"
     data-comic-title="${comic.title}"
     data-chapter-number="${translation.chapter.chapterNumber}"
     data-language-name="${translation.language.name}"
     data-total-pages="${totalPages}"
     data-initial-page="${initialPage}"
     data-is-logged="${isLogged}"
     data-prev-translation-id="${prevTranslation != null ? prevTranslation.id : ''}"
     data-next-translation-id="${nextTranslation != null ? nextTranslation.id : ''}"
     data-prev-chapter-id="${prevTranslation != null ? prevTranslation.chapter.id : ''}"
     data-next-chapter-id="${nextTranslation != null ? nextTranslation.chapter.id : ''}"
     data-prev-language-name="${prevTranslation != null ? prevTranslation.language.name : ''}"
     data-next-language-name="${nextTranslation != null ? nextTranslation.language.name : ''}"
     data-comic-url="<c:url value='/comics/${comic.id}'/>"
     data-back-url="<c:url value='/comics/${comic.id}?tab=chapters'/>">

    <header id="readerTopbar" class="reader-topbar">
        <div class="reader-topbar-left">
            <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="reader-icon-btn" title="На страницу комикса">
               <span class="reader-file-icon"
                 style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span>
            </a>

            <c:choose>
                <c:when test="${prevTranslation != null}">
                    <a href="<c:url value='/read/${prevTranslation.id}'/>" id="readerPrevChapterBtn" class="reader-icon-btn" title="Предыдущая глава">
                        <span class="reader-file-icon"
                                                 style="-webkit-mask-image:url('<c:url value="/assets/icons/prev.svg"/>'); mask-image:url('<c:url value="/assets/icons/prev.svg"/>');"></span>
                    </a>
                </c:when>
                <c:otherwise>
                    <button type="button" class="reader-icon-btn is-disabled" disabled title="Предыдущей главы нет">
                        <span class="reader-file-icon"
                                                 style="-webkit-mask-image:url('<c:url value="/assets/icons/prev-disabled.svg"/>'); mask-image:url('<c:url value="/assets/icons/prev-disabled.svg"/>');"></span>
                    </button>
                </c:otherwise>
            </c:choose>

            <c:choose>
                <c:when test="${nextTranslation != null}">
                    <a href="<c:url value='/read/${nextTranslation.id}'/>" id="readerNextChapterBtn" class="reader-icon-btn" title="Следующая глава">
                        <span class="reader-file-icon"
                                                 style="-webkit-mask-image:url('<c:url value="/assets/icons/next.svg"/>'); mask-image:url('<c:url value="/assets/icons/next.svg"/>');"></span>
                    </a>
                </c:when>
                <c:otherwise>
                    <button type="button" class="reader-icon-btn is-disabled" disabled title="Следующей главы нет">
                        <span class="reader-file-icon"
                         style="-webkit-mask-image:url('<c:url value="/assets/icons/next-disabled.svg"/>'); mask-image:url('<c:url value="/assets/icons/next-disabled.svg"/>');"></span>
                    </button>
                </c:otherwise>
            </c:choose>

            <div class="reader-title-wrap">
                <a class="reader-comic-link" href="<c:url value='/comics/${comic.id}'/>">${comic.title}</a>
                <div class="reader-subtitle">
                    Глава ${translation.chapter.chapterNumber}
                    <span class="reader-dot">•</span>
                    ${translation.language.name}
                </div>
            </div>
        </div>

        <div class="reader-topbar-right">
            <c:choose>
                <c:when test="${isNotificationsEnabled}">
                    <c:url var="readerNotificationIconUrl" value="/assets/icons/notification-on.svg"/>
                </c:when>
                <c:otherwise>
                    <c:url var="readerNotificationIconUrl" value="/assets/icons/notification-off.svg"/>
                </c:otherwise>
            </c:choose>
            <button type="button"
                    id="readerCollectionBtn"
                    class="reader-icon-btn reader-collection-btn js-collection-toggle ${inCollections ? 'is-bookmarked' : ''}"
                    data-comic-id="${comic.id}"
                    data-authenticated="${isLogged}"
                    data-icon-only="true"
                    title="Коллекция"
                    aria-label="Коллекция">
                <span class="reader-file-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/collection.svg"/>'); mask-image:url('<c:url value="/assets/icons/collection.svg"/>');"></span>
            </button>

            <button type="button"
                    id="readerNotificationBtn"
                    class="reader-icon-btn reader-notification-btn js-notification-toggle ${isNotificationsEnabled ? 'is-active' : ''}"
                    data-comic-id="${comic.id}"
                    data-authenticated="${isLogged}"
                    data-icon-only="true"
                    data-toggle-url="<c:url value='/notifications/toggle'/>"
                    data-icon-on-url="<c:url value='/assets/icons/notification-on.svg'/>"
                    data-icon-off-url="<c:url value='/assets/icons/notification-off.svg'/>"
                    title="${isNotificationsEnabled ? 'Отключить оповещения' : 'Включить оповещения'}"
                    aria-label="${isNotificationsEnabled ? 'Отключить оповещения' : 'Включить оповещения'}">
                <span class="reader-file-icon js-notification-toggle-icon"
                      style="-webkit-mask-image:url('${readerNotificationIconUrl}'); mask-image:url('${readerNotificationIconUrl}');"></span>
            </button>

            <button type="button"
                    id="readerComplaintBtn"
                    class="reader-icon-btn reader-complaint-btn js-open-complaint-modal"
                    data-authenticated="${isLogged}"
                    data-target-id="${translation.id}"
                    title="Пожаловаться"
                    aria-label="Пожаловаться">
                <span class="reader-file-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/complaint.svg"/>'); mask-image:url('<c:url value="/assets/icons/complaint.svg"/>');"></span>
            </button>



            <button type="button" id="readerThemeBtn" class="reader-icon-btn" title="Тема">
                <span class="reader-file-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/theme.svg"/>'); mask-image:url('<c:url value="/assets/icons/theme.svg"/>');"></span>
            </button>

            <button type="button" id="readerSettingsBtn" class="reader-icon-btn" title="Настройки">
                <span class="reader-file-icon"
                      style="-webkit-mask-image:url('<c:url value="/assets/icons/settings.svg"/>'); mask-image:url('<c:url value="/assets/icons/settings.svg"/>');"></span>
            </button>
        </div>
    </header>

    <div id="readerLoadingOverlay" class="reader-loading-overlay" aria-hidden="true">
        <div class="reader-loading-box">
            <div class="reader-loading-spinner"></div>
            <div id="readerLoadingText" class="reader-loading-text">Загрузка страниц…</div>
        </div>
    </div>


    <div id="readerSurface" class="reader-surface">
        <div id="readerHorizontal" class="reader-horizontal">
            <div id="readerHorizontalStage" class="reader-horizontal-stage">
                <img id="readerCurrentImage" class="reader-current-image" src="" alt="">
            </div>
        </div>

        <div id="readerVertical" class="reader-vertical hidden">
            <c:forEach var="p" items="${pages}">
                <img class="reader-vertical-image"
                     data-page-number="${p.pageNumber}"
                     data-image-path="${p.imagePath}"
                     src="${pageContext.request.contextPath}/assets/pages/${p.imagePath}"
                     loading="lazy"
                     decoding="async"
                     alt="Страница ${p.pageNumber}">
            </c:forEach>
        </div>

        <div id="readerZonesFrame" class="reader-zones-frame">
            <button type="button" id="readerZonePrev" class="reader-zone reader-zone-prev" aria-label="Предыдущая страница"></button>
            <button type="button" id="readerZoneNext" class="reader-zone reader-zone-next" aria-label="Следующая страница"></button>
        </div>
    </div>

    <div id="readerPageCounter" class="reader-page-counter">
        <span id="readerPageCurrent">${initialPage}</span>/<span id="readerPageTotal">${totalPages}</span>
    </div>

    <aside id="readerSettingsPanel" class="reader-settings hidden" aria-hidden="true">
        <div class="reader-settings-card">
            <div class="reader-settings-head">
                <h3>Настройки ридера</h3>
                <button type="button" id="readerSettingsClose" class="reader-icon-btn" title="Закрыть">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18.3 5.71 12 12l6.3 6.29-1.41 1.41L10.59 13.41 4.29 19.7 2.88 18.29 9.17 12 2.88 5.71 4.29 4.3l6.3 6.29 6.29-6.3z"/></svg>
                </button>
            </div>

            <div id="readerSettingsNotice" class="reader-settings-notice" hidden></div>

            <div class="reader-setting-group">
                <label class="reader-setting-label">Режим чтения</label>
                <select id="settingReadingMode" class="reader-select">
                    <option value="horizontal">Горизонтальный</option>
                    <option value="vertical">Вертикальный</option>
                </select>
            </div>

            <div class="reader-setting-group">
                <label class="reader-setting-label">Отображение изображения</label>
                <select id="settingFitMode" class="reader-select">
                    <option value="height">По высоте экрана</option>
                    <option value="width">По ширине</option>
                </select>
            </div>

            <div class="reader-setting-group" id="imageWidthGroup">
                <label class="reader-setting-label">
                    Ширина изображения
                    <span id="settingImageWidthValue">100%</span>
                </label>
                <input id="settingImageWidth" class="reader-range" type="range" min="30" max="100" step="10" value="100">
            </div>

            <div class="reader-setting-group" id="verticalGapGroup">
                <label class="reader-setting-label">
                    Расстояние между страницами
                    <span id="settingVerticalGapValue">20px</span>
                </label>
                <input id="settingVerticalGap" class="reader-range" type="range" min="0" max="200" step="10" value="20">
            </div>

            <div class="reader-setting-group">
                <label class="reader-setting-label">Области нажатия</label>
                <select id="settingClickZones" class="reader-select">
                    <option value="left-right">Лево / право</option>
                    <option value="top-bottom">Верх / низ</option>
                </select>
            </div>

            <div class="reader-setting-group reader-setting-inline">
                <label class="reader-check">
                    <input id="settingInvertClicks" type="checkbox" class="check-ui">
                    <span>Инвертировать клики</span>
                </label>
            </div>

            <div class="reader-setting-group reader-setting-inline">
                <label class="reader-check">
                    <input id="settingTopbarVisible" type="checkbox" class="check-ui" checked>
                    <span>Показывать верхнюю панель</span>
                </label>
            </div>

            <div class="reader-setting-group reader-setting-inline">
                <label class="reader-check">
                    <input id="settingCounterVisible" type="checkbox"  class="check-ui" checked>
                    <span>Показывать счётчик страниц</span>
                </label>
            </div>

            <div class="reader-setting-group">
                <label class="reader-setting-label">Клавиша назад</label>
                <input id="settingPrevKey" class="reader-key-input" type="text" readonly value="ArrowLeft">
            </div>

            <div class="reader-setting-group">
                <label class="reader-setting-label">Клавиша вперёд</label>
                <input id="settingNextKey" class="reader-key-input" type="text" readonly value="ArrowRight">
            </div>

            <div class="reader-setting-group">
                <label class="reader-setting-label">Клавиша настроек</label>
                <input id="settingSettingsKey" class="reader-key-input" type="text" readonly value="KeyS">
            </div>
        </div>
    </aside>

    <div id="translationsModal" class="modal hidden" aria-modal="true" role="dialog">
        <div class="modal-content tr-modal">
            <button type="button" class="close-button tr-close" id="trClose" aria-label="Закрыть">&times;</button>
            <h3 id="trTitle" class="tr-title">Переводы главы</h3>

            <p id="trNotice" class="reader-translation-notice hidden"></p>

            <div class="tr-controls">
                <label for="trLangSelect">Язык:</label>
                <select id="trLangSelect" class="tr-select"></select>
            </div>

            <ul id="trList" class="translation-list"></ul>

            <div class="tr-footer">
                <button id="trMore" type="button" class="btn">Показать ещё</button>
            </div>
        </div>
    </div>


    <script>
        window.readerBootstrap = {
            pages: [
                <c:forEach var="p" items="${pages}" varStatus="st">
                {
                    number: ${p.pageNumber},
                    imagePath: "<c:out value='${p.imagePath}'/>"
                }<c:if test="${!st.last}">,</c:if>
                </c:forEach>
            ]
        };
    </script>
    <div id="readerToast" class="reader-toast" role="status" aria-live="polite"></div>
    <script src="<c:url value='/script/reader.js'/>"></script>
</div>
<jsp:include page="/WEB-INF/views/auth/auth-required-modal.jsp"/>
<jsp:include page="/WEB-INF/views/collections/collection-global-modal.jsp"/>

<script src="<c:url value='/script/auth-required-modal.js'/>"></script>
<script src="<c:url value='/script/complaint-modal.js'/>"></script>
<script src="<c:url value='/script/collection-modal.js'/>"></script>
<script src="<c:url value='/script/notifications.js'/>"></script>
<jsp:include page="/WEB-INF/views/fragments/complaint-modal.jsp"/>
</body>
</html>