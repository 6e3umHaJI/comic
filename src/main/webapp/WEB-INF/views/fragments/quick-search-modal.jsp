<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div id="quickSearchModal"
     class="quick-search-modal"
     data-search-url="<c:url value='/search/quick'/>"
     data-catalog-apply-url="<c:url value='/catalog/apply'/>"
     hidden>
    <div class="quick-search-dialog" role="dialog" aria-modal="true" aria-labelledby="quickSearchInput">
        <div class="quick-search-head">
            <input type="text"
                   id="quickSearchInput"
                   class="quick-search-input"
                   placeholder="Введите название или оригинальное название"
                   autocomplete="off">
            <button type="button"
                    class="quick-search-close"
                    id="quickSearchClose"
                    aria-label="Закрыть">
                &times;
            </button>
        </div>

        <div class="quick-search-body">
            <div class="quick-search-message" id="quickSearchMessage">
                Введите название или оригинальное название.
            </div>

            <div class="quick-search-results" id="quickSearchResults"></div>
            <div class="quick-search-footer" id="quickSearchFooter" hidden>
                <button type="button" class="btn" id="quickSearchMoreBtn" hidden></button>
                <a class="btn btn-outline" id="quickSearchCatalogLink" hidden>
                    Открыть все результаты в каталоге
                </a>
            </div>
        </div>
    </div>
</div>
