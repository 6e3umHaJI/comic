<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Вход</title>
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
                <h1>Вход в аккаунт</h1>
                <p class="auth-subtitle">Войди по почте или никнейму и паролю, либо через Google.</p>

                <div id="authClientStatus" class="auth-alert auth-alert-error" style="display:none;"></div>

                <c:if test="${not empty authStatusMessage}">
                    <div class="auth-alert auth-alert-error">
                        <c:out value="${authStatusMessage}"/>
                    </div>
                </c:if>

                <c:if test="${param.error == 'true'}">
                    <div class="auth-alert auth-alert-error">Неверный логин или пароль.</div>
                    <a href="<c:url value='/reset'/>">Забыли пароль?</a>
                </c:if>

                <c:if test="${param.resetSuccess != null}">
                    <div class="auth-status success">
                        Пароль успешно изменён. Теперь войдите с новым паролем.
                    </div>
                </c:if>

                <c:if test="${param.oauthError == 'true'}">
                    <div class="auth-alert auth-alert-error">Не удалось войти через Google.</div>
                </c:if>

                <c:if test="${param.registered == 'true'}">
                    <div class="auth-alert auth-alert-success">Аккаунт создан. Теперь можно войти.</div>
                </c:if>

                <form id="loginForm" method="post" action="<c:url value='/auth/login'/>" class="auth-form" novalidate>
                    <label class="auth-label" for="login">Почта или никнейм</label>
                    <input id="login"
                           name="login"
                           type="text"
                           class="auth-input"
                           autocomplete="username"
                           maxlength="254"
                           required>

                    <label class="auth-label" for="password">Пароль</label>
                    <input id="password"
                           name="password"
                           type="password"
                           class="auth-input"
                           autocomplete="current-password"
                           maxlength="72"
                           required>

                    <button type="submit" class="btn auth-submit">Войти</button>
                </form>

                <div class="auth-divider"><span>или</span></div>

                <a href="<c:url value='/oauth2/authorization/google'/>" class="auth-google-btn">
                    <span class="auth-google-icon"></span>
                    Войти через Google
                </a>

                <p class="auth-switch">
                    Нет аккаунта?
                    <a href="<c:url value='/oauth2/authorization/google'/>">Зарегистрироваться через Google</a>
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
        const GMAIL_REGEX = /^(?=.{6,30}@gmail\.com$)(?!\.)(?!.*\.\.)([A-Za-z0-9.]+)(?<!\.)@gmail\.com$/i;
        const GENERIC_EMAIL_REGEX = /^[^\s@]{1,64}@[^\s@]{1,190}\.[^\s@]{2,63}$/;

        const form = document.getElementById('loginForm');
        const loginInput = document.getElementById('login');
        const passwordInput = document.getElementById('password');
        const status = document.getElementById('authClientStatus');

        function showStatus(message) {
            status.textContent = message;
            status.style.display = 'block';
        }

        function hideStatus() {
            status.textContent = '';
            status.style.display = 'none';
        }

        function validateLogin(value) {
            const normalized = value.trim();

            if (normalized.includes('@')) {
                if (normalized.toLowerCase().endsWith('@gmail.com')) {
                    return GMAIL_REGEX.test(normalized)
                        ? ''
                        : 'Gmail должен содержать от 6 до 30 символов до @gmail.com. Допустимы латинские буквы, цифры и точка.';
                }

                return GENERIC_EMAIL_REGEX.test(normalized) && normalized.length <= 254
                    ? ''
                    : 'Введите корректный email.';
            }

            return USERNAME_REGEX.test(normalized)
                ? ''
                : 'Никнейм должен быть от 3 до 30 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.';
        }

        function validatePassword(value) {
            return PASSWORD_REGEX.test(value)
                ? ''
                : 'Пароль должен быть от 8 до 72 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.';
        }

        loginInput.addEventListener('input', () => {
            loginInput.value = loginInput.value.slice(0, 254);
            hideStatus();
        });

        passwordInput.addEventListener('input', () => {
            passwordInput.value = passwordInput.value.slice(0, 72);
            hideStatus();
        });

        form.addEventListener('submit', (event) => {
            hideStatus();

            loginInput.value = loginInput.value.trim();

            const loginError = validateLogin(loginInput.value);
            if (loginError) {
                event.preventDefault();
                showStatus(loginError);
                return;
            }

            const passwordError = validatePassword(passwordInput.value);
            if (passwordError) {
                event.preventDefault();
                showStatus(passwordError);
            }
        });
    })();
</script>
</body>
</html>