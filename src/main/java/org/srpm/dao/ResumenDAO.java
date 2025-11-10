package org.srpm.dao;

import org.srpm.model.Noticia;
import org.srpm.model.Resumen;

import java.util.List;
import java.util.Optional;

public interface ResumenDAO {
    /**
     * Guarda una noticia (nueva o actualizada).
     */
    void save(Resumen resumen);

    void deleteById(Long id);

    /**
     * Devuelve todas las noticias que tenemos guardadas.
     */
    List<Resumen> findAll();

    /**
     * Busca una noticia por el ID asignado
     */
    Optional<Resumen> findById(Long id);
}
