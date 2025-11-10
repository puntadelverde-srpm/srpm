package org.srpm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.srpm.dao.NoticiaDAO;
import org.srpm.dao.NoticiaDaoEnMemoria;
import org.srpm.model.Resumen;
import org.srpm.service.RssParserService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/resumenes")
public class MainController {

    private final List<Resumen> resumenes = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);


    private final NoticiaDaoEnMemoria noticiaDaoEnMemoria;
    private final RssParserService rssParserService;



    @Autowired
    public MainController(NoticiaDaoEnMemoria noticiaDaoEnMemoria, RssParserService rssParserService) {

        this.noticiaDaoEnMemoria = noticiaDaoEnMemoria;
        this.rssParserService = new RssParserService(noticiaDaoEnMemoria);

        rssParserService.parseAndSaveFeed(noticiaDaoEnMemoria);
    }



    @GetMapping
    public List<Resumen> getAll() {
        return resumenes;
    }


    @GetMapping("/{id}")
    public Resumen getById(@PathVariable Integer id) {
        return resumenes.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Resumen no encontrado con id: " + id));
    }


    @PostMapping
    public Resumen addResumen(@RequestBody Resumen nuevoResumen) {
        nuevoResumen.setId(idCounter.getAndIncrement());
        resumenes.add(nuevoResumen);
        return nuevoResumen;
    }


    @PutMapping("/{id}")
    public Resumen updateResumen(@PathVariable Integer id, @RequestBody Resumen actualizado) {
        for (Resumen resumen : resumenes) {
            if (resumen.getId().equals(id)) {
                resumen.setTitular(actualizado.getTitular());
                resumen.setCuerpo(actualizado.getCuerpo());
                return resumen;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Resumen no encontrado con id: " + id);
    }

    @DeleteMapping("/{id}")
    public void deleteResumen(@PathVariable Integer id) {
        boolean removed = resumenes.removeIf(resumen -> resumen.getId().equals(id));
        if (!removed) {
            throw new RuntimeException("Resumen no encontrado con id: " + id);
        }
    }
}
