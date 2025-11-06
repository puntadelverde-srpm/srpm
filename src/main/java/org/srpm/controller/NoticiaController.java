package org.srpm.controller;

import org.srpm.dao.NoticiaDAO;
import org.srpm.model.Noticia;
import org.srpm.service.RssParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * API REST para exponer las noticias.
 */
@RestController
@RequestMapping("/api/v1/noticias")
/**
 * Habilita CORS para que tu amigo (Frontend) pueda llamar a esta API
 * desde otro dominio (ej. localhost:3000).
 * En producción, cambia "*" por la URL de tu frontend.
 */
@CrossOrigin(origins = "*")
public class NoticiaController {

    private final NoticiaDAO noticiaDAO;
    private final RssParserService parserService;

    @Autowired
    public NoticiaController(NoticiaDAO noticiaDAO, RssParserService parserService) {
        this.noticiaDAO = noticiaDAO;
        this.parserService = parserService;
    }

    /**
     * Endpoint para obtener TODAS las noticias.
     * URL: GET http://localhost:8080/api/v1/noticias
     */
    @GetMapping
    public List<Noticia> getTodasLasNoticias() {
        return noticiaDAO.findAll();
    }

    /**
     * Endpoint para filtrar noticias por fuente.
     * URL: GET http://localhost:8080/api/v1/noticias/fuente/COPE
     * * --- VERSIÓN MODIFICADA CON BUCLE 'FOR' ---
     */
    @GetMapping("/fuente/{fuente}")
    public List<Noticia> getNoticiasPorFuente(@PathVariable String fuente) {

        // 1. Obtener la lista completa (la "caja grande")
        List<Noticia> todasLasNoticias = noticiaDAO.findAll();

        // 2. Crear una lista vacía para guardar los resultados (la "caja nueva")
        List<Noticia> noticiasFiltradas = new ArrayList<>();

        // 3. Recorrer cada noticia de la lista completa (el "inspector")
        for (Noticia noticia : todasLasNoticias) {

            // 4. La condición del filtro: ¿La fuente coincide? (ignorando mayús/minús)
            if (noticia.getFuente().equalsIgnoreCase(fuente)) {

                // 5. Si coincide, añadirla a la lista de resultados
                noticiasFiltradas.add(noticia);
            }
        }

        // 6. Devolver la lista filtrada
        return noticiasFiltradas;
    }

    /**
     * Endpoint para que el frontend dispare la actualización de los RSS.
     * URL: POST http://localhost:8080/api/v1/noticias/actualizar
     */
    @PostMapping("/actualizar")
    public ResponseEntity<String> triggerUpdate() {
        // 1. Llama al servicio para que haga el trabajo
        String resumen = parserService.fetchAllFeeds();

        // 2. Devuelve una respuesta (200 OK) con el resumen
        return ResponseEntity.ok(resumen);
    }
}