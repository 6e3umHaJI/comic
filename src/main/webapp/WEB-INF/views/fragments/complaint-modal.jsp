<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:url var="complaintSubmitUrl" value="/complaints/submit"/>

<div id="complaintModal" class="modal hidden complaint-modal" aria-modal="true" role="dialog">
    <div class="modal-content complaint-modal-content">
        <button type="button"
                id="complaintCloseBtn"
                class="close-button complaint-close-btn"
                aria-label="Закрыть">
            &times;
        </button>

        <h3 class="complaint-modal-title">Отправить жалобу</h3>

        <form id="complaintForm"
              class="complaint-form"
              action="${complaintSubmitUrl}"
              method="post">
            <input type="hidden" name="targetId" id="complaintTargetId">

            <div class="complaint-form-group">
                <label for="complaintTypeId">Тип жалобы</label>
                <select id="complaintTypeId" name="complaintTypeId" required>
                    <c:forEach var="type" items="${complaintTypes}">
                        <option value="${type.id}">${type.name}</option>
                    </c:forEach>
                </select>
            </div>

            <div class="complaint-form-group">
                <label for="complaintDescription">Описание жалобы</label>
                <textarea id="complaintDescription"
                          name="description"
                          maxlength="200"
                          required
                          placeholder="Опишите проблему подробнее(до 200 символов)"></textarea>
            </div>

            <div id="complaintFormStatus" class="complaint-form-status hidden"></div>

            <div class="complaint-form-actions">
                <button type="submit" class="btn">Отправить</button>
            </div>
        </form>
    </div>
</div>