const API_BASE = "/resumenes";
const cuerpoTabla = document.getElementById("cuerpo-tabla");
const alertPlaceholder = document.getElementById("alert-placeholder");

// Bootstrap modal instances
let modalInfoEl = document.getElementById('modalInfo');
let modalInfo = new bootstrap.Modal(modalInfoEl);
let modalEditarEl = document.getElementById('modalEditar');
let modalEditar = new bootstrap.Modal(modalEditarEl);

// formulario & campos
const formEditar = document.getElementById('form-editar');
const inputId = document.getElementById('resumen-id');
const inputTitular = document.getElementById('resumen-titular');
const inputCuerpo = document.getElementById('resumen-cuerpo');
const btnNuevo = document.getElementById('btn-nuevo');
const btnRefrescar = document.getElementById('btn-refrescar');

let resumenes = []; // cache local

function showAlert(message, type = "info", timeout = 4000) {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = `
      <div class="alert alert-${type} alert-dismissible" role="alert">
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
      </div>
    `;
    alertPlaceholder.appendChild(wrapper);
    if (timeout) setTimeout(() => wrapper.remove(), timeout);
}

function handleFetchError(response) {
    if (!response.ok) {
        return response.text().then(text => {
            let msg = text;
            try { const j = JSON.parse(text); msg = j.message || JSON.stringify(j); } catch(e) {}
            throw new Error(`${response.status} ${response.statusText}: ${msg}`);
        });
    }
    return response;
}

/* Cargar todos */
function loadResumenes() {
    fetch(API_BASE)
        .then(handleFetchError)
        .then(r => r.json())
        .then(data => {
            resumenes = data;
            renderTabla();
        })
        .catch(err => {
            console.error(err);
            showAlert("Error cargando resúmenes: " + err.message, "danger", 6000);
        });
}

function renderTabla() {
    cuerpoTabla.innerHTML = "";
    if (!resumenes || resumenes.length === 0) {
        cuerpoTabla.innerHTML = `<tr><td colspan="3" class="text-center">No hay resúmenes</td></tr>`;
        return;
    }

    resumenes.forEach((res, idx) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${res.id}</td>
            <td>${escapeHtml(res.titular)}</td>
            <td>
                <div class="d-flex gap-2">
                  <button class="btn btn-sm btn-info" title="Ver" onclick="showInfo(${res.id})">Ver</button>
                  <button class="btn btn-sm btn-warning" title="Editar" onclick="openEdit(${res.id})">Editar</button>
                  <button class="btn btn-sm btn-danger" title="Eliminar" onclick="deleteResumen(${res.id})">Eliminar</button>
                </div>
            </td>
        `;
        cuerpoTabla.appendChild(tr);
    });
}

function escapeHtml(text) {
    if (text == null) return "";
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

/* Mostrar info */
window.showInfo = function(id) {
    fetch(`${API_BASE}/${id}`)
        .then(handleFetchError)
        .then(r => r.json())
        .then(res => {
            document.getElementById('info-titular').textContent = res.titular;
            document.getElementById('info-cuerpo').textContent = res.cuerpo;
            modalInfo.show();
        })
        .catch(err => {
            console.error(err);
            if (err.message.startsWith("404")) {
                showAlert("Resumen no encontrado", "warning");
            } else {
                showAlert("Error al obtener resumen: " + err.message, "danger");
            }
        });
};

/* Abrir modal para nuevo resumen */
btnNuevo.addEventListener('click', () => {
    inputId.value = "";
    inputTitular.value = "";
    inputCuerpo.value = "";
    document.getElementById('modalEditarTitle').textContent = "Nuevo resumen";
    modalEditar.show();
});

/* ⭐ NUEVO btnRefrescar con Loading de SweetAlert2 ⭐ */
btnRefrescar.addEventListener('click', () => {

    // 1. Mostrar pantalla de carga
    Swal.fire({
        title: 'Analizando Noticias',
        html: 'Consultando RSS y generando resúmenes con IA...<br><b>Esto puede tardar unos segundos.</b>',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading(); // Activa el spinner nativo de SweetAlert
        }
    });

    // 2. Hacer la petición al backend
    fetch(API_BASE + '/refresh', { method: "POST" })
        .then(handleFetchError)
        .then(() => {
            // 3. Recargar la tabla
            return loadResumenes();
        })
        .then(() => {
            // 4. Cerrar loading y mostrar éxito
            Swal.fire({
                icon: 'success',
                title: '¡Actualizado!',
                text: 'Se han obtenido las últimas noticias correctamente.',
                timer: 2000,
                showConfirmButton: false
            });
        })
        .catch(err => {
            console.error(err);
            // 5. Mostrar error si falla
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudieron refrescar las noticias: ' + err.message
            });
        });
});

/* Abrir modal para editar */
window.openEdit = function(id) {
    fetch(`${API_BASE}/${id}`)
        .then(handleFetchError)
        .then(r => r.json())
        .then(res => {
            inputId.value = res.id;
            inputTitular.value = res.titular;
            inputCuerpo.value = res.cuerpo;
            document.getElementById('modalEditarTitle').textContent = "Editar resumen";
            modalEditar.show();
        })
        .catch(err => {
            console.error(err);
            showAlert("No se pudo cargar el resumen para editar: " + err.message, "danger");
        });
};

/* Guardar */
formEditar.addEventListener('submit', (ev) => {
    ev.preventDefault();
    const id = inputId.value ? Number(inputId.value) : null;
    const payload = {
        titular: inputTitular.value.trim(),
        cuerpo: inputCuerpo.value
    };

    if (!payload.titular) {
        showAlert("El titular es obligatorio", "warning");
        return;
    }

    if (id === null) {
        fetch(API_BASE, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        })
            .then(handleFetchError)
            .then(r => r.json())
            .then(nuevo => {
                resumenes.push(nuevo);
                renderTabla();
                modalEditar.hide();
                showAlert("Resumen creado", "success");
            })
            .catch(err => {
                console.error(err);
                showAlert("Error creando resumen: " + err.message, "danger");
            });
    } else {
        fetch(`${API_BASE}/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        })
            .then(handleFetchError)
            .then(r => r.json())
            .then(actualizado => {
                const i = resumenes.findIndex(r => r.id === actualizado.id);
                if (i >= 0) resumenes[i] = actualizado;
                renderTabla();
                modalEditar.hide();
                showAlert("Resumen actualizado", "success");
            })
            .catch(err => {
                console.error(err);
                showAlert("Error actualizando resumen: " + err.message, "danger");
            });
    }
});

/* deleteResumen con SweetAlert2 */
window.deleteResumen = function(id) {
    Swal.fire({
        title: '¿Eliminar resumen?',
        text: 'Esta acción no se puede deshacer.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6'
    }).then((result) => {
        if (result.isConfirmed) {

            fetch(`${API_BASE}/${id}`, { method: "DELETE" })
                .then(response => {
                    if (response.status === 404) throw new Error("404 Not Found");
                    if (!response.ok) {
                        return response.text().then(t => { throw new Error(t || response.statusText); });
                    }

                    resumenes = resumenes.filter(r => r.id !== id);
                    renderTabla();

                    Swal.fire({
                        icon: 'success',
                        title: 'Eliminado',
                        text: 'El resumen ha sido eliminado correctamente.',
                        timer: 2000,
                        showConfirmButton: false
                    });
                })
                .catch(err => {
                    Swal.fire({
                        icon: 'error',
                        title: 'Error',
                        text: 'Error eliminando: ' + err.message
                    });
                });

        }
    });
};

// carga inicial
loadResumenes();