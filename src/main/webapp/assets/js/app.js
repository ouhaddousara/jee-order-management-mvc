/*
 * Progressive enhancement only: every page works without this script
 * (confirmation prompts, dynamic order rows and the auto-submitting filter are conveniences).
 */
(function () {
    'use strict';

    // Ask for confirmation before any form carrying data-confirm="message" is submitted.
    document.addEventListener('submit', function (event) {
        var message = event.target.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });

    // Status filter: submit as soon as the selection changes.
    document.querySelectorAll('select[data-autosubmit]').forEach(function (select) {
        select.addEventListener('change', function () {
            select.form.submit();
        });
    });

    // Order creation form: add / remove product rows.
    var body = document.getElementById('lines-body');
    var addButton = document.getElementById('add-line');
    if (!body || !addButton) {
        return;
    }

    function clearRow(row) {
        row.querySelectorAll('select').forEach(function (s) { s.selectedIndex = 0; });
        row.querySelectorAll('input').forEach(function (i) { i.value = ''; });
    }

    addButton.addEventListener('click', function () {
        var row = body.querySelector('tr').cloneNode(true);
        clearRow(row);
        body.appendChild(row);
        var select = row.querySelector('select');
        if (select) {
            select.focus();
        }
    });

    body.addEventListener('click', function (event) {
        if (!event.target.classList.contains('remove-line')) {
            return;
        }
        var row = event.target.closest('tr');
        if (body.querySelectorAll('tr').length > 1) {
            row.remove();
        } else {
            clearRow(row); // keep at least one row on screen
        }
    });
})();
