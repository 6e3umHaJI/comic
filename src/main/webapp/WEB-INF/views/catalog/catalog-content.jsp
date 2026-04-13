<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="comics-container ${searchCriteria.viewMode == 'list' ? 'list-view' : 'card-view'}">
    <c:choose>
        <c:when test="${empty comics}">
            <div class="no-results">
                <p>Ничего не найдено...</p>
            </div>
        </c:when>
        <c:otherwise>
            <c:forEach items="${comics}" var="comic">
                <c:choose>
                    <c:when test="${searchCriteria.viewMode == 'list'}">
                        <div class="list-comic">
                            <img src="/assets/covers/${comic.cover}" alt="${comic.title}" class="comic-list-cover">
                            <div class="list-info">
                                <h3><a href="<c:url value='/comics/${comic.id}'/>">${comic.title}</a></h3>
                                <c:if test="${not empty comic.originalTitle}">
                                  <p class="original-title">(${comic.originalTitle})</p>
                                </c:if>
                                <p class="release"><b>Релиз:</b> ${comic.releaseYear}</p>
                                <p class="short-desc">${comic.shortDescription}</p>
                                <p class="meta">
                                    <span class="rating">★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></span> |
                                    <span class="genres">
                                        <c:forEach items="${comic.genres}" var="g" varStatus="st">
                                            ${g.name}<c:if test="${!st.last}">, </c:if>
                                        </c:forEach>
                                    </span>
                                </p>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="card-comic">
                            <a href="<c:url value='/comics/${comic.id}'/>" class="cover-link">
                                <img src="/assets/covers/${comic.cover}" alt="${comic.title}">
                            </a>
                            <h4><a href="<c:url value='/comics/${comic.id}'/>">${comic.title}</a></h4>
                            <c:if test="${not empty comic.originalTitle}">
                                <p class="orig-name">(${comic.originalTitle})</p>
                            </c:if>
                            <p>${comic.releaseYear}</p>
                            <p>★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${totalPages > 1}">
    <div class="pagination-management">
        <ul class="pagination">

            <c:if test="${currentPage > 1}">
                <li>
                    <a href="#"
                       onclick="changePage(${currentPage - 1}); return false;"
                       class="pagination-icon-link"
                       aria-label="Предыдущая страница">
                        <span class="nav-icon"
                              style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span>
                    </a>
                </li>
            </c:if>

            <c:if test="${beginPage > 1}">
                <li><a href="#" onclick="changePage(1); return false;">1</a></li>
            </c:if>

            <c:if test="${showLeftDots}">
                <li class="disabled"><span>...</span></li>
            </c:if>

            <c:forEach var="i" begin="${beginPage}" end="${endPage}">
                <li>
                    <a href="#"
                       onclick="changePage(${i}); return false;"
                       class="${i == currentPage ? 'active-page' : ''}">
                        ${i}
                    </a>
                </li>
            </c:forEach>

            <c:if test="${showRightDots}">
                <li class="disabled"><span>...</span></li>
            </c:if>

            <c:if test="${endPage < totalPages}">
                <li><a href="#" onclick="changePage(${totalPages}); return false;">${totalPages}</a></li>
            </c:if>

            <c:if test="${currentPage < totalPages}">
                <li>
                    <a href="#"
                       onclick="changePage(${currentPage + 1}); return false;"
                       class="pagination-icon-link"
                       aria-label="Следующая страница">
                        <span class="nav-icon"
                              style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>');"></span>
                    </a>
                </li>
            </c:if>

        </ul>
    </div>
</c:if>
