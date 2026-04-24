(() => {
    if (window.__comicAdminTranslationActionsInitialized) {
        return;
    }
    window.__comicAdminTranslationActionsInitialized = true;

    document.addEventListener("DOMContentLoaded", () => {
        const translationModal = document.getElementById("adminDeleteTranslationModal");
        const translationModalText = document.getElementById("adminDeleteTranslationModalText");
        const translationConfirmForm = document.getElementById("adminDeleteTranslationForm");

        const comicModal = document.getElementById("adminDeleteComicModal");
        const comicModalText = document.getElementById("adminDeleteComicModalText");
        const comicConfirmForm = document.getElementById("adminDeleteComicForm");

        function openModal(modal) {
            if (!modal) {
                return;
            }

            modal.classList.remove("hidden");
            modal.classList.add("visible");
            modal.style.display = "flex";
            document.body.style.overflow = "hidden";
        }

        function closeModal(modal) {
            if (!modal) {
                return;
            }

            modal.classList.add("hidden");
            modal.classList.remove("visible");
            modal.style.display = "none";
            document.body.style.overflow = "";
        }

        function openTranslationModal(form, button) {
            if (!translationModal || !translationModalText || !translationConfirmForm) {
                return;
            }

            const translationTitle = (button.dataset.translationTitle || "").trim();
            const chapterNumber = (button.dataset.chapterNumber || "").trim();
            const languageName = (button.dataset.languageName || "").trim();

            const titlePart = translationTitle ? `«${translationTitle}»` : "этот перевод";
            const chapterPart = chapterNumber ? ` главы ${chapterNumber}` : "";
            const languagePart = languageName ? ` (${languageName})` : "";

            translationModalText.textContent =
                `Удалить ${titlePart}${chapterPart}${languagePart}? Если это последний перевод главы, сама глава тоже будет удалена.`;

            translationConfirmForm.action = form.action;
            openModal(translationModal);
        }

        function openComicModal(form, button) {
            if (!comicModal || !comicModalText || !comicConfirmForm) {
                return;
            }

            const comicTitle = (button.dataset.comicTitle || "").trim();
            const titlePart = comicTitle ? `«${comicTitle}»` : "этот комикс";

            comicModalText.textContent =
                `Удалить ${titlePart} вместе со всеми переводами, предложенными главами, подписками, связями и связанными данными?`;

            comicConfirmForm.action = form.action;
            openModal(comicModal);
        }

        document.addEventListener("click", (event) => {
            const translationDeleteBtn = event.target.closest(".js-admin-delete-translation");
            if (translationDeleteBtn) {
                event.preventDefault();
                const form = translationDeleteBtn.closest("form");
                if (form) {
                    openTranslationModal(form, translationDeleteBtn);
                }
                return;
            }

            const comicDeleteBtn = event.target.closest(".js-open-admin-delete-comic-modal");
            if (comicDeleteBtn) {
                event.preventDefault();
                const form = comicDeleteBtn.closest("form");
                if (form) {
                    openComicModal(form, comicDeleteBtn);
                }
                return;
            }

            if (event.target.closest(".js-close-admin-delete-translation-modal")) {
                event.preventDefault();
                closeModal(translationModal);
                return;
            }

            if (event.target.closest(".js-close-admin-delete-comic-modal")) {
                event.preventDefault();
                closeModal(comicModal);
                return;
            }

            if (event.target === translationModal) {
                closeModal(translationModal);
                return;
            }

            if (event.target === comicModal) {
                closeModal(comicModal);
            }
        });

        window.addEventListener("keydown", (event) => {
            if (event.key !== "Escape") {
                return;
            }

            if (translationModal && !translationModal.classList.contains("hidden")) {
                closeModal(translationModal);
                return;
            }

            if (comicModal && !comicModal.classList.contains("hidden")) {
                closeModal(comicModal);
            }
        });
    });
})();