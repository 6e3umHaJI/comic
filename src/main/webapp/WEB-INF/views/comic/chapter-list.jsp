<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div id="chaptersChunk"
     data-total="${total}"
     data-total-pages="${totalPages}"
     data-comic-id="${comicId}"
     data-page="${page}"
     data-size="${size}"
     data-dir="${dir}"
     data-q="${q}">

    <c:choose>
        <c:when test="${total == 0}">
            <p class="muted">
                <c:choose>
                    <c:when test="${empty q}">В скорейшем времени главы будут добавлены.</c:when>
                    <c:otherwise>Ничего не найдено.</c:otherwise>
                </c:choose>
            </p>
        </c:when>
        <c:otherwise>
            <div class="chapter-tree">
                <c:forEach var="chapter" items="${chapters}">
                    <c:set var="__langsCsv" value="${langsCsvByChapter[chapter.id]}"/>
                    <div class="chapter-group chapter-click"
                         data-chapter-id="${chapter.id}"
                         data-langs="${empty __langsCsv ? '' : __langsCsv}"
                         role="button"
                         tabindex="0">
                        <div class="chapter-header">
                            <b>Глава ${chapter.chapterNumber}</b>
                            <span class="small muted">Нажмите, чтобы открыть переводы</span>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <c:if test="${totalPages > 1}">
                <div class="pagination-management">
                    <ul class="pagination chaptersPagination">

                        <c:if test="${currentPage > 1}">
                            <li>
                                <a href="#" data-page="${currentPage - 2}" class="pagination-icon-link" aria-label="Предыдущая страница">
                                    <span class="nav-icon"
                                          style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-left.svg"/>');"></span>
                                </a>
                            </li>
                        </c:if>

                        <c:if test="${beginPage > 1}">
                            <li>
                                <a href="#" data-page="0">1</a>
                            </li>
                        </c:if>

                        <c:if test="${showLeftDots}">
                            <li class="disabled"><span>...</span></li>
                        </c:if>

                        <c:forEach var="i" begin="${beginPage}" end="${endPage}">
                            <li>
                                <a href="#"
                                   data-page="${i - 1}"
                                   class="${i == currentPage ? 'active-page' : ''}">
                                    ${i}
                                </a>
                            </li>
                        </c:forEach>

                        <c:if test="${showRightDots}">
                            <li class="disabled"><span>...</span></li>
                        </c:if>

                        <c:if test="${endPage < totalPages}">
                            <li>
                                <a href="#" data-page="${totalPages - 1}">${totalPages}</a>
                            </li>
                        </c:if>

                        <c:if test="${currentPage < totalPages}">
                            <li>
                                <a href="#" data-page="${currentPage}" class="pagination-icon-link" aria-label="Следующая страница">
                                    <span class="nav-icon"
                                          style="-webkit-mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>'); mask-image:url('<c:url value="/assets/icons/arrow-right.svg"/>');"></span>
                                </a>
                            </li>
                        </c:if>

                    </ul>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>