package org.srpm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.srpm.model.Resumen;

import java.util.ArrayList;
import java.util.List;

@RestController
public class MainController {

    @GetMapping("/")
    public List<Resumen> index() {
        ArrayList<Resumen> resumens = new ArrayList<>();
        resumens.add(new Resumen("Mazón dimite", "blabkaabalbsbalba", 0));
        resumens.add(new Resumen("Mazón dimite2", "blabkaabalbsbalba", 1));
        return resumens; // Se convierte automáticamente a JSON
    }
}