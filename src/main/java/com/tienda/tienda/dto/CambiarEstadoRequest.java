package com.tienda.tienda.dto;

import com.tienda.tienda.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public class CambiarEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoPedido estado;

    public CambiarEstadoRequest() {
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }
}
