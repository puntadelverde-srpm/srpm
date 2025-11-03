package org.srpm.model;

public class Resumen {
    private String titular;
    private String cuerpo;
    private Integer id;

    public Resumen(String titular, String cuerpo, Integer id) {
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
