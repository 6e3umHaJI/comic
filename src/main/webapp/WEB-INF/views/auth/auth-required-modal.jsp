<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div id="authRequiredModal" class="modal auth-required-modal">
    <div class="modal-content auth-required-modal-content">
        <button type="button" class="tr-close" id="authRequiredModalClose" aria-label="Закрыть">&times;</button>
        <h3>Нужен аккаунт</h3>
        <p class="auth-required-text">
            Чтобы выполнить это действие, нужно войти в аккаунт.
        </p>
        <div class="auth-required-actions">
            <a href="<c:url value='/auth/login'/>" class="btn">Войти</a>
            <button type="button" class="btn btn-outline" id="authRequiredStayBtn">Остаться на странице</button>
        </div>
    </div>
</div>