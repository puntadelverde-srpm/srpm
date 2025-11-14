package org.srpm.exception;

/**
 * Su único propósito es darnos un "nombre" para el error "No Encontrado".
 * 3.  El MainController la lanza cuando no encuentra un ID.
 * 4.  El GlobalExceptionHandler sabe cómo reaccionar CUANDO OYE ESTA ALARMA EN CONCRETO.
 */
public class ResumenNotFoundException extends RuntimeException {

    // Constructor simple
    public ResumenNotFoundException(String message) {
        super(message); // Pasa el mensaje a la clase padre
    }
}