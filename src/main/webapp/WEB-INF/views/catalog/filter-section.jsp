<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<meta charset="UTF-8">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class="filters-wrapper">

    <div class="filter-group">
      <div class="filter-label">Тип:</div>
      <div class="filter-options">
        <c:forEach items="${types}" var="t">
          <label class="checkbox-custom">
            <input type="checkbox" name="selectedTypes" value="${t.name}"
                   <c:if test="${fn:contains(searchCriteria.selectedTypes, t.name)}">checked</c:if> />
            <span>${t.name}</span>
          </label>
        </c:forEach>
      </div>
    </div>

    <div class="filter-group">
      <div class="filter-label">Статус тайтла:</div>
      <div class="filter-options">
        <c:forEach items="${statuses}" var="s">
          <label class="checkbox-custom">
            <input type="checkbox" name="selectedComicStatuses" value="${s.name}"
                   <c:if test="${fn:contains(searchCriteria.selectedComicStatuses, s.name)}">checked</c:if> />
            <span>${s.name}</span>
          </label>
        </c:forEach>
      </div>
    </div>

    <div class="filter-group">
      <div class="filter-label">Статус перевода:</div>
      <div class="filter-options">
        <c:forEach items="${translationStatuses}" var="tr">
          <label class="checkbox-custom">
            <input type="checkbox" name="selectedTranslationStatuses" value="${tr.name}"
                   <c:if test="${fn:contains(searchCriteria.selectedTranslationStatuses, tr.name)}">checked</c:if> />
            <span>${tr.name}</span>
          </label>
        </c:forEach>
      </div>
    </div>

    <div class="filter-group">
      <div class="filter-label">Возрастной рейтинг:</div>
      <div class="filter-options">
        <c:forEach items="${ageRatings}" var="ar">
          <label class="checkbox-custom">
            <input type="checkbox" name="selectedAgeRatings" value="${ar.name}"
                   <c:if test="${fn:contains(searchCriteria.selectedAgeRatings, ar.name)}">checked</c:if> />
            <span>${ar.name}</span>
          </label>
        </c:forEach>
      </div>
    </div>

    <div class="filter-group">
      <div class="filter-label">Средний рейтинг:</div>
      <div class="filter-range">
        <input type="number" name="avgRatingFrom" step="0.1" min="0" max="5" placeholder="От"
               value="${searchCriteria.avgRatingFrom}" oninput="if(this.value>5)this.value=5;if(this.value<0)this.value=0">
        <span>–</span>
        <input type="number" name="avgRatingTo" step="0.1" min="0" max="5" placeholder="До"
               value="${searchCriteria.avgRatingTo}" oninput="if(this.value>5)this.value=5;if(this.value<0)this.value=0">
      </div>
    </div>

    <div class="filter-group">
      <div class="filter-label">Дата релиза:</div>
      <div class="filter-range">
        <input type="date" name="releaseYearFrom" value="${searchCriteria.releaseYearFrom}">
        <span>–</span>
        <input type="date" name="releaseYearTo" value="${searchCriteria.releaseYearTo}">
      </div>
    </div>

    <div class="filter-group">
      <div class="filter-label">Дата обновления:</div>
      <div class="filter-range">
        <input type="date" name="updatedFrom" value="${searchCriteria.updatedFrom}">
        <span>–</span>
        <input type="date" name="updatedTo" value="${searchCriteria.updatedTo}">
      </div>
    </div>

  <div class="filter-group collapsible">
    <div class="filter-label">Жанры:</div>
    <input type="text" class="filter-search" placeholder="Поиск по жанрам...">
    <div class="filter-options scrollable">
      <c:forEach items="${genres}" var="g">
        <label class="checkbox-custom">
          <input type="checkbox" name="selectedGenres" value="${g.name}"
                 <c:if test="${fn:contains(searchCriteria.selectedGenres, g.name)}">checked</c:if> />
          <span>${g.name}</span>
        </label>
      </c:forEach>
    </div>
    <label class="switch-toggle">
      <input type="checkbox" name="strictGenreMatch"
             <c:if test="${searchCriteria.strictGenreMatch}">checked</c:if>>
      <span class="slider"></span> Строгое совпадение
    </label>
  </div>

  <div class="filter-group collapsible">
    <div class="filter-label">Теги:</div>
    <input type="text" class="filter-search" placeholder="Поиск по тегам...">
    <div class="filter-options scrollable">
      <c:forEach items="${tags}" var="t">
        <label class="checkbox-custom">
          <input type="checkbox" name="selectedTags" value="${t.name}"
                 <c:if test="${fn:contains(searchCriteria.selectedTags, t.name)}">checked</c:if> />
          <span>${t.name}</span>
        </label>
      </c:forEach>
    </div>
    <label class="switch-toggle">
      <input type="checkbox" name="strictTagMatch"
             <c:if test="${searchCriteria.strictTagMatch}">checked</c:if>>
      <span class="slider"></span> Строгое совпадение
    </label>
  </div>

</div>