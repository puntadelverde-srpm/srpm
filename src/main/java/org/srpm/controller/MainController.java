package org.srpm.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.srpm.model.Resumen;
import org.srpm.service.ResumenService;

import java.util.List;

@RestController
@RequestMapping("/resumenes")
public class MainController {

    private final ResumenService resumenService;

    @Autowired
    public MainController(ResumenService resumenService) {
        this.resumenService = resumenService;
    }


    @PostConstruct
    public void init() {
        resumenService.generarYGuardarResumenes();
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK) // Devuelve 200 OK cuando termina
    public void refrescar() {
        resumenService.refrescarTodo();
    }

    // --- ENDPOINTS HTTP ---

    @GetMapping
    public List<Resumen> getAll() {
        return resumenService.findAll();
    }

    @GetMapping("/{id}")
    public Resumen getById(@PathVariable Long id) {
        return resumenService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resumen create(@RequestBody Resumen resumen) {
        return resumenService.save(resumen);
    }

    @PutMapping("/{id}")
    public Resumen update(@PathVariable Long id, @RequestBody Resumen resumen) {
        return resumenService.update(id, resumen);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        resumenService.delete(id);
    }
}