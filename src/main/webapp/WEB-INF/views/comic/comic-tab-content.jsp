<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:choose>
  <c:when test="${tab == 'description'}">
    <c:choose>
      <c:when test="${fn:length(comic.fullDescription) > 400}">
        <p id="descShort">${fn:substring(comic.fullDescription,0,400)}...</p>
        <p id="descFull" class="hidden">${comic.fullDescription}</p>
        <button id="toggleDesc" class="btn btn-outline">Показать полностью</button>
      </c:when>
      <c:otherwise>
        <p>${comic.fullDescription}</p>
      </c:otherwise>
    </c:choose>

    <c:if test="${relatedCount > 0}">
      <h3>Связанные тайтлы</h3>
      <div id="relatedBlock" data-total="${relatedCount}" data-size="5">
        <div id="relatedContainer" class="related-carousel"></div>
        <div class="carousel-controls <c:if test='${relatedCount <= 5}'>hidden</c:if>">
          <button id="relatedPrev" class="btn btn-outline icon-only-btn" disabled type="button" aria-label="Назад">
            <span class="btn-icon"
                  style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span>
          </button>
          <span id="relatedPageInfo" class="small"></span>
          <button id="relatedNext" class="btn icon-only-btn" type="button" aria-label="Вперёд">
            <span class="btn-icon"
                  style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>');"></span>
          </button>
        </div>
      </div>
    </c:if>

    <c:if test="${not empty similarComics}">
      <h3 style="margin-top:25px;">Похожие комиксы</h3>
      <div class="related-carousel">
        <c:forEach var="s" items="${similarComics}">
          <div class="related-comic">
            <a href="<c:url value='/comics/${s.id}'/>" style="display:block;">
              <div class="cover-wrap">
                <img src="${pageContext.request.contextPath}/assets/covers/${s.cover}" alt="${s.title}">
                <span class="rating-badge">★ <fmt:formatNumber value="${s.avgRating}" pattern="0.00"/></span>
              </div>
              <p>${s.title}</p>
            </a>
          </div>
        </c:forEach>
      </div>
    </c:if>

    <h3 style="margin-top:25px;">Статистика отзывов</h3>
    <div class="rating-stats">
      <p>Средняя оценка: ★ ${comic.avgRating} (оценок: ${comic.ratingsCount})</p>
      <table class="rating-table">
        <c:forEach var="entry" items="${ratingDistribution}">
          <tr>
            <td style="width:40px;">${entry.key}★</td>
            <td style="width:100%;">
              <div class="bar" style="width:${comic.ratingsCount > 0 ? (entry.value * 100 / comic.ratingsCount) : 0}%"></div>
            </td>
            <td>${entry.value}</td>
          </tr>
        </c:forEach>
      </table>
    </div>

    <h4 style="margin-top:15px;">Добавлено в коллекцию:</h4>
    <ul>
      <c:forEach var="section" items="${favoriteStats}">
        <li><b>${section.key}</b> — ${section.value}</li>
      </c:forEach>
    </ul>
  </c:when>

  <c:when test="${tab == 'chapters'}">
    <div id="chaptersBlock"
         data-total="0"
         data-size="${chaptersPageSize}"
         data-dir="desc">

      <div class="chapters-controls" style="display:flex;gap:10px;align-items:center;margin:10px 0;">
        <input type="text" id="chapterSearch" placeholder="Номер главы..."
               style="flex:1 1 auto;max-width:420px;">
        <button type="button" id="chapterDirToggle" class="btn btn-outline icon-only-btn" title="Сначала новые" aria-label="Сортировка">
            <span class="btn-icon"
                  style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-up.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-up.svg"/>');"></span>
        </button>
      </div>

      <div id="chaptersContainer"></div>
    </div>
  </c:when>
</c:choose>