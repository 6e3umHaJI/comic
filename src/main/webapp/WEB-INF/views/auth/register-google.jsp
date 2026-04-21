<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html data-theme="light">
<head>
    <title>Регистрация через Google</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/auth.css'/>">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container">
        <div class="auth-page">
            <div class="auth-card">
                <h1>Заверши регистрацию</h1>
                <p class="auth-subtitle">Google-аккаунт подтвержден. Осталось придумать никнейм и пароль.</p>

                <div id="registerClientStatus" class="auth-alert auth-alert-error" style="display:none;"></div>

                <c:if test="${not empty registerError}">
                    <div class="auth-alert auth-alert-error"><c:out value="${registerError}"/></div>
                </c:if>

                <div class="auth-google-profile">
                    <c:if test="${not empty pendingGoogle.avatarUrl}">
                        <img src="${fn:escapeXml(pendingGoogle.avatarUrl)}"
                             alt="Google avatar"
                             class="auth-avatar">
                    </c:if>

                    <div>
                        <div class="auth-google-name"><c:out value="${pendingGoogle.displayName}"/></div>
                        <div class="auth-google-email"><c:out value="${pendingGoogle.email}"/></div>
                    </div>
                </div>

                <form:form id="googleRegisterForm" method="post" modelAttribute="form" class="auth-form" novalidate="novalidate">
                    <label class="auth-label" for="username">Никнейм</label>
                    <form:input path="username"
                                id="username"
                                cssClass="auth-input"
                                autocomplete="nickname"
                                maxlength="30"/>
                    <form:errors path="username" cssClass="auth-field-error"/>

                    <label class="auth-label" for="password">Пароль</label>
                    <form:password path="password"
                                   id="password"
                                   cssClass="auth-input"
                                   autocomplete="new-password"
                                   maxlength="72"/>
                    <form:errors path="password" cssClass="auth-field-error"/>

                    <label class="auth-label" for="confirmPassword">Повтори пароль</label>
                    <form:password path="confirmPassword"
                                   id="confirmPassword"
                                   cssClass="auth-input"
                                   autocomplete="new-password"
                                   maxlength="72"/>
                    <form:errors path="confirmPassword" cssClass="auth-field-error"/>

                    <button type="submit" class="btn auth-submit">Создать аккаунт</button>
                </form:form>

                <p class="auth-switch">
                    Уже есть аккаунт?
                    <a href="<c:url value='/auth/login'/>">Перейти ко входу</a>
                </p>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script>
    (() => {
        const USERNAME_REGEX = /^[A-Za-z0-9_-]{3,30}$/;
        const PASSWORD_REGEX = /^[A-Za-z0-9_-]{8,72}$/;

        const form = document.getElementById('googleRegisterForm');
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');
        const status = document.getElementById('registerClientStatus');

        function showStatus(message) {
            status.textContent = message;
            status.style.display = 'block';
        }

        function hideStatus() {
            status.textContent = '';
            status.style.display = 'none';
        }

        usernameInput.addEventListener('input', () => {
            usernameInput.value = usernameInput.value.slice(0, 30);
            hideStatus();
        });

        passwordInput.addEventListener('input', () => {
            passwordInput.value = passwordInput.value.slice(0, 72);
            hideStatus();
        });

        confirmPasswordInput.addEventListener('input', () => {
            confirmPasswordInput.value = confirmPasswordInput.value.slice(0, 72);
            hideStatus();
        });

        form.addEventListener('submit', (event) => {
            hideStatus();

            usernameInput.value = usernameInput.value.trim();

            if (!USERNAME_REGEX.test(usernameInput.value)) {
                event.preventDefault();
                showStatus('Никнейм должен быть от 3 до 30 символов и может содержать только латинские буквы(a-z,A-Z), цифры(0-9), дефис(-) и подчёркивание(_).');
                return;
            }

            if (!PASSWORD_REGEX.test(passwordInput.value)) {
                event.preventDefault();
                showStatus('Пароль должен быть от 8 до 72 символов и может содержать только латинские буквы(a-z,A-Z), цифры(0-9), дефис(-) и подчёркивание(_).');
                return;
            }

            if (!PASSWORD_REGEX.test(confirmPasswordInput.value)) {
                event.preventDefault();
                showStatus('Пароль должен быть от 8 до 72 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.');
                return;
            }

            if (passwordInput.value !== confirmPasswordInput.value) {
                event.preventDefault();
                showStatus('Пароли не совпадают.');
            }
        });
    })();
</script>
</body>
</html>
