(() => {
    if (window.__adminTranslationEditScriptInitialized) {
        return;
    }
    window.__adminTranslationEditScriptInitialized = true;

    document.addEventListener("DOMContentLoaded", () => {
        const form = document.getElementById("adminTranslationEditForm");
        const pagesEditor = document.getElementById("adminPagesEditor");
        const pagesPayloadInput = document.getElementById("pagesPayload");
        const statusBox = document.getElementById("adminTranslationEditStatus");
        const appendPageBtn = document.getElementById("adminAppendPageBtn");

        const deleteModal = document.getElementById("adminDeleteTranslationModal");
        const openDeleteBtn = document.getElementById("openDeleteTranslationModalBtn");

        if (!form || !pagesEditor || !pagesPayloadInput || !statusBox) {
            return;
        }

        let newPageCounter = 0;

        function showStatus(message) {
            statusBox.textContent = (message || "").trim();
            statusBox.classList.remove("hidden");
        }

        function hideStatus() {
            statusBox.textContent = "";
            statusBox.classList.add("hidden");
        }

        function getCards() {
            return Array.from(pagesEditor.querySelectorAll(".admin-page-card"));
        }

        function refreshPageOrders() {
            getCards().forEach((card, index) => {
                const order = card.querySelector(".js-admin-page-order");
                if (order) {
                    order.textContent = String(index + 1);
                }
            });
        }

        function updatePreviewFromInput(card, input) {
            if (!card || !input || !input.files || !input.files.length) {
                return;
            }

            const img = card.querySelector(".admin-page-card-image");
            const label = card.querySelector(".js-admin-page-file-label");

            if (img) {
                img.src = URL.createObjectURL(input.files[0]);
            }

            if (label) {
                label.textContent = input.files[0].name;
            }
        }

        function buildHiddenFileInput(name) {
            const input = document.createElement("input");
            input.type = "file";
            input.name = name;
            input.accept = "image/*";
            input.dataset.role = "page-file";
            input.className = "hidden";
            return input;
        }

        function buildNewCard(input) {
            const card = document.createElement("article");
            card.className = "admin-page-card admin-page-card-new";
            card.dataset.pageId = "";

            card.innerHTML = `
                <div class="admin-page-card-media">
                    <img class="admin-page-card-image" alt="Новая страница">
                </div>

                <div class="admin-page-card-body">
                    <div class="admin-page-card-title">
                        Страница <span class="js-admin-page-order"></span>
                    </div>

                    <div class="admin-page-card-file js-admin-page-file-label"></div>

                    <div class="admin-page-card-actions">
                        <button type="button" class="btn btn-outline js-page-insert-before">Вставить перед</button>
                        <button type="button" class="btn btn-outline js-page-replace">Заменить</button>
                        <button type="button" class="btn btn-outline js-page-delete">Удалить</button>
                        <button type="button" class="btn btn-outline js-page-insert-after">Вставить после</button>
                        <button type="button" class="btn btn-outline js-page-move-up">Выше</button>
                        <button type="button" class="btn btn-outline js-page-move-down">Ниже</button>
                    </div>

                    <div class="hidden"></div>
                </div>
            `;

            card.querySelector(".hidden").appendChild(input);
            bindFileInput(card, input);
            updatePreviewFromInput(card, input);
            return card;
        }

        function bindFileInput(card, input) {
            input.addEventListener("change", () => {
                if (!input.files || !input.files.length) {
                    return;
                }
                updatePreviewFromInput(card, input);
            });
        }

        function chooseFile(onSelected) {
            const inputName = `pageFile_new_${Date.now()}_${newPageCounter++}`;
            const input = buildHiddenFileInput(inputName);
            form.appendChild(input);

            input.addEventListener("change", () => {
                if (!input.files || !input.files.length) {
                    input.remove();
                    return;
                }
                onSelected(input);
            });

            input.click();
        }

        function insertNewCard(targetCard, insertAfter) {
            chooseFile((input) => {
                const newCard = buildNewCard(input);

                if (!targetCard) {
                    pagesEditor.appendChild(newCard);
                } else if (insertAfter) {
                    targetCard.insertAdjacentElement("afterend", newCard);
                } else {
                    targetCard.insertAdjacentElement("beforebegin", newCard);
                }

                refreshPageOrders();
                hideStatus();
            });
        }

        function replaceCardFile(card) {
            const existingInput = card.querySelector('input[data-role="page-file"]');
            if (!existingInput) {
                return;
            }
            existingInput.click();
        }

        function deleteCard(card) {
            if (getCards().length <= 1) {
                showStatus("В переводе должна остаться хотя бы одна страница.");
                return;
            }

            const input = card.querySelector('input[data-role="page-file"]');
            if (input && !card.dataset.pageId) {
                input.remove();
            }

            card.remove();
            refreshPageOrders();
            hideStatus();
        }

        function moveCard(card, direction) {
            if (direction === "up" && card.previousElementSibling) {
                card.parentNode.insertBefore(card, card.previousElementSibling);
            }

            if (direction === "down" && card.nextElementSibling) {
                card.parentNode.insertBefore(card.nextElementSibling, card);
            }

            refreshPageOrders();
            hideStatus();
        }

        pagesEditor.addEventListener("click", (event) => {
            const card = event.target.closest(".admin-page-card");
            if (!card) {
                return;
            }

            if (event.target.closest(".js-page-insert-before")) {
                event.preventDefault();
                insertNewCard(card, false);
                return;
            }

            if (event.target.closest(".js-page-insert-after")) {
                event.preventDefault();
                insertNewCard(card, true);
                return;
            }

            if (event.target.closest(".js-page-replace")) {
                event.preventDefault();
                replaceCardFile(card);
                return;
            }

            if (event.target.closest(".js-page-delete")) {
                event.preventDefault();
                deleteCard(card);
                return;
            }

            if (event.target.closest(".js-page-move-up")) {
                event.preventDefault();
                moveCard(card, "up");
                return;
            }

            if (event.target.closest(".js-page-move-down")) {
                event.preventDefault();
                moveCard(card, "down");
            }
        });

        getCards().forEach((card) => {
            const fileInput = card.querySelector('input[data-role="page-file"]');
            if (fileInput) {
                bindFileInput(card, fileInput);
            }
        });

        appendPageBtn?.addEventListener("click", () => {
            insertNewCard(null, true);
        });

        form.addEventListener("submit", (event) => {
            hideStatus();

            const cards = getCards();
            if (!cards.length) {
                event.preventDefault();
                showStatus("В переводе должна остаться хотя бы одна страница.");
                return;
            }

            const payload = [];

            for (let index = 0; index < cards.length; index++) {
                const card = cards[index];
                const pageIdValue = card.dataset.pageId ? Number(card.dataset.pageId) : null;
                const fileInput = card.querySelector('input[data-role="page-file"]');
                const hasFile = Boolean(fileInput && fileInput.files && fileInput.files.length > 0);
                const fileField = hasFile ? fileInput.name : "";

                if (!pageIdValue && !hasFile) {
                    event.preventDefault();
                    showStatus("Для новой страницы нужно выбрать изображение.");
                    return;
                }

                payload.push({
                    pageId: pageIdValue,
                    fileField,
                    order: index
                });
            }

            pagesPayloadInput.value = JSON.stringify(payload);
        });

        function openDeleteModal() {
            if (!deleteModal) {
                return;
            }

            deleteModal.classList.remove("hidden");
            deleteModal.classList.add("visible");
            deleteModal.style.display = "flex";
            document.body.style.overflow = "hidden";
        }

        function closeDeleteModal() {
            if (!deleteModal) {
                return;
            }

            deleteModal.classList.add("hidden");
            deleteModal.classList.remove("visible");
            deleteModal.style.display = "none";
            document.body.style.overflow = "";
        }

        openDeleteBtn?.addEventListener("click", openDeleteModal);

        document.addEventListener("click", (event) => {
            if (event.target.closest(".js-close-admin-delete-modal")) {
                event.preventDefault();
                closeDeleteModal();
                return;
            }

            if (event.target === deleteModal) {
                closeDeleteModal();
            }
        });

        window.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && deleteModal && !deleteModal.classList.contains("hidden")) {
                closeDeleteModal();
            }
        });

        refreshPageOrders();
    });
})();
