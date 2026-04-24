(() => {
    if (window.__comicAdminTranslationActionsInitialized) {
        return;
    }
    window.__comicAdminTranslationActionsInitialized = true;

    document.addEventListener("DOMContentLoaded", () => {
        const modal = document.getElementById("adminDeleteTranslationModal");
        const modalText = document.getElementById("adminDeleteTranslationModalText");
        const confirmForm = document.getElementById("adminDeleteTranslationForm");

        if (!modal || !modalText || !confirmForm) {
            return;
        }

        function openModal(form, button) {
            const translationTitle = (button.dataset.translationTitle || "").trim();
            const chapterNumber = (button.dataset.chapterNumber || "").trim();
            const languageName = (button.dataset.languageName || "").trim();

            const titlePart = translationTitle ? `«${translationTitle}»` : "этот перевод";
            const chapterPart = chapterNumber ? ` главы ${chapterNumber}` : "";
            const languagePart = languageName ? ` (${languageName})` : "";

            modalText.textContent =
                `Удалить ${titlePart}${chapterPart}${languagePart}? Если это последний перевод главы, сама глава тоже будет удалена.`;

            confirmForm.action = form.action;

            modal.classList.remove("hidden");
            modal.classList.add("visible");
            modal.style.display = "flex";
            document.body.style.overflow = "hidden";
        }

        function closeModal() {
            modal.classList.add("hidden");
            modal.classList.remove("visible");
            modal.style.display = "none";
            document.body.style.overflow = "";
        }

        document.addEventListener("click", (event) => {
            const deleteBtn = event.target.closest(".js-admin-delete-translation");
            if (deleteBtn) {
                event.preventDefault();
                const form = deleteBtn.closest("form");
                if (form) {
                    openModal(form, deleteBtn);
                }
                return;
            }

            if (event.target.closest(".js-close-admin-delete-translation-modal")) {
                event.preventDefault();
                closeModal();
                return;
            }

            if (event.target === modal) {
                closeModal();
            }
        });

        window.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !modal.classList.contains("hidden")) {
                closeModal();
            }
        });
    });
})();
