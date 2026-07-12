(function () {
  const token = localStorage.getItem('token');
  const rol   = (localStorage.getItem('rol') || '').toLowerCase();
  const ROLES_PERMITIDOS = ['municipalidad_admin', 'municipalidad_operador'];
  if (!token || !ROLES_PERMITIDOS.includes(rol)) {
    window.location.href = '/admin/login';
    return;
  }
  window.ADMIN_TOKEN = token;
  window.ADMIN_ROL   = rol;
  document.addEventListener('DOMContentLoaded', () => {
    const nombreEl = document.getElementById('sb-nombre');
    if (nombreEl) {
      const nombreCompleto = (localStorage.getItem('nombre') || '') + ' ' + (localStorage.getItem('apellido') || '');
      nombreEl.textContent = nombreCompleto;
      nombreEl.title = nombreCompleto;
    }
    const seccionAdmin = document.getElementById('seccion-administrador');
    if (seccionAdmin && rol === 'municipalidad_admin') {
      seccionAdmin.style.display = 'block';
    }
  });
})();

async function cerrarSesion() {
  try {
    await fetch('/api/auth/logout', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + window.ADMIN_TOKEN }
    });
  } finally {
    localStorage.clear();
    window.location.href = '/';
  }
}

(function () {
  const MIN_VISIBLE_MS = 350;
  let pending = 0;
  let overlayEl = null;
  let shownAt = 0;
  let hideTimer = null;

  function injectStyles() {
    if (document.getElementById('admin-loader-styles')) return;
    const style = document.createElement('style');
    style.id = 'admin-loader-styles';
    style.textContent = `
      .admin-loader-overlay {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(255,255,255,.82);
        backdrop-filter: blur(2px);
        -webkit-backdrop-filter: blur(2px);
        opacity: 0;
        visibility: hidden;
        pointer-events: none;
        transition: opacity .18s ease;
        z-index: 5000;
        border-radius: inherit;
      }
      .admin-loader-overlay.is-visible {
        opacity: 1;
        visibility: visible;
        pointer-events: auto;
      }
      .admin-loader-box {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: .8rem;
      }
      .admin-loader-ring {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        border: 3px solid rgba(26,58,143,.12);
        border-top-color: #1a3a8f;
        border-right-color: #0fa89a;
        animation: admin-loader-spin .75s linear infinite;
      }
      .admin-loader-text {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        font-size: .78rem;
        font-weight: 600;
        color: #4b5563;
        letter-spacing: .01em;
      }
      @keyframes admin-loader-spin {
        to { transform: rotate(360deg); }
      }
    `;
    document.head.appendChild(style);
  }


  function getLoaderHost() {
    const modalBox = document.querySelector('.modal-overlay.open .modal');
    if (modalBox) return modalBox;
    return document.querySelector('.content') || document.querySelector('.main') || document.body;
  }

  function ensureOverlay() {
    injectStyles();
    const host = getLoaderHost();
    if (getComputedStyle(host).position === 'static') {
      host.style.position = 'relative';
    }
    if (!overlayEl) {
      overlayEl = document.createElement('div');
      overlayEl.className = 'admin-loader-overlay';
      overlayEl.innerHTML = `
        <div class="admin-loader-box">
          <div class="admin-loader-ring"></div>
          <div class="admin-loader-text">Cargando información…</div>
        </div>
      `;
    }

    if (overlayEl.parentElement !== host) {
      host.appendChild(overlayEl);
    }
    return overlayEl;
  }

  function showLoader() {
    clearTimeout(hideTimer);
    const el = ensureOverlay();
    if (!el.classList.contains('is-visible')) {
      shownAt = Date.now();
      el.classList.add('is-visible');
    }
  }

  function hideLoader() {
    const el = ensureOverlay();
    const elapsed = Date.now() - shownAt;
    const wait = Math.max(0, MIN_VISIBLE_MS - elapsed);
    clearTimeout(hideTimer);
    hideTimer = setTimeout(() => el.classList.remove('is-visible'), wait);
  }

  const originalFetch = window.fetch.bind(window);
  window.fetch = function (...args) {
    const req = args[0];
    const url = (req && req.url) ? req.url : req;
    const isApiCall = typeof url === 'string' && url.includes('/api/');
    if (isApiCall) {
      pending++;
      showLoader();
    }
    return originalFetch(...args).finally(() => {
      if (isApiCall) {
        pending = Math.max(0, pending - 1);
        if (pending === 0) hideLoader();
      }
    });
  };

  window.AdminLoader = { show: showLoader, hide: hideLoader };
})();