<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:forEach var="item" items="${related}">
    <div class="related-comic">
        <a href="<c:url value='/comics/${item.comic.id}'/>" class="related-link">
            <div class="cover-wrap">
                <img src="${pageContext.request.contextPath}/assets/covers/${item.comic.cover}" alt="${item.comic.title}">
                <span class="rating-badge">★ <fmt:formatNumber value="${item.comic.avgRating}" pattern="0.00"/></span>
            </div>

            <p class="related-title">${item.comic.title}</p>

            <c:if test="${not empty item.relationType}">
                <div class="related-types">
                    <c:forEach var="relType" items="${fn:split(item.relationType, ',')}">
                        <span class="rel-chip">${fn:trim(relType)}</span>
                    </c:forEach>
                </div>
            </c:if>
        </a>
    </div>
</c:forEach>
