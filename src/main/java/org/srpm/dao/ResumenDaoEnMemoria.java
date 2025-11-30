package org.srpm.dao;

import org.springframework.stereotype.Repository;
import org.srpm.model.Resumen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
@Repository
public class ResumenDaoEnMemoria implements ResumenDAO{


    private final ConcurrentHashMap<Long, Resumen> resumenPorId = new ConcurrentHashMap<>();

    private final AtomicLong idCounter = new AtomicLong(1);


    @Override
    public void save(Resumen resumen) {

        if (resumen.getId()==null|| resumen.getId() == 0L) { // 0L es más explícito para tipo 'long'
            resumen.setId(idCounter.getAndIncrement());
        }

        resumenPorId.put(resumen.getId(), resumen);
    }

    @Override
    public void deleteById(Long id) {
        resumenPorId.remove(id);
    }

    /**
     * Devuelve todas las noticias que tenemos guardadas.
     */
    @Override
    public List<Resumen> findAll() {
        return new ArrayList<>(resumenPorId.values());
    }

    /**
     * Busca una noticia por su ID numérico (de BBDD).
     */
    @Override
    public Optional<Resumen> findById(Long id) {
        return Optional.ofNullable(resumenPorId.get(id));
    }

    @Override
    public void deleteAll() {
        resumenPorId.clear();
        idCounter.set(1);
    }

}
