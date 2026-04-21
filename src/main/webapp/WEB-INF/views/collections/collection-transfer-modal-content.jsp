<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="collection-picker">
    <h3><c:out value="${title}"/></h3>

    <div class="collection-inline-notice" id="collectionTransferNotice" hidden></div>

    <div class="collection-picker-list">
        <c:forEach items="${targetSections}" var="section">
            <button type="button"
                    class="collection-choice-btn"
                    data-transfer-target-id="${section.id}">
                <span class="collection-choice-name"><c:out value="${section.name}"/></span>
            </button>
        </c:forEach>
    </div>

    <div class="collection-modal-actions">
        <button type="button"
                class="btn collection-save-btn"
                id="confirmTransferActionBtn"
                data-action="${action}"
                data-section-id="${sectionId}">
            Подтвердить
        </button>
    </div>
</div>
