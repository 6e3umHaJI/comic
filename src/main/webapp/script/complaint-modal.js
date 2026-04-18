(() => {
    if (window.__complaintModalScriptInitialized) {
        return;
    }
    window.__complaintModalScriptInitialized = true;

    function getAuthRequiredModal() {
        return document.getElementById('authRequiredModal');
    }

    function openAuthRequiredModal() {
        const modal = getAuthRequiredModal();
        if (modal) {
            modal.classList.add('visible');
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const modal = document.getElementById('complaintModal');
        const form = document.getElementById('complaintForm');
        const closeBtn = document.getElementById('complaintCloseBtn');
        const statusBox = document.getElementById('complaintFormStatus');
        const targetIdInput = document.getElementById('complaintTargetId');
        const typeSelect = document.getElementById('complaintTypeId');
        const descriptionInput = document.getElementById('complaintDescription');

        if (!modal || !form || !statusBox || !targetIdInput || !typeSelect || !descriptionInput) {
            return;
        }

        function resetStatus() {
            statusBox.textContent = '';
            statusBox.classList.add('hidden');
            statusBox.classList.remove('is-error', 'is-success');
        }

        function showStatus(message, isSuccess) {
            statusBox.textContent = message;
            statusBox.classList.remove('hidden', 'is-error', 'is-success');
            statusBox.classList.add(isSuccess ? 'is-success' : 'is-error');
        }

        function openModal(button) {
            const isAuthenticated = button.dataset.authenticated === 'true';
            if (!isAuthenticated) {
                openAuthRequiredModal();
                return;
            }

            form.reset();
            targetIdInput.value = button.dataset.targetId || '';
            resetStatus();

            if (typeSelect.options.length > 0) {
                typeSelect.selectedIndex = 0;
            }

            modal.classList.remove('hidden');
            modal.classList.add('visible');
            modal.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        }

        function closeModal() {
            modal.classList.add('hidden');
            modal.classList.remove('visible');
            modal.style.display = 'none';
            document.body.style.overflow = '';
            resetStatus();
        }

        document.addEventListener('click', (event) => {
            const openBtn = event.target.closest('.js-open-complaint-modal');
            if (openBtn) {
                event.preventDefault();
                openModal(openBtn);
                return;
            }

            if (event.target === modal) {
                closeModal();
            }
        });

        if (closeBtn) {
            closeBtn.addEventListener('click', closeModal);
        }

        window.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !modal.classList.contains('hidden')) {
                closeModal();
            }
        });

        form.addEventListener('submit', (event) => {
            event.preventDefault();
            resetStatus();

            const description = (descriptionInput.value || '').trim();
            if (!typeSelect.value) {
                showStatus('Выберите тип жалобы.', false);
                return;
            }

            if (!description) {
                showStatus('Описание жалобы обязательно к заполнению.', false);
                return;
            }

            fetch(form.action, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams({
                    targetId: targetIdInput.value,
                    complaintTypeId: typeSelect.value,
                    description
                }).toString()
            })
                .then(async (response) => {
                    const payload = await response.json();

                    if (response.status === 401) {
                        openAuthRequiredModal();
                        throw new Error(payload?.message || 'Авторизуйтесь, чтобы отправить жалобу.');
                    }

                    if (!payload?.success) {
                        throw new Error(payload?.message || 'Не удалось отправить жалобу.');
                    }

                    showStatus(payload.message || 'Жалоба успешно отправлена.', true);
                    form.reset();
                    targetIdInput.value = targetIdInput.value;

                    if (typeSelect.options.length > 0) {
                        typeSelect.selectedIndex = 0;
                    }
                })
                .catch((error) => {
                    showStatus(error.message || 'Не удалось отправить жалобу.', false);
                });
        });
    });
})();