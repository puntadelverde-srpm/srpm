package org.srpm.exception;

import java.time.LocalDateTime;

/**
 * Esta es la plantilla para el JSON de error que le daremos al usuario.
 * Spring convertirá esto automáticamente en un JSON bonito como:
 * {
 * "timestamp": "2025-11-14T18:30:00",
 * "status": 404,
 * "error": "No se encontró el resumen...",
 * "path": "/resumenes/ID"
 * }
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String path
) {
}