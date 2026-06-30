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
      nombreEl.textContent =
        (localStorage.getItem('nombre') || '') + ' ' + (localStorage.getItem('apellido') || '');
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