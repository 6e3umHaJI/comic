<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>Восстановление пароля</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/auth.css'/>">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container auth-page">
        <div class="auth-card">
            <h1>Восстановление пароля</h1>

            <div id="resetClientStatus" class="auth-alert auth-alert-error" style="display:none;"></div>

            <c:if test="${not empty status}">
                <div class="auth-status ${statusColor == 'green' ? 'success' : 'error'}">
                    <c:out value="${status}"/>
                </div>
            </c:if>

            <c:choose>
                <c:when test="${step == 'verify'}">
                    <p class="auth-hint">
                        Мы отправили код на <b><c:out value="${maskedEmail}"/></b>
                    </p>

                    <form id="resetVerifyForm" method="post" action="<c:url value='/reset/confirm'/>" class="auth-form" novalidate>
                        <input type="hidden" name="login" value="<c:out value='${login}'/>"/>

                        <label for="code">Код</label>
                        <input id="code"
                               name="code"
                               type="text"
                               inputmode="numeric"
                               maxlength="6"
                               value="<c:out value='${verifyForm.code}'/>"
                               required>

                        <label for="newPassword">Новый пароль</label>
                        <input id="newPassword"
                               name="newPassword"
                               type="password"
                               maxlength="72"
                               required>

                        <label for="repeatPassword">Повторите пароль</label>
                        <input id="repeatPassword"
                               name="repeatPassword"
                               type="password"
                               maxlength="72"
                               required>

                        <button type="submit" class="btn auth-submit">Сменить пароль</button>
                    </form>

                    <div class="auth-links">
                        <c:url var="resendUrl" value="/reset">
                            <c:param name="login" value="${login}"/>
                        </c:url>
                        <a href="${resendUrl}">Отправить код заново</a>
                        <a href="<c:url value='/auth/login'/>">Вернуться ко входу</a>
                    </div>
                </c:when>

                <c:otherwise>
                    <p class="auth-hint">
                        Введите email или никнейм. Мы отправим код на привязанную почту.
                    </p>

                    <form id="resetRequestForm" method="post" action="<c:url value='/reset/send-code'/>" class="auth-form" novalidate>
                        <label for="login">Email или никнейм</label>
                        <input id="login"
                               name="login"
                               type="text"
                               maxlength="254"
                               value="<c:out value='${requestForm.login}'/>"
                               required>

                        <button type="submit" class="btn auth-submit">Получить код</button>
                    </form>

                    <div class="auth-links">
                        <a href="<c:url value='/auth/login'/>">Вернуться ко входу</a>
                    </div>
                </c:otherwise>
            </c:choose>
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
        const CODE_REGEX = /^[0-9]{6}$/;

        const requestForm = document.getElementById('resetRequestForm');
        const verifyForm = document.getElementById('resetVerifyForm');
        const status = document.getElementById('resetClientStatus');

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

        if (requestForm) {
            const loginInput = document.getElementById('login');

            loginInput.addEventListener('input', () => {
                loginInput.value = loginInput.value.slice(0, 254);
                hideStatus();
            });

            requestForm.addEventListener('submit', (event) => {
                hideStatus();
                loginInput.value = loginInput.value.trim();

                const loginError = validateLogin(loginInput.value);
                if (loginError) {
                    event.preventDefault();
                    showStatus(loginError);
                }
            });
        }

        if (verifyForm) {
            const codeInput = document.getElementById('code');
            const newPasswordInput = document.getElementById('newPassword');
            const repeatPasswordInput = document.getElementById('repeatPassword');

            codeInput.addEventListener('input', () => {
                codeInput.value = codeInput.value.replace(/\D/g, '').slice(0, 6);
                hideStatus();
            });

            newPasswordInput.addEventListener('input', () => {
                newPasswordInput.value = newPasswordInput.value.slice(0, 72);
                hideStatus();
            });

            repeatPasswordInput.addEventListener('input', () => {
                repeatPasswordInput.value = repeatPasswordInput.value.slice(0, 72);
                hideStatus();
            });

            verifyForm.addEventListener('submit', (event) => {
                hideStatus();

                if (!CODE_REGEX.test(codeInput.value)) {
                    event.preventDefault();
                    showStatus('Код должен состоять из 6 цифр.');
                    return;
                }

                if (!PASSWORD_REGEX.test(newPasswordInput.value)) {
                    event.preventDefault();
                    showStatus('Пароль должен быть от 8 до 72 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.');
                    return;
                }

                if (!PASSWORD_REGEX.test(repeatPasswordInput.value)) {
                    event.preventDefault();
                    showStatus('Пароль должен быть от 8 до 72 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.');
                    return;
                }

                if (newPasswordInput.value !== repeatPasswordInput.value) {
                    event.preventDefault();
                    showStatus('Пароли не совпадают.');
                }
            });
        }
    })();
</script>
</body>
</html>
