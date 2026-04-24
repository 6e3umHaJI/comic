<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div id="translationsChunk"
     data-total="${total}"
     data-total-pages="${totalPages}"
     data-page="${page}"
     data-size="${size}"
     data-lang="${lang}">
    <ul class="translation-list">
        <c:choose>
            <c:when test="${total == 0}">
                <li class="translation-empty"><em>Нет переводов для выбранного языка.</em></li>
            </c:when>

            <c:otherwise>
                <c:forEach var="t" items="${translations}">
                    <c:set var="isReadTranslation" value="${readTranslationIds.contains(t.id)}"/>

                    <li class="translation-item">
                        <a class="tr-card${isReadTranslation ? ' is-read' : ''}" href="<c:url value='/read/${t.id}'/>">
                            <div class="tr-card-head">
                                <div>
                                    <b>Название перевода:</b>
                                    <c:choose>
                                        <c:when test="${not empty t.title}">
                                            <c:out value="${t.title}"/>
                                        </c:when>
                                        <c:otherwise>
                                            Без названия
                                        </c:otherwise>
                                    </c:choose>
                                </div>

                                <c:if test="${isReadTranslation}">
                                    <span class="tr-read-badge">Читали</span>
                                </c:if>
                            </div>

                            <div><b>Язык:</b> <c:out value="${t.language.name}"/></div>
                            <div><b>Тип перевода:</b> <c:out value="${t.translationType.name}"/></div>

                            <c:if test="${t.translationType.name == 'Любительский' and not empty t.user}">
                                <div><b>Автор перевода:</b> <c:out value="${t.user.username}"/></div>
                            </c:if>

                            <div>
                                <b>Добавлено:</b>
                                <time class="tr-date" datetime="${t.createdAtIso}">
                                    <c:out value="${t.createdAtFormatted}"/>
                                </time>
                            </div>
                        </a>

                        <c:if test="${isAdmin}">
                            <div class="translation-admin-actions">
                                <a href="<c:url value='/admin/translations/${t.id}/edit'/>" class="btn btn-outline">
                                    Редактировать
                                </a>

                                <form action="<c:url value='/admin/translations/${t.id}/delete'/>"
                                      method="post"
                                      class="translation-admin-delete-form">
                                    <button type="button"
                                            class="btn btn-outline js-admin-delete-translation"
                                            data-translation-title="<c:out value='${t.title}'/>"
                                            data-chapter-number="${t.chapter.chapterNumber}"
                                            data-language-name="<c:out value='${t.language.name}'/>">
                                        Удалить
                                    </button>
                                </form>
                            </div>
                        </c:if>
                    </li>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </ul>
</div>
