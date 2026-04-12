(function () {
    const modal = document.getElementById('authRequiredModal');
    if (!modal) return;

    const closeBtn = document.getElementById('authRequiredModalClose');
    const stayBtn = document.getElementById('authRequiredStayBtn');

    function openAuthRequiredModal() {
        modal.classList.add('visible');
    }

    function closeAuthRequiredModal() {
        modal.classList.remove('visible');
    }

    window.openAuthRequiredModal = openAuthRequiredModal;
    window.closeAuthRequiredModal = closeAuthRequiredModal;

    window.handleAuthRequiredResponse = function (response) {
        if (!response) return false;
        if (response.status === 401 || response.status === 403) {
            openAuthRequiredModal();
            return true;
        }
        return false;
    };

    closeBtn?.addEventListener('click', closeAuthRequiredModal);
    stayBtn?.addEventListener('click', closeAuthRequiredModal);

    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeAuthRequiredModal();
    });

    document.addEventListener('click', (e) => {
        const trigger = e.target.closest('[data-requires-auth="true"]');
        if (!trigger) return;

        const isAuthenticated = document.body.dataset.authenticated === 'true';
        if (isAuthenticated) return;

        e.preventDefault();
        openAuthRequiredModal();
    });
})();