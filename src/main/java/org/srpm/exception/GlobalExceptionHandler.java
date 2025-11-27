package org.srpm.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

/**
 * Esta es la clase MÁS IMPORTANTE para la gestión de errores.
 *
 * @RestControllerAdvice: Le dice a Spring "Voy a vigilar a TODOS
 * los Controladores de esta aplicación".
 *
 * @ExceptionHandler: Son sus "oídos". Tiene un metodo para cada "alarma"
 * que sabe cómo gestionar.
 *
 * 3.  Beneficio: El MainController solo hace el CRUD y no se
 * preocupa de los errores. Esta clase se encarga de todo
 * el trabajo sucio de hablar con el cliente si algo sale mal.
 */
@RestControllerAdvice
public class GlobalExceptionHandler{

    // Un "Logger" para escribir en la consola del servidor
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Este método "oye" al ResumenNotFoundException.
     *
     * @param ex      La alarma que se pulsó (contiene el mensaje de error).
     * @param request La petición del cliente (para saber qué URL falló).
     * @return Una "Nota de Disculpa" (ErrorResponse) con el código 404 NOT FOUND.
     */
    @ExceptionHandler(ResumenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResumenNotFound(
            ResumenNotFoundException ex, HttpServletRequest request) {

        // 1. Crea la "Nota de Disculpa" rellenando la plantilla
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(), // 404
                ex.getMessage(), // El mensaje: "No se encontró el resumen..."
                request.getRequestURI() // La URL: "/resumenes/99"
        );

        // 2. La envía al cliente con el código de estado 404
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Este método "oye" CUALQUIER OTRA ALARMA INESPERADA (Exception.class).
     * Es el "atrapa-todo". Evita que la app "explote" con una página blanca.
     *
     * @param ex      El error inesperado (ej. NullPointerException).
     * @param request La petición del cliente.
     * @return Una "Nota de Disculpa" genérica con el código 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // 1. ¡¡MUY IMPORTANTE!! Escribe el error real en la consola del servidor
        //    para que nosotros (los programadores) podamos ver qué pasó.
        logger.error("Error inesperado (500) capturado por el Jefe de Sala: ", ex);

        // 2. Crea una "Nota de Disculpa" genérica.
        //    ¡NUNCA le des los detalles del error al cliente! (Seguridad)
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                "Error interno del servidor. Por favor, contacte al administrador.",
                request.getRequestURI()
        );

        // 3. La envía al cliente con el código 500
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}