<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:forEach var="item" items="${related}">
  <div class="related-comic">
    <a href="<c:url value='/comics/${item.comic.id}'/>" style="display:block;">
      <div class="cover-wrap">
        <span class="rel-badge">${item.relationType}</span>
        <img src="${pageContext.request.contextPath}/assets/covers/${item.comic.cover}" alt="${item.comic.title}">
        <span class="rating-badge">★ <fmt:formatNumber value="${item.comic.avgRating}" pattern="0.00"/></span>
      </div>
      <p>${item.comic.title}</p>
    </a>
  </div>
</c:forEach>