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
                    <li class="translation-item">
                        <a class="tr-card" href="<c:url value='/read/${t.id}'/>">
                            <div><b>Язык:</b> ${t.language.name}</div>
                            <div><b>Тип перевода:</b> ${t.translationType.name}</div>

                            <c:if test="${t.translationType.name == 'Любительский' and not empty t.user}">
                                <div><b>Автор перевода:</b> ${t.user.username}</div>
                            </c:if>

                            <div>
                                <b>Добавлено:</b>
                                <time class="tr-date" datetime="${t.createdAtIso}">
                                    ${t.createdAtFormatted}
                                </time>
                            </div>
                        </a>
                    </li>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </ul>
</div>