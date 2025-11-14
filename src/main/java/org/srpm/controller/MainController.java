package org.srpm.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.srpm.dao.NoticiaDaoEnMemoria;
import org.srpm.dao.ResumenDaoEnMemoria;
import org.srpm.exception.ResumenNotFoundException;
import org.srpm.model.Noticia;
import org.srpm.model.Resumen;
import org.srpm.service.RssParserService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * =================================================================================
 * EXPLICACIÓN PARA EL EQUIPO (CONTROLADOR)
 * =================================================================================
 * 1.  Su trabajo principal es el CRUD (Crear, Leer, Actualizar, Borrar).
 * 2.  Ya NO se encarga de gestionar errores.
 * 3.  Si no encuentra un resumen, simplemente  lanza la
 * excepción 'ResumenNotFoundException' y deja que el
 * (GlobalExceptionHandler) se ocupe.
 *
 * Esto hace nuestro código MUCHO más limpio.
 */
@RestController
@RequestMapping("/resumenes") // Todas las URL de esta clase empiezan con /resumenes
public class MainController {

    // --- Dependencias (los "Ingredientes" que necesita) ---
    private final RssParserService rssParserService;
    private final ResumenDaoEnMemoria resumenDaoEnMemoria;
    private final NoticiaDaoEnMemoria noticiaDaoEnMemoria; // ¡Importante! Para leer las noticias
    private final RestTemplate restTemplate; // El "Teléfono" para llamar a la IA

    // La URL de la IA, leída desde application.properties
    @Value("${ia.service.url}")
    private String urlServicioIA;

    private static final int TIMEOUT_SECONDS = 30; // Tiempo de espera para la IA

    /**
     * El constructor. Spring usa esto para "inyectar" (darnos) las herramientas.
     */
    @Autowired
    public MainController(RssParserService rssParserService,
                          ResumenDaoEnMemoria resumenDaoEnMemoria,
                          NoticiaDaoEnMemoria noticiaDaoEnMemoria, // <-- AÑADIDO
                          RestTemplateBuilder restTemplateBuilder) {

        this.rssParserService = rssParserService;
        this.resumenDaoEnMemoria = resumenDaoEnMemoria;
        this.noticiaDaoEnMemoria = noticiaDaoEnMemoria; // <-- AÑADIDO


        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * MÉTODO DE ARRANQUE (@PostConstruct)
     * Esto se ejecuta UNA SOLA VEZ cuando la app arranca.
     * Su trabajo es llenar los DaoEnMemoria de resúmenes.
     */
    @PostConstruct
    public void inicializarYProcesarDatos() {
        System.out.println("Iniciando procesamiento de datos...");

        // 1. Le decimos al RssParser que lea los feeds.
        // Este servicio los guarda en NoticiaDaoEnMemoria.
        rssParserService.fetchAllFeeds(20);

        // 2. Recogemos las noticias que el parser acaba de guardar.
        List<Noticia> noticias = noticiaDaoEnMemoria.findAll();

        // Si no hay noticias, no molestamos a la IA.
        if (noticias.isEmpty()) {
            System.out.println("No se encontraron noticias. No se llama a la IA.");
            return;
        }

        System.out.println("Enviando " + noticias.size() + " noticias al servicio de IA...");

        // 3. Llamamos a la IA (Proyecto api-ia)
        // Este try-catch SÍ se queda aquí, porque si falla, la app
        // puede seguir funcionando (aunque no tenga resúmenes).
        try {
            ResponseEntity<Resumen[]> response = restTemplate.postForEntity(
                    urlServicioIA, // URL de application.properties
                    noticias,      // El body de la petición
                    Resumen[].class // Esperamos un array de Resumen
            );

            // 4. Si la IA responde bien (2xx), guardamos los resúmenes
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                List<Resumen> resumenes = Arrays.asList(response.getBody());

                // Guardamos cada resumen en el DAO de Resúmenes
                resumenes.forEach(resumenDaoEnMemoria::save);

                System.out.println("Guardados " + resumenes.size() + " resúmenes en el DAO.");
            } else {
                System.err.println("La API de IA devolvió un estado: " + response.getStatusCode());
            }

        } catch (ResourceAccessException e) {
            // Error de conexión (el Proyecto api-ia NO está arrancado)
            System.err.println("------------------------------------------------------------------");
            System.err.println(" ¡No se pudo conectar a la API de IA!");
            System.err.println("URL: " + urlServicioIA);
            System.err.println("ASEGÚRATE DE QUE EL PROYECTO API-IA (analizadornoticias) ESTÉ EN EJECUCIÓN.");
            System.err.println("------------------------------------------------------------------");
        } catch (RestClientException e) {
            // Error 500 o 400 de la IA (la IA falló por dentro)
            System.err.println("La API de IA devolvió un error. Mensaje: " + e.getMessage());
        }
    }

    // =================================================================================
    // AQUÍ EMPIEZA EL CRUD (Create, Read, Update, Delete)
    // =================================================================================

    /**
     * Obtener TODOS los resúmenes.
     * GET /resumenes
     */
    @GetMapping
    public List<Resumen> getAll() {
        return resumenDaoEnMemoria.findAll();
    }

    /**
     * Obtener UN resumen por su ID.
     * GET /resumenes/id
     */
    @GetMapping("/{id}")
    public Resumen getById(@PathVariable Long id) {
        // Busca el resumen. Si no lo encuentra (.orElseThrow),
        // lanza ResumenNotFoundException.
        return resumenDaoEnMemoria.findById(id)
                .orElseThrow(() -> new ResumenNotFoundException("No se encontró el resumen con ID: " + id));
    }

    /**
     * Añadir un nuevo resumen.
     * POST /resumenes
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Devuelve un código 201 (Creado)
    public Resumen create(@RequestBody Resumen resumen) {
        // El save le asigna un ID si es nuevo
        resumenDaoEnMemoria.save(resumen);
        return resumen;
    }

    /**
     * Modificar un resumen existente.
     * PUT /resumenes/id
     */
    @PutMapping("/{id}")
    public Resumen update(@PathVariable Long id, @RequestBody Resumen resumenConCambios) {

        // 1. Comprueba si existe. Si no, lanza un 404.
        Resumen resumenExistente = resumenDaoEnMemoria.findById(id)
                .orElseThrow(() -> new ResumenNotFoundException("No se puede actualizar. No existe el resumen con ID: " + id));

        // 2. Actualiza los datos del resumen que encontramos
        resumenExistente.setTitular(resumenConCambios.getTitular());
        resumenExistente.setCuerpo(resumenConCambios.getCuerpo());

        // 3. Guarda los cambios. (Como es un objeto en memoria, ya está actualizado,
        // pero 'save' se asegura de ello).
        resumenDaoEnMemoria.save(resumenExistente);

        // 4. Devuelve el resumen con los cambios aplicados.
        return resumenExistente;
    }

    /**
     *Eliminar un resumen por su ID.
     * DELETE /resumenes/id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Devuelve un 204 (Sin Contenido)
    public void delete(@PathVariable Long id) {

        // 1. Comprueba si existe. Si no, lanza un 404.
        if (resumenDaoEnMemoria.findById(id).isEmpty()) {
            throw new ResumenNotFoundException("No se puede eliminar. No existe el resumen con ID: " + id);
        }

        // 2. Si existe, bórralo.
        resumenDaoEnMemoria.deleteById(id);
    }
}