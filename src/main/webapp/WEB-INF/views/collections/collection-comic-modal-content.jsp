<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="collection-picker" data-comic-id="${comicId}">
    <h3>Коллекция</h3>

    <div class="collection-create-inline">
        <input type="text"
               id="newCollectionName"
               class="auth-input collection-name-input"
               placeholder="Новая категория...">

        <button type="button"
                class="btn collection-add-btn"
                id="createCollectionFromModalBtn"
                title="Добавить категорию">
            <span class="btn-icon"
                  style="-webkit-mask-image:url('/assets/icons/plus.svg'); mask-image:url('/assets/icons/plus.svg');"></span>
        </button>
    </div>

    <div class="collection-inline-notice" id="collectionModalNotice" hidden></div>

    <div class="collection-picker-scroll">
        <div class="collection-picker-list">
            <c:forEach items="${sections}" var="section">
                <button type="button"
                        class="collection-choice-btn ${selectedSectionIds.contains(section.id) ? 'selected' : ''}"
                        data-section-id="${section.id}"
                        aria-pressed="${selectedSectionIds.contains(section.id) ? 'true' : 'false'}">
                    <span class="collection-choice-name"><c:out value="${section.name}"/></span>
                </button>
            </c:forEach>
        </div>
    </div>

    <div class="collection-modal-actions">
        <button type="button" class="btn collection-save-btn" id="saveComicCollectionsBtn">
            Сохранить
        </button>

        <c:if test="${inCollections}">
            <button type="button" class="btn btn-outline collection-remove-all-btn" id="removeComicFromAllBtn">
                Удалить отовсюду
            </button>
        </c:if>
    </div>
</div>
