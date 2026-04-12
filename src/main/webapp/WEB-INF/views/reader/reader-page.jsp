<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<head>
    <title>${comic.title}, глава ${translation.chapter.chapterNumber}</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
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
     data-prev-language-name="${prevTranslation != null ? prevTranslation.language.name : ''}"
     data-next-language-name="${nextTranslation != null ? nextTranslation.language.name : ''}"
     data-prev-chapter-number="${prevTranslation != null ? prevTranslation.chapter.chapterNumber : ''}"
     data-next-chapter-number="${nextTranslation != null ? nextTranslation.chapter.chapterNumber : ''}"
     data-comic-url="<c:url value='/comics/${comic.id}'/>"
     data-back-url="<c:url value='/comics/${comic.id}?tab=chapters'/>">

    <header id="readerTopbar" class="reader-topbar">
        <div class="reader-topbar-left">
            <a href="<c:url value='/comics/${comic.id}?tab=chapters'/>" class="reader-icon-btn" title="Назад">
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
            <button type="button" id="readerComplaintBtn" class="reader-icon-btn" title="Жалоба">
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

            <div class="reader-setting-group">
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
                    <input id="settingInvertClicks" type="checkbox">
                    <span>Инвертировать клики</span>
                </label>
            </div>

            <div class="reader-setting-group reader-setting-inline">
                <label class="reader-check">
                    <input id="settingTopbarVisible" type="checkbox" checked>
                    <span>Показывать верхнюю панель</span>
                </label>
            </div>

            <div class="reader-setting-group reader-setting-inline">
                <label class="reader-check">
                    <input id="settingCounterVisible" type="checkbox" checked>
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

    <div id="readerLanguageWarning" class="modal">
        <div class="modal-content reader-warning-modal">
            <button type="button" class="tr-close" id="readerLanguageWarningClose" aria-label="Закрыть">&times;</button>
            <h3 id="readerLanguageWarningTitle">Язык перевода изменится</h3>
            <p id="readerLanguageWarningText" class="reader-warning-text"></p>
            <div class="reader-warning-actions">
                <button type="button" class="btn" id="readerLanguageWarningContinue">Перейти</button>
                <a class="btn btn-outline" id="readerLanguageWarningComicLink" href="<c:url value='/comics/${comic.id}?tab=chapters'/>">На страницу комикса</a>
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
    <script src="<c:url value='/script/reader.js'/>"></script>
    <div id="readerToast" class="reader-toast" role="status" aria-live="polite"></div>
</div>
</body>
</html>