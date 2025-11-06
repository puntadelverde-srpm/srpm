package org.srpm.dao;

import org.srpm.model.Noticia;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz para acceder a los datos de las noticias.
 * Define QUÉ se puede hacer, pero no CÓMO.
 */
public interface NoticiaDAO {

    /**
     * Busca una noticia por su link (ID del RSS).
     */
    Optional<Noticia> findByLinkNoticia(String linkNoticia);

    /**
     * Guarda una noticia (nueva o actualizada).
     */
    void save(Noticia noticia);

    /**
     * Devuelve todas las noticias que tenemos guardadas.
     */
    List<Noticia> findAll();

    /**
     * Busca una noticia por el ID asignado
     */
    Optional<Noticia> findById(Long id);
}