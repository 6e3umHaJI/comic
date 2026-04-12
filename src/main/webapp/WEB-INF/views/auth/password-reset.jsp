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

            <c:if test="${not empty status}">
                <div class="auth-status ${statusColor == 'green' ? 'success' : 'error'}">
                    ${status}
                </div>
            </c:if>

            <c:choose>
                <c:when test="${step == 'verify'}">
                    <p class="auth-hint">
                        Мы отправили код на <b>${maskedEmail}</b>
                    </p>

                    <form method="post" action="<c:url value='/reset/confirm'/>" class="auth-form">
                        <input type="hidden" name="login" value="${login}"/>

                        <label for="code">Код</label>
                        <input id="code" name="code" type="text" maxlength="6" value="${verifyForm.code}" required>

                        <label for="newPassword">Новый пароль</label>
                        <input id="newPassword" name="newPassword" type="password" required>

                        <label for="repeatPassword">Повторите пароль</label>
                        <input id="repeatPassword" name="repeatPassword" type="password" required>

                        <button type="submit" class="btn auth-submit">Сменить пароль</button>
                    </form>

                    <div class="auth-links">
                        <a href="<c:url value='/reset?login=${login}'/>">Отправить код заново</a>
                        <a href="<c:url value='/auth/login'/>">Вернуться ко входу</a>
                    </div>
                </c:when>

                <c:otherwise>
                    <p class="auth-hint">
                        Введите email или никнейм. Мы отправим код на привязанную почту.
                    </p>

                    <form method="post" action="<c:url value='/reset/send-code'/>" class="auth-form">
                        <label for="login">Email или никнейм</label>
                        <input id="login" name="login" type="text" value="${requestForm.login}" required>

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
</body>
</html>