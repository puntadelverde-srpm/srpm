package org.srpm.model;

import java.time.LocalDate;

public class Noticia {
    private long id=0;
    private String fuente;
    private String titular;
    private String linkNoticia;
    private LocalDate fecha;

    public Noticia(String fuente, String titular, String linkNoticia, LocalDate fecha) {
        this.fuente = fuente;
        this.titular = titular;
        this.linkNoticia = linkNoticia;
        this.id++;
        this.fecha = fecha;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getLinkNoticia() {
        return linkNoticia;
    }

    public void setLinkNoticia(String linkNoticia) {
        this.linkNoticia = linkNoticia;
    }
    public LocalDate getFecha() {
        return fecha;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
