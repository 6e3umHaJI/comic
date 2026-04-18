document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.js-admin-complaint-status-select').forEach((select) => {
        select.addEventListener('change', () => {
            const form = select.closest('form');
            if (form) {
                form.submit();
            }
        });
    });
});
