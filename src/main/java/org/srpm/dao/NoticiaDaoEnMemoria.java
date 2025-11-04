package org.srpm.dao;

import org.srpm.model.Noticia;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class NoticiaDaoEnMemoria implements NoticiaDAO {

    // Mapa para buscar por LINK (para evitar duplicados)
    private final ConcurrentHashMap<String, Noticia> noticiasPorLink = new ConcurrentHashMap<>();

    // Mapa para buscar por ID (para el nuevo controlador)
    private final ConcurrentHashMap<Long, Noticia> noticiasPorId = new ConcurrentHashMap<>();

    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Optional<Noticia> findByLinkNoticia(String linkNoticia) {
        return Optional.ofNullable(noticiasPorLink.get(linkNoticia));
    }

    @Override
    public void save(Noticia noticia) {
 
        // --- ¡¡AQUÍ ESTÁ EL ARREGLO!! ---
        // Volvemos a comprobar si es '0' (el valor por defecto de 'long')
        if (noticia.getId() == 0L) { // 0L es más explícito para tipo 'long'
            noticia.setId(idCounter.getAndIncrement());
        }

        if (noticia.getFecha() == null) {
            noticia.setFecha(LocalDateTime.now());
        }

        // Guarda la noticia en AMBOS mapas
        noticiasPorLink.put(noticia.getLinkNoticia(), noticia);
        noticiasPorId.put(noticia.getId(), noticia);
    }

    /**
     * Devuelve todas las noticias que tenemos guardadas.
     */
    @Override
    public List<Noticia> findAll() {
        return new ArrayList<>(noticiasPorId.values());
    }

    /**
     * Busca una noticia por su ID numérico (de BBDD).
     */
    @Override
    public Optional<Noticia> findById(Long id) {
        return Optional.ofNullable(noticiasPorId.get(id));
    }
}