package org.srpm.controller;

import org.srpm.dao.NoticiaDAO;
import org.srpm.model.Noticia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * API REST para el "Tercer Servicio".
 * Recibe listas de IDs de noticias y devuelve las listas
 * de los objetos Noticia completos.
 */
@RestController
@RequestMapping("/api/v1/noticias-agrupadas")
@CrossOrigin(origins = "*") // Habilita CORS igual que en el otro controlador
public class NoticiaAgrupadaController {

    private final NoticiaDAO noticiaDAO;

    // Inyectamos el NoticiaDAO. Es el mismo que usa RssParserService
    // y NoticiaController. Spring se encarga de dárnoslo.
    @Autowired
    public NoticiaAgrupadaController(NoticiaDAO noticiaDAO) {
        this.noticiaDAO = noticiaDAO;
    }

    /**
     * Endpoint para "resolver" o "hidratar" los grupos de IDs.
     * URL: POST http://localhost:8080/api/v1/noticias-agrupadas/resolver
     *
     * Ejemplo de Body (JSON) que espera recibir:
     * [
     * [1, 5, 12],
     * [2, 8],
     * [22, 30, 41]
     * ]
     */
    @PostMapping("/resolver")
    public List<List<Noticia>> resolverNoticiasAgrupadas(
            @RequestBody List<List<Long>> gruposDeIds) {

        // 1. Creamos la lista principal que vamos a devolver
        List<List<Noticia>> resultadoFinal = new ArrayList<>();

        // 2. Recorremos cada lista de IDs que nos llegó
        // (Ej. en la primera vuelta, 'grupoDeIds' será [1, 5, 12])
        for (List<Long> grupoDeIds : gruposDeIds) {

            // 3. Creamos una lista interna para guardar las noticias de este grupo
            List<Noticia> grupoDeNoticias = new ArrayList<>();

            // 4. Recorremos cada ID individual dentro de ese grupo
            // (Ej. 1, luego 5, luego 12)
            for (Long id : grupoDeIds) {

                // 5. ¡LA MAGIA! Buscamos la noticia en la BBDD por su ID
                Optional<Noticia> noticiaOpt = noticiaDAO.findById(id);

                // 6. 'findById' devuelve un 'Optional' (puede que exista o no)
                // Si la noticia existe...
                if (noticiaOpt.isPresent()) {
                    // ...la añadimos a nuestra lista interna 'grupoDeNoticias'
                    grupoDeNoticias.add(noticiaOpt.get());
                }
                // Si no existe (ej. te pasan un ID '999' que no existe),
                // simplemente lo ignoramos y no se añade a la lista.
            }

            // 7. Añadimos la lista interna (ya llena de Noticias) a la lista final
            resultadoFinal.add(grupoDeNoticias);
        }

        // 8. Devolvemos la lista de listas de Noticias.
        // Spring lo convertirá a JSON automáticamente.
        return resultadoFinal;
    }
}
