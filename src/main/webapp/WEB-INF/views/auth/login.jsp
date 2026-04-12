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

                <form method="post" action="<c:url value='/auth/login'/>" class="auth-form">
                    <label class="auth-label" for="login">Почта или никнейм</label>
                    <input id="login" name="login" type="text" class="auth-input" autocomplete="username" required>

                    <label class="auth-label" for="password">Пароль</label>
                    <input id="password" name="password" type="password" class="auth-input" autocomplete="current-password" required>

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
</body>
</html>