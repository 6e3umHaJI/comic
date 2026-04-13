<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<html data-theme="light">
<head>
    <title>${comic.title}</title>
    <jsp:include page="/WEB-INF/views/dependencies.jsp" />
    <jsp:include page="/WEB-INF/views/theme-init.jsp"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="<c:url value='/style/common.css'/>">
    <link rel="stylesheet" href="<c:url value='/style/comic-page.css'/>">
    <style>.hidden{display:none;}</style>
</head>

<body data-authenticated="${pageContext.request.userPrincipal != null}">
<div class="wrapper">
    <jsp:include page="/WEB-INF/views/header.jsp"/>

    <main class="main container comic-page">
        <div class="comic-left">
            <img src="${pageContext.request.contextPath}/assets/covers/${comic.cover}"
                 alt="${comic.title}" class="comic-cover">

            <div class="comic-meta">
                <c:if test="${not empty comic.originalTitle}">
                    <p><b>Оригинальное название:</b> ${comic.originalTitle}</p>
                </c:if>
                <c:if test="${not empty comic.type.name}">
                    <p><b>Тип:</b> ${comic.type.name}</p>
                </c:if>
                <c:if test="${not empty comic.releaseYear}">
                    <p><b>Год:</b> ${comic.releaseYear}</p>
                </c:if>
                <c:if test="${not empty comic.ageRating.name}">
                    <p><b>Возраст:</b> ${comic.ageRating.name}</p>
                </c:if>
                <c:if test="${not empty comic.comicStatus.name}">
                    <p><b>Статус:</b> ${comic.comicStatus.name}</p>
                </c:if>
                <c:if test="${not empty approvedLangStats}">
                    <p>
                        <b>Языки перевода:</b>
                        <c:forEach var="row" items="${approvedLangStats}" varStatus="st">
                            ${row[0]} (${row[1]})<c:if test="${!st.last}">, </c:if>
                        </c:forEach>
                    </p>
                </c:if>

                <c:if test="${not empty comic.genres}">
                    <p><b>Жанры:</b>
                    <c:forEach var="g" items="${comic.genres}" varStatus="st">
                        ${g.name}<c:if test="${!st.last}">, </c:if>
                    </c:forEach>
                </p>
                </c:if>
                <c:if test="${not empty comic.tags}">
                <p><b>Теги:</b>
                    <c:forEach var="t" items="${comic.tags}" varStatus="st">
                        #${t.name}<c:if test="${!st.last}">, </c:if>
                    </c:forEach>
                </p>
                </c:if>
                <button type="button"
                        class="btn btn-outline collection-action-btn js-collection-toggle ${inCollections ? 'is-active' : ''}"
                        data-comic-id="${comic.id}"
                        data-authenticated="${isLogged}">
                    <span class="btn-icon"
                          style="-webkit-mask-image:url('<c:url value="/assets/icons/collection.svg"/>'); mask-image:url('<c:url value="/assets/icons/collection.svg"/>');"></span>
                    <span class="js-collection-toggle-text">
                        ${inCollections ? 'В коллекции' : 'Добавить в коллекцию'}
                    </span>
                </button>
                <button class="btn add-chapter-btn">Добавить главу</button>
            </div>
        </div>

        <div class="comic-right">
            <div class="comic-title">${comic.title}</div>
            <div class="comic-rating" title="Оценить">★ <fmt:formatNumber value="${comic.avgRating}" pattern="0.00"/></div>

            <div class="tab-buttons">
                <button type="button" data-tab="description"
                        class="${tab == 'description' ? 'active' : ''}">Описание</button>
                <button type="button" data-tab="chapters"
                        class="${tab == 'chapters' ? 'active' : ''}">Главы</button>
            </div>

            <div id="tabContent" class="tab-content">
                <jsp:include page="comic-tab-content.jsp" />
            </div>

            <div id="comicPage"
                 data-comic-id="${comic.id}"
                 data-context-path="${pageContext.request.contextPath}"></div>
        </div>
    </main>

    <c:if test="${isLogged}">
    <div id="ratingModal" class="modal">
      <div class="modal-content">
        <span class="close-button" id="closeModal">&times;</span>
        <h3>Оцените комикс</h3>
        <div class="stars" id="ratingStars">
          <c:forEach var="i" begin="1" end="5">
            <span class="star" data-value="${i}">&#9733;</span>
          </c:forEach>
        </div>
        <button id="removeRating" class="btn btn-outline">Удалить оценку</button>
      </div>
    </div>
    </c:if>

    <div id="translationsModal" class="modal hidden" aria-modal="true" role="dialog">
      <div class="modal-content tr-modal">
        <button class="close-button tr-close" id="trClose" aria-label="Закрыть">&times;</button>
        <h3 id="trTitle" class="tr-title">Переводы главы</h3>
        <div class="tr-controls">
          <label for="trLangSelect">Язык:</label>
          <select id="trLangSelect" class="tr-select"></select>
        </div>
        <ul id="trList" class="translation-list"></ul>
        <div class="tr-footer">
          <button id="trMore" class="btn">Показать ещё</button>
        </div>
      </div>
    </div>

    <script>
    document.addEventListener("DOMContentLoaded", () => {
      const tabButtons = document.querySelectorAll(".tab-buttons button");
      const tabContent = document.getElementById("tabContent");

      const holder = document.getElementById("comicPage");
      const comicId = holder.dataset.comicId;
      const ctx = holder.dataset.contextPath || "";

      let relatedPage = 0;
      function fetchRelated() {
        const container = tabContent.querySelector("#relatedContainer");
        const prevBtn = tabContent.querySelector("#relatedPrev");
        const nextBtn = tabContent.querySelector("#relatedNext");
        const info = tabContent.querySelector("#relatedPageInfo");
        const block = tabContent.querySelector("#relatedBlock");
        if (!block || !container) return;

        const total = parseInt(block.dataset.total || "0", 10);
        const size = parseInt(block.dataset.size || "5", 10);
        const totalPages = Math.max(1, Math.ceil(total / size));
        relatedPage = Math.min(Math.max(0, relatedPage), totalPages - 1);

        const url = ctx + "/comics/" + comicId + "/related?page=" + relatedPage;
        fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
          .then(res => { if (!res.ok) throw new Error('HTTP ' + res.status); return res.text(); })
          .then(html => {
            container.innerHTML = html;
            if (info) info.textContent = (totalPages > 1) ? ((relatedPage + 1) + " / " + totalPages) : "";
            if (prevBtn) prevBtn.disabled = (relatedPage === 0);
            if (nextBtn) nextBtn.disabled = (relatedPage >= totalPages - 1);
          })
          .catch(e => console.error("Ошибка при загрузке связанных:", e));
      }

      let chaptersPage = 0;
      let chaptersTotal = 0;
      let chaptersSize = 20;
      let chaptersDir = "desc";
      let chaptersQ = "";


      function initChapters() {
        const block = tabContent.querySelector("#chaptersBlock");
        if (!block) return;

        chaptersTotal = 0;
        chaptersSize  = parseInt(block.dataset.size || "20", 10);
        chaptersDir   = block.dataset.dir || "desc";
        chaptersQ     = "";

        const search = tabContent.querySelector("#chapterSearch");
        const dirBtn = tabContent.querySelector("#chapterDirToggle");

        if (search) {
          const debounced = debounce(() => {
            chaptersQ = search.value || "";
            chaptersPage = 0;
            loadChapters();
          }, 300);
          search.addEventListener("input", debounced);
        }

        if (dirBtn) {
          dirBtn.addEventListener("click", () => {
            chaptersDir = (chaptersDir === "desc") ? "asc" : "desc";
            updateDirButtonUI();
            chaptersPage = 0;
            loadChapters();
          });
          updateDirButtonUI();
        }

        chaptersPage = 0;
        loadChapters();
      }

      function loadChapters() {
        const container = tabContent.querySelector("#chaptersContainer");
        if (!container) return;

        const params = new URLSearchParams({
          page: String(chaptersPage),
          size: String(chaptersSize),
          dir: chaptersDir,
          q: chaptersQ || ""
        });
        const url = ctx + "/comics/" + comicId + "/chapters?" + params.toString();

        fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
          .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.text(); })
          .then(html => {
            const tmp = document.createElement("div");
            tmp.innerHTML = html;
            const chunk = tmp.querySelector("#chaptersChunk");
            if (!chunk) {
              container.innerHTML = "<p>Ошибка загрузки глав.</p>";
              tabContent.querySelectorAll(".chaptersPager").forEach(p => p.style.display = "none");
              return;
            }

            chaptersTotal = parseInt(chunk.dataset.total || "0", 10);
            chaptersSize  = parseInt(chunk.dataset.size || String(chaptersSize), 10);

            container.innerHTML = chunk.innerHTML;
          })
          .catch(e => {
            console.error("Ошибка при загрузке глав:", e);
            tabContent.querySelectorAll(".chaptersPager").forEach(p => p.style.display = "none");
          });
      }

      function updateDirButtonUI() {
        const dirBtn = tabContent.querySelector("#chapterDirToggle");
        if (!dirBtn) return;
        if (chaptersDir === "desc") {
          dirBtn.textContent = "↑";
          dirBtn.title = "Сначала новые";
        } else {
          dirBtn.textContent = "↓";
          dirBtn.title = "Сначала старые";
        }
      }

      const trModal = document.getElementById("translationsModal");
      const trClose = document.getElementById("trClose");
      const trTitle = document.getElementById("trTitle");
      const trLangSelect = document.getElementById("trLangSelect");
      const trList = document.getElementById("trList");
      const trMore = document.getElementById("trMore");

      let trChapterId = null;
      let trPage = 0;
      let trSize = 10;
      let trTotal = 0;

      function fmtIsoToRu(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        if (!Number.isNaN(d.getTime())) {
          const dd = String(d.getDate()).padStart(2,'0');
          const mm = String(d.getMonth()+1).padStart(2,'0');
          const yy = String(d.getFullYear()).slice(-2);
          const hh = String(d.getHours()).padStart(2,'0');
          const mi = String(d.getMinutes()).padStart(2,'0');
          return `${dd}.${mm}.${yy} ${hh}.${mi}`;
        }
        const m = String(iso).match(/^(\d{4})-(\d{2})-(\d{2})T?(\d{2}):(\d{2})/);
        if (m) return `${m[3]}.${m[2]}.${m[1].slice(-2)} ${m[4]}.${m[5]}`;
        return iso;
      }

      function openTranslationsModal(chapterId, langsCsv, chapterLabel) {
        trChapterId = chapterId;
        trPage = 0;
        trTotal = 0;
        trList.innerHTML = "";
        trTitle.textContent = "Переводы главы " + (chapterLabel || "");

        trLangSelect.innerHTML = "";
        const optAll = document.createElement("option");
        optAll.value = "";
        optAll.textContent = "Все языки";
        trLangSelect.appendChild(optAll);

        (langsCsv || "")
          .split(",")
          .map(s => s.trim())
          .filter(Boolean)
          .forEach(l => {
            const o = document.createElement("option");
            o.value = l;
            o.textContent = l;
            trLangSelect.appendChild(o);
          });

        trModal.classList.remove("hidden");
        trModal.style.display = "flex";
        document.body.style.overflow = "hidden";

        loadTranslations(false);
      }

      function closeTranslationsModal() {
        trModal.classList.add("hidden");
        trModal.style.display = "none";
        document.body.style.overflow = "";
      }

      function loadTranslations(append) {
        if (!trChapterId) return;
        const lang = trLangSelect.value || "";
        const params = new URLSearchParams({
          page: String(trPage),
          size: String(trSize),
          lang: lang
        });
        const url = ctx + "/comics/chapters/" + trChapterId + "/translations?" + params.toString();

        fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
          .then(r => { if (!r.ok) throw new Error("HTTP " + r.status); return r.text(); })
          .then(html => {
            const tmp = document.createElement("div");
            tmp.innerHTML = html;
            const chunk = tmp.querySelector("#translationsChunk");
            if (!chunk) {
              if (!append) trList.innerHTML = "<li class='translation-empty'><em>Не удалось загрузить переводы.</em></li>";
              trMore.style.display = "none";
              return;
            }

            const total = parseInt(chunk.dataset.total || "0", 10);
            if (trPage === 0) trTotal = total;

            const listNode = chunk.querySelector(".translation-list");
            if (!append) trList.innerHTML = "";
            if (listNode) {
              Array.from(listNode.children).forEach(li => trList.appendChild(li));
            }

            const loaded = trList.querySelectorAll(".translation-item").length;
            trMore.style.display = (loaded < trTotal) ? "inline-flex" : "none";
          })
          .catch(e => {
            console.error("Ошибка при загрузке переводов:", e);
            if (!append) trList.innerHTML = "<li class='translation-empty'><em>Ошибка загрузки.</em></li>";
            trMore.style.display = "none";
          });
      }

      if (trClose) trClose.addEventListener("click", closeTranslationsModal);
      window.addEventListener("click", (e) => { if (e.target === trModal) closeTranslationsModal(); });
      window.addEventListener("keydown", (e) => { if (e.key === "Escape" && trModal && trModal.style.display === "flex") closeTranslationsModal(); });
      if (trLangSelect) trLangSelect.addEventListener("change", () => { trPage = 0; loadTranslations(false); });
      if (trMore) trMore.addEventListener("click", () => { trPage++; loadTranslations(true); });

      tabContent.addEventListener("click", (e) => {
        if (e.target && e.target.id === "relatedPrev") { e.preventDefault(); relatedPage--; fetchRelated(); return; }
        if (e.target && e.target.id === "relatedNext") { e.preventDefault(); relatedPage++; fetchRelated(); return; }

        const pageLink = e.target.closest('.chaptersPagination a[data-page]');
        const dp = pageLink?.dataset.page;
        if (typeof dp !== 'undefined' && dp !== null && dp !== '') {
          e.preventDefault();
          const p = parseInt(dp, 10);
          if (!Number.isNaN(p)) {
            chaptersPage = Math.max(0, p);
            loadChapters();
          }
          return;
        }

        const chapterRow = e.target.closest(".chapter-group");
        if (chapterRow && chapterRow.dataset.chapterId) {
          const chId  = chapterRow.dataset.chapterId;
          const label = chapterRow.querySelector(".chapter-header b")?.textContent || "";
          const langs = chapterRow.dataset.langs || "";
          openTranslationsModal(chId, langs, label.replace("Глава ", "").trim());
          return;
        }

        if (e.target && e.target.id === "toggleDesc") {
          const shortEl = tabContent.querySelector("#descShort");
          const fullEl  = tabContent.querySelector("#descFull");
          if (shortEl && fullEl) {
            const isHidden = fullEl.classList.contains("hidden");
            if (isHidden) { fullEl.classList.remove("hidden"); shortEl.classList.add("hidden"); e.target.textContent = "Скрыть"; }
            else { fullEl.classList.add("hidden"); shortEl.classList.remove("hidden"); e.target.textContent = "Показать полностью"; }
          }
        }
      });

      function onTabRendered(tab) {
        if (tab === "description") {
          const relatedBlock = tabContent.querySelector("#relatedBlock");
          if (relatedBlock) {
            relatedPage = 0;
            fetchRelated();
          }
        }
        if (tab === "chapters") {
          initChapters();
        }
      }

      tabButtons.forEach(btn => {
        btn.addEventListener("click", () => {
          const tab = btn.dataset.tab;
          tabButtons.forEach(b => b.classList.remove("active"));
          btn.classList.add("active");
          const url = ctx + "/comics/" + comicId + "?tab=" + encodeURIComponent(tab);
          fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
            .then(res => { if (!res.ok) throw new Error('HTTP ' + res.status); return res.text(); })
            .then(html => {
              tabContent.innerHTML = html;
              onTabRendered(tab);
            })
            .catch(err => {
              console.error(err);
              tabContent.innerHTML = "<p>Ошибка загрузки вкладки.</p>";
            });
        });
      });

      const initialTab = document.querySelector(".tab-buttons button.active")?.dataset.tab || "description";
      onTabRendered(initialTab);

      const modal = document.getElementById("ratingModal");
      const openBtn = document.querySelector(".comic-rating");
      if (openBtn) {
        if (!modal) {
          openBtn.addEventListener("click", () => {
            if (window.openAuthRequiredModal) {
              window.openAuthRequiredModal();
            }
          });
        } else {
          const closeBtn = document.getElementById("closeModal");
          const stars = document.querySelectorAll(".star");
          const removeBtn = document.getElementById("removeRating");
          openBtn.addEventListener("click", () => modal.classList.add("visible"));
          closeBtn.addEventListener("click", () => modal.classList.remove("visible"));
          window.addEventListener("click", (e) => { if (e.target === modal) modal.classList.remove("visible"); });
          stars.forEach(star => {
            star.addEventListener('mouseover', () => {
              stars.forEach(s => s.classList.toggle('hover', +s.dataset.value <= +star.dataset.value));
            });
            star.addEventListener('click', () => {
              const val = star.dataset.value;
              fetch(ctx + "/api/ratings/" + comicId + "?value=" + val, { method: 'POST' })
                .then(r => r.status === 401 ? alert("Авторизуйтесь для оценки") : location.reload());
            });
          });
          if (removeBtn) {
            removeBtn.addEventListener("click", () => {
              fetch(ctx + "/api/ratings/" + comicId, { method: "DELETE" })
                .then(res => res.ok && location.reload());
            });
          }
        }
      }

      function debounce(fn, delay) {
        let t; return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
      }
    });
    </script>

    <jsp:include page="/WEB-INF/views/footer.jsp"/>
</div>
<jsp:include page="/WEB-INF/views/collections/collection-global-modal.jsp"/>
<script src="<c:url value='/script/collection-modal.js'/>"></script>
</body>
</html>