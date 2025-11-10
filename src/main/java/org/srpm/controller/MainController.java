package org.srpm.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.srpm.dao.NoticiaDaoEnMemoria;
import org.srpm.dao.ResumenDaoEnMemoria;
import org.srpm.model.Noticia;
import org.srpm.model.Resumen;
import org.srpm.service.RssParserService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/resumenes")
public class MainController {

    private final RssParserService rssParserService;
    private final NoticiaDaoEnMemoria noticiaDaoEnMemoria;
    private final ResumenDaoEnMemoria resumenDaoEnMemoria;

    // Configuración del timeout
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * CORRECCIÓN 1: El RestTemplate se inicializa de forma FINAL aquí
     * con la configuración de timeouts y ya no se toca en el constructor.
     */
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)) // Tiempo máximo para establecer la conexión
            .setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))    // Tiempo máximo para esperar los datos
            .build();

    @Autowired
    public MainController(NoticiaDaoEnMemoria noticiaDaoEnMemoria,
                          RssParserService rssParserService,
                          ResumenDaoEnMemoria resumenDaoEnMemoria) {

        this.noticiaDaoEnMemoria = noticiaDaoEnMemoria;
        this.rssParserService = rssParserService;
        this.resumenDaoEnMemoria = resumenDaoEnMemoria;

        // SE ELIMINA LA LÍNEA PROBLEMÁTICA: this.restTemplate = new RestTemplate();
    }

    /**
     * CORRECCIÓN 2: Se añade un bloque try-catch para manejar errores de HTTP (4xx/5xx)
     * o errores de conexión/timeout, asegurando que la aplicación se inicie.
     */
    @PostConstruct
    public void inicializarYProcesarDatos() {
        System.out.println("Iniciando procesamiento de datos y llamada a API externa...");

        // 1. Ejecutar el parseo de RSS
        rssParserService.fetchAllFeeds(10);

        // 2. Obtener las noticias
        List<Noticia> noticias = noticiaDaoEnMemoria.findAll();

        // 3. Configuración de la llamada POST
        String url = "http://localhost:8081/api/resumenes-objetivos";

        try {
            // 4. Llamada a la API externa
            ResponseEntity<Resumen[]> response = restTemplate.postForEntity(
                    url,
                    noticias,
                    Resumen[].class
            );

            // 5. Procesar la respuesta y guardar en el DAO
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {

                // Convertir el array de respuesta a lista
                List<Resumen> resumenes = Arrays.asList(response.getBody());

                // 6. ¡¡GUARDAR EN EL DAO!!
                for (Resumen resumen : resumenes) {
                    resumenDaoEnMemoria.save(resumen);
                }

                System.out.println("Guardados " + resumenes.size() + " resúmenes en el DAO.");
            } else {
                // Esto se manejaría si la API devuelve un 3xx o si no tiene cuerpo, pero no un 4xx/5xx (que lanza excepción)
                System.err.println("La API devolvió un estado inesperado pero no falló con excepción. Status: " + response.getStatusCode());
            }
        } catch (ResourceAccessException e) {
            // Error de conexión (ej. localhost:8081 no está corriendo o timeout)
            System.err.println("ERROR DE CONEXIÓN: La API externa en " + url + " no está disponible o ha agotado el tiempo de espera (timeout).");
            System.err.println("Detalle: " + e.getMessage());
        } catch (RestClientException e) {
            // Error de respuesta (ej. 400 Bad Request, 500 Internal Server Error)
            System.err.println("ERROR DE RESPUESTA: La API externa devolvió un error HTTP (4xx o 5xx).");
            System.err.println("Mensaje: " + e.getMessage());
            // Si quieres ver la traza completa, descomenta: e.printStackTrace();
        }

    }


    @GetMapping
    public List<Resumen> getAll() {
        return resumenDaoEnMemoria.findAll();
    }
}