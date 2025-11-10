package org.srpm.controller;

import org.srpm.dao.NoticiaDAO;
import org.srpm.model.Noticia;
import org.srpm.service.RssParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/noticias")
@CrossOrigin(origins = "*")
public class NoticiaController {

    private final NoticiaDAO noticiaDAO;
    private final RssParserService parserService;

    // **CAMBIO MINIMO:** Se define una constante para el valor por defecto (5),
    // para mantener el comportamiento original si no se pasa el parámetro.
    private static final int LIMITE_POR_DEFECTO = 5;

    @Autowired
    public NoticiaController(NoticiaDAO noticiaDAO, RssParserService parserService) {
        this.noticiaDAO = noticiaDAO;
        this.parserService = parserService;
    }


    /**
     * Endpoint para que el frontend dispare la actualización de los RSS.
     * URL: POST http://localhost:8080/api/noticias/actualizar?limite=3
     * * **CAMBIO MINIMO:** Se añade el parámetro `@RequestParam` con un valor por defecto.
     */
    @PostMapping("/actualizar")
    public ResponseEntity<String> triggerUpdate(
            @RequestParam(name = "limite", required = false, defaultValue = "" + LIMITE_POR_DEFECTO) int limite
    ) {
        // **CAMBIO MINIMO:** Se pasa el parámetro al servicio.
        String resumen = parserService.fetchAllFeeds(limite);

        return ResponseEntity.ok(resumen);
    }






    // ... (getTodasLasNoticias y getNoticiasPorFuente se mantienen igual) ...
    @GetMapping
    public List<Noticia> getTodasLasNoticias() {
        return noticiaDAO.findAll();
    }

    @GetMapping("/fuente/{fuente}")
    public List<Noticia> getNoticiasPorFuente(@PathVariable String fuente) {
        List<Noticia> todasLasNoticias = noticiaDAO.findAll();
        List<Noticia> noticiasFiltradas = new ArrayList<>();
        for (Noticia noticia : todasLasNoticias) {
            if (noticia.getFuente().equalsIgnoreCase(fuente)) {
                noticiasFiltradas.add(noticia);
            }
        }
        return noticiasFiltradas;
    }
}
