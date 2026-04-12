document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('profileMenuToggle');
    const dropdown = document.getElementById('profileDropdown');

    if (!toggle || !dropdown) return;

    function openDropdown() {
        dropdown.hidden = false;
        toggle.setAttribute('aria-expanded', 'true');
    }

    function closeDropdown() {
        dropdown.hidden = true;
        toggle.setAttribute('aria-expanded', 'false');
    }

    function toggleDropdown() {
        if (dropdown.hidden) {
            openDropdown();
        } else {
            closeDropdown();
        }
    }

    toggle.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        toggleDropdown();
    });

    dropdown.addEventListener('click', (e) => {
        e.stopPropagation();
    });

    document.addEventListener('click', () => {
        closeDropdown();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeDropdown();
        }
    });
});
