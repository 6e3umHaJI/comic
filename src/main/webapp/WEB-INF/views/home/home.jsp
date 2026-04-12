<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<html data-theme="light">
<head>
  <title>Главная</title>
  <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
  <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link href="<c:url value='/style/common.css'/>" rel="stylesheet">
  <link href="<c:url value='/style/home.css'/>" rel="stylesheet">
</head>
<body data-authenticated="${pageContext.request.userPrincipal != null}">
<div class="wrapper">
  <jsp:include page="/WEB-INF/views/header.jsp"/>

  <main class="main container">

    <section>
      <div class="hero-text">
        <h1>Откройте мир комиксов</h1>
        <p>Читайте популярные тайтлы, следите за новыми главами и находите переводы на любимом языке.</p>
      </div>
    </section>

    <!-- Популярные: горизонтальный скролл -->
    <section class="home-section">
      <div class="section-head">
        <h2>Самые популярные</h2>
        <a class="see-all" href="<c:url value='/catalog?sort=popular'/>">Все популярные →</a>
      </div>
      <div class="card-scroller" tabindex="0">
        <c:forEach var="comic" items="${popularComics}">
          <a class="card-tile" href="<c:url value='/comics/${comic.id}'/>">
            <div class="cover-wrap">
              <img src="<c:url value='/assets/covers/${comic.cover}'/>" alt="${comic.title}">
              <span class="rating-badge">★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></span>
            </div>
            <div class="tile-title" title="${comic.title}">${comic.title}</div>
          </a>
        </c:forEach>
      </div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <h2>Последние обновления</h2>
      </div>
      <div class="updates-list">
        <c:forEach var="item" items="${recentUpdates}">
          <a class="update-row" href="<c:url value='/comics/${item.comicId}'/>">
            <div class="u-cover">
              <img src="<c:url value='/assets/covers/${item.comicCover}'/>" alt="${item.comicTitle}">
            </div>
            <div class="u-main">
              <div class="u-title">${item.comicTitle}</div>
              <div class="u-meta">
                <b>Глава: </b>${item.chapterNumber}
                <b>Язык: </b>${item.languageName}
                <b>Добавлено: </b><time class="u-date" datetime="${item.createdAtIso}">${item.createdAtFormatted}</time>
              </div>
            </div>
          </a>
        </c:forEach>
      </div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <h2>Новинки</h2>
        <a class="see-all" href="<c:url value='/catalog?sort=new'/>">Все новинки →</a>
      </div>
      <div class="grid-tiles">
        <c:forEach var="comic" items="${newComics}">
          <a class="grid-card" href="<c:url value='/comics/${comic.id}'/>">
            <div class="cover-wrap">
              <img src="<c:url value='/assets/covers/${comic.cover}'/>" alt="${comic.title}">
              <span class="rating-badge">★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></span>
            </div>
            <div class="g-title">${comic.title}</div>
            <div class="g-sub">
              Добавлен: <time class="u-date" datetime="${comic.createdAtIso}">${comic.createdAtFormatted}</time>
            </div>
          </a>
        </c:forEach>
      </div>
    </section>

  </main>

  <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>
</body>
</html>