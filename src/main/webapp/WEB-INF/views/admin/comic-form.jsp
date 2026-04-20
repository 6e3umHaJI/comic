<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html data-theme="light">
<head>
    <title>${pageTitle}</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp"/>
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/admin-comic-form.css'/>">
</head>
<body>
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container admin-comic-form-page">
        <div class="admin-comic-form-head">
            <h1>${pageTitle}</h1>
            <p>Заполните данные комикса и сохраните изменения.</p>
        </div>

        <div id="clientFormError" class="admin-form-alert is-error hidden"></div>

        <c:if test="${not empty errorMessage}">
            <div class="admin-form-alert is-error">${errorMessage}</div>
        </c:if>

        <c:url var="saveComicUrl" value="/admin/comics/save"/>
        <c:url var="searchComicUrl" value="/admin/comics/search"/>

        <form id="adminComicForm"
              action="${saveComicUrl}"
              method="post"
              enctype="multipart/form-data"
              class="admin-comic-form"
              novalidate>

            <input type="hidden" name="comicId" value="${form.comicId}">
            <input type="hidden" name="currentCover" value="${form.currentCover}">
            <input type="hidden" name="genreOperationsJson" id="genreOperationsJson" value="<c:out value='${form.genreOperationsJson}'/>">
            <input type="hidden" name="tagOperationsJson" id="tagOperationsJson" value="<c:out value='${form.tagOperationsJson}'/>">
            <input type="hidden" name="relationTypeOperationsJson" id="relationTypeOperationsJson" value="<c:out value='${form.relationTypeOperationsJson}'/>">
            <input type="hidden" name="relationsJson" id="relationsJson" value="<c:out value='${form.relationsJson}'/>">

            <div id="selectedGenreInputs"></div>
            <div id="selectedTagInputs"></div>

            <div class="admin-comic-form-grid">
                <section class="admin-form-card admin-form-card-cover">
                    <h2>Обложка</h2>

                    <div class="cover-preview-wrap">
                        <c:choose>
                            <c:when test="${not empty form.currentCover}">
                                <img id="coverPreview"
                                     class="cover-preview-image"
                                     src="<c:url value='/assets/covers/${form.currentCover}'/>"
                                     alt="Обложка">
                            </c:when>
                            <c:otherwise>
                                <div id="coverPreviewPlaceholder" class="cover-preview-placeholder">
                                    Обложка не загружена
                                </div>
                                <img id="coverPreview" class="cover-preview-image hidden" alt="Обложка">
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <label class="admin-field-label" for="coverFile">Файл обложки</label>
                    <input type="file"
                           id="coverFile"
                           name="coverFile"
                           accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp">
                    <div class="field-hint">До 5 МБ. Форматы: JPG, PNG, WEBP.</div>
                </section>

                <section class="admin-form-card">
                    <h2>Основная информация</h2>

                    <div class="admin-field-group">
                        <label for="title">Название *</label>
                        <input type="text" id="title" name="title" value="${form.title}" maxlength="255" required>
                    </div>

                    <div class="admin-field-group">
                        <label for="originalTitle">Оригинальное название</label>
                        <input type="text" id="originalTitle" name="originalTitle" value="${form.originalTitle}" maxlength="255">
                    </div>

                    <div class="admin-field-group">
                        <label for="releaseYear">Год релиза *</label>
                        <input type="text"
                               id="releaseYear"
                               name="releaseYear"
                               value="${form.releaseYear}"
                               maxlength="4"
                               inputmode="numeric"
                               pattern="[0-9]{4}"
                               placeholder="XXXX"
                               required>
                    </div>

                    <div class="admin-field-group">
                        <label for="typeId">Тип *</label>
                        <select id="typeId" name="typeId" required>
                            <option value="">Выберите тип</option>
                            <c:forEach var="type" items="${comicTypes}">
                                <option value="${type.id}" ${form.typeId == type.id ? 'selected' : ''}>${type.name}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="admin-field-group">
                        <label for="ageRatingId">Возраст</label>
                        <select id="ageRatingId" name="ageRatingId">
                            <option value="">Не выбран</option>
                            <c:forEach var="age" items="${ageRatings}">
                                <option value="${age.id}" ${form.ageRatingId == age.id ? 'selected' : ''}>${age.name}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="admin-field-group">
                        <label for="comicStatusId">Статус *</label>
                        <select id="comicStatusId" name="comicStatusId" required>
                            <option value="">Выберите статус</option>
                            <c:forEach var="status" items="${comicStatuses}">
                                <option value="${status.id}" ${form.comicStatusId == status.id ? 'selected' : ''}>${status.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                </section>

                <section class="admin-form-card admin-form-card-wide">
                    <h2>Описание</h2>

                    <div class="admin-field-group">
                        <label for="shortDescription">Краткое описание *</label>
                        <textarea id="shortDescription" name="shortDescription" rows="4" maxlength="500" required>${form.shortDescription}</textarea>
                    </div>

                    <div class="admin-field-group">
                        <label for="fullDescription">Описание *</label>
                        <textarea id="fullDescription" name="fullDescription" rows="8" maxlength="2000" required>${form.fullDescription}</textarea>
                    </div>
                </section>

                <section class="admin-form-card">
                    <h2>Жанры</h2>

                    <div class="selected-values-wrap">
                        <div id="selectedGenresChips" class="selected-values-chips"></div>
                        <button type="button" class="btn btn-outline js-open-lookup-modal" data-kind="genre">
                            Выбрать и редактировать
                        </button>
                    </div>
                </section>

                <section class="admin-form-card">
                    <h2>Теги</h2>

                    <div class="selected-values-wrap">
                        <div id="selectedTagsChips" class="selected-values-chips"></div>
                        <button type="button" class="btn btn-outline js-open-lookup-modal" data-kind="tag">
                            Выбрать и редактировать
                        </button>
                    </div>
                </section>

                <section class="admin-form-card admin-form-card-wide">
                    <div class="admin-card-head">
                        <h2>Связанные комиксы</h2>
                        <button type="button" class="btn btn-outline js-open-lookup-modal" data-kind="relationType">
                            Редактировать метки связей
                        </button>
                    </div>

                    <div class="admin-field-group">
                        <label for="relatedComicSearch">Поиск комикса</label>
                        <input type="text"
                               id="relatedComicSearch"
                               placeholder="Введите название комикса"
                               data-search-url="${searchComicUrl}"
                               data-exclude-comic-id="${form.comicId}">
                    </div>

                    <div id="relatedComicSearchResults" class="related-search-results"></div>

                    <div id="relatedComicRelations" class="related-relations">
                        <c:forEach var="relation" items="${form.relationItems}">
                            <div class="relation-item" data-related-comic-id="${relation.relatedComicId}">
                                <div class="relation-item-main">
                                    <div class="relation-item-title">${relation.relatedComicTitle}</div>
                                    <input type="hidden" class="relation-comic-id" value="${relation.relatedComicId}">
                                    <input type="text"
                                           class="relation-type-name manual-trim-input"
                                           value="${relation.relationTypeName}"
                                           list="relationTypeNames"
                                           maxlength="50"
                                           placeholder="Метка связи *">
                                </div>
                                <button type="button" class="btn btn-outline relation-remove-btn js-remove-relation">Удалить</button>
                            </div>
                        </c:forEach>
                    </div>

                    <datalist id="relationTypeNames"></datalist>
                </section>
            </div>

            <div class="admin-comic-form-actions">
                <button type="submit" class="btn">Сохранить</button>
            </div>
        </form>

        <div id="lookupModal" class="modal admin-lookup-modal hidden" aria-modal="true" role="dialog">
            <div class="modal-content admin-lookup-modal-content">
                <button type="button"
                        id="lookupModalCloseBtn"
                        class="admin-lookup-close-btn"
                        aria-label="Закрыть">
                    &times;
                </button>

                <div class="admin-lookup-modal-header">
                    <h3 id="lookupModalTitle">Редактор</h3>
                </div>

                <div class="admin-field-group">
                    <label for="lookupModalSearch">Поиск</label>
                    <input type="text" id="lookupModalSearch" placeholder="Введите название">
                </div>

                <div id="lookupModalRows" class="lookup-modal-rows"></div>

                <div class="admin-lookup-modal-actions">
                    <button type="button" id="lookupModalAddBtn" class="btn btn-outline">Добавить</button>
                    <button type="button" id="lookupModalApplyBtn" class="btn">Готово</button>
                </div>
            </div>
        </div>

        <div id="genreSourceRows" class="hidden">
            <c:forEach var="genre" items="${genres}">
                <div class="lookup-source-row"
                     data-id="${genre.id}"
                     data-name="${genre.name}"
                     data-selected="${form.genreIds != null && form.genreIds.contains(genre.id)}"></div>
            </c:forEach>
        </div>

        <div id="tagSourceRows" class="hidden">
            <c:forEach var="tag" items="${tags}">
                <div class="lookup-source-row"
                     data-id="${tag.id}"
                     data-name="${tag.name}"
                     data-selected="${form.tagIds != null && form.tagIds.contains(tag.id)}"></div>
            </c:forEach>
        </div>

        <div id="relationTypeSourceRows" class="hidden">
            <c:forEach var="relationType" items="${relationTypes}">
                <div class="lookup-source-row"
                     data-id="${relationType.id}"
                     data-name="${relationType.name}"></div>
            </c:forEach>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>

<script src="<c:url value='/script/admin-comic-form.js'/>"></script>
</body>
</html>
