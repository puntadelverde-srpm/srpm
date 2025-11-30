package org.srpm.model;

public class Resumen {
    private String titular;
    private String cuerpo;
    private Long id;

    public Resumen() {}

    public Resumen(String titular, String cuerpo, Long id) {
        this.titular = titular;
        this.cuerpo = cuerpo;
        this.id = id;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
