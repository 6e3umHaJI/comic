<script>
    (function () {
        try {
            const storedTheme = localStorage.getItem('theme');
            document.documentElement.setAttribute('data-theme', storedTheme || 'light');
        } catch (e) {
            document.documentElement.setAttribute('data-theme', 'light');
        }
    })();
</script>