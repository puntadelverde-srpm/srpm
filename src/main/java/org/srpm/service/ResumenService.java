package org.srpm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.srpm.dao.NoticiaDAO;
import org.srpm.dao.ResumenDAO;
import org.srpm.exception.ResumenNotFoundException;
import org.srpm.model.Noticia;
import org.srpm.model.Resumen;

import java.time.Duration;
import java.util.*;

@Service
public class ResumenService {

    // Dependencias
    private final RssParserService rssParserService;
    private final ResumenDAO resumenDAO;
    private final NoticiaDAO noticiaDAO;
    private final RestTemplate restTemplate;

    @Value("${ia.service.url}")
    private String urlServicioIA;

    private static final int TIMEOUT_SECONDS = 30;
    private static final int LIMITE_NOTICIAS = 20;

    @Autowired
    public ResumenService(RssParserService rssParserService,
                          ResumenDAO resumenDAO,
                          NoticiaDAO noticiaDAO,
                          RestTemplateBuilder restTemplateBuilder) {
        this.rssParserService = rssParserService;
        this.resumenDAO = resumenDAO;
        this.noticiaDAO = noticiaDAO;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }


    /**
     * Método maestro que coordina todo el proceso de actualización.
     *
     */
    public void generarYGuardarResumenes() {
        System.out.println("--- [SERVICIO] Iniciando ciclo de actualización ---");

        // 1. Descargar noticias RSS
        rssParserService.fetchAllFeeds(LIMITE_NOTICIAS);

        // 2. Obtener noticias crudas de la BBDD
        List<Noticia> noticias = noticiaDAO.findAll();
        if (noticias.isEmpty()) {
            System.out.println("No hay noticias para procesar.");
            return;
        }

        // 3. Obtener resúmenes de la IA
        List<Resumen> nuevosResumenes = obtenerResumenesDeIA(noticias);

        // 4. Guardar resultados
        guardarResumenesEnBBDD(nuevosResumenes);
    }


    // MÉTODOS HELPER

    private List<Resumen> obtenerResumenesDeIA(List<Noticia> noticias) {
        System.out.println("Enviando " + noticias.size() + " noticias a la IA...");
        try {
            ResponseEntity<Resumen[]> response = restTemplate.postForEntity(
                    urlServicioIA,
                    noticias,
                    Resumen[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                System.err.println("La IA respondió con estado: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            System.err.println("Error conectando con el servicio de IA: " + e.getMessage());
            // No lanzamos excepción para no detener la app, devolvemos lista vacía
        }
        return Collections.emptyList();
    }

    private void guardarResumenesEnBBDD(List<Resumen> resumenes) {
        if (resumenes.isEmpty()) return;

        System.out.println("Guardando " + resumenes.size() + " resúmenes nuevos.");
        for (Resumen resumen : resumenes) {
            resumenDAO.save(resumen);
        }
    }


    public List<Resumen> findAll() {
        return resumenDAO.findAll();
    }

    public Resumen findById(Long id) {
        return resumenDAO.findById(id)
                .orElseThrow(() -> new ResumenNotFoundException("Resumen no encontrado con ID: " + id));
    }

    public Resumen save(Resumen resumen) {
        resumenDAO.save(resumen);
        return resumen;
    }


    public Resumen update(Long id, Resumen resumenNuevosDatos) {
        Resumen existente = findById(id); // Reutilizamos findById que ya lanza la excepción

        existente.setTitular(resumenNuevosDatos.getTitular());
        existente.setCuerpo(resumenNuevosDatos.getCuerpo());

        resumenDAO.save(existente);
        return existente;
    }

    public void delete(Long id) {
        if (resumenDAO.findById(id).isEmpty()) {
            throw new ResumenNotFoundException("No se puede eliminar. ID no existe: " + id);
        }
        resumenDAO.deleteById(id);
    }
    public void refrescarTodo() {
        System.out.println("--- [SERVICIO] Ejecutando REFRESCO TOTAL ---");

        // 1. Limpieza total (Borrón)
        resumenDAO.deleteAll();
        noticiaDAO.deleteAll();
        System.out.println("Datos antiguos eliminados.");

        this.generarYGuardarResumenes();

        System.out.println("--- [SERVICIO] Refresco completado ---");
    }
 }
