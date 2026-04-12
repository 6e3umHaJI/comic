<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

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

                <c:if test="${not empty registerError}">
                    <div class="auth-alert auth-alert-error">${registerError}</div>
                </c:if>

                <div class="auth-google-profile">
                    <c:if test="${not empty pendingGoogle.avatarUrl}">
                        <img src="${pendingGoogle.avatarUrl}" alt="Google avatar" class="auth-avatar">
                    </c:if>
                    <div>
                        <div class="auth-google-name">${pendingGoogle.displayName}</div>
                        <div class="auth-google-email">${pendingGoogle.email}</div>
                    </div>
                </div>

                <form:form method="post" modelAttribute="form" class="auth-form">
                    <label class="auth-label" for="username">Никнейм</label>
                    <form:input path="username" id="username" cssClass="auth-input" autocomplete="nickname"/>
                    <form:errors path="username" cssClass="auth-field-error"/>

                    <label class="auth-label" for="password">Пароль</label>
                    <form:password path="password" id="password" cssClass="auth-input" autocomplete="new-password"/>
                    <form:errors path="password" cssClass="auth-field-error"/>

                    <label class="auth-label" for="confirmPassword">Повтори пароль</label>
                    <form:password path="confirmPassword" id="confirmPassword" cssClass="auth-input" autocomplete="new-password"/>
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
</body>
</html>