package org.srpm.exception;

/**
 * Su único propósito es darnos un "nombre" para el error "No Encontrado".
 *  El MainController la lanza cuando no encuentra un ID.
 *  El GlobalExceptionHandler sabe cómo reaccionar CUANDO OYE ESTA ALARMA EN CONCRETO.
 */
public class ResumenNotFoundException extends RuntimeException {

    public ResumenNotFoundException(String message) {
        super(message);
    }
}