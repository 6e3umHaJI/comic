(() => {
    if (window.__comicAddTranslationScriptInitialized) {
        return;
    }
    window.__comicAddTranslationScriptInitialized = true;

    document.addEventListener("DOMContentLoaded", () => {
        const addTranslationBtn = document.querySelector(".add-chapter-btn");
        const addTranslationStatus = document.getElementById("addChapterStatus");
        const holder = document.getElementById("comicPage");
        const ctx = holder?.dataset.contextPath || "";

        if (!addTranslationBtn || !addTranslationStatus) {
            return;
        }

        function showStatus(message) {
            addTranslationStatus.textContent = (message || "Не удалось открыть форму добавления перевода.").trim();
            addTranslationStatus.style.display = "block";
        }

        function hideStatus() {
            addTranslationStatus.textContent = "";
            addTranslationStatus.style.display = "none";
        }

        addTranslationBtn.addEventListener("click", async (event) => {
            event.preventDefault();
            hideStatus();

            const isAuthenticated = addTranslationBtn.dataset.authenticated === "true";
            if (!isAuthenticated) {
                if (window.openAuthRequiredModal) {
                    window.openAuthRequiredModal();
                } else {
                    window.location.assign(ctx + "/auth/login");
                }
                return;
            }

            try {
                const response = await fetch(addTranslationBtn.href, {
                    headers: {
                        "X-Requested-With": "XMLHttpRequest"
                    }
                });

                const message = (await response.text()).trim();

                if (response.status === 401 || message === "AUTH_REQUIRED") {
                    if (window.openAuthRequiredModal) {
                        window.openAuthRequiredModal();
                    } else {
                        window.location.assign(ctx + "/auth/login");
                    }
                    return;
                }

                if (response.ok) {
                    window.location.assign(addTranslationBtn.href);
                    return;
                }

                showStatus(message || "Не удалось открыть форму добавления перевода.");
            } catch (error) {
                console.error("Ошибка открытия формы добавления перевода:", error);
                showStatus("Не удалось открыть форму добавления перевода.");
            }
        });
    });
})();
