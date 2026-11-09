package com.example.sqlite;

/**
 * Modelo de datos para representar un evento/registro en el Historial de Auditoría.
 */
public class RegistroAudit {
    private String tipoAccion; // Ej. ENTRADA, SALIDA, DESHABILITADO, CATEGORÍA, PROVEEDOR, PRODUCTO
    private String detalle;    // Descripción detallada del evento u operación
    private String usuario;    // Usuario que ejecutó la acción o "Sistema"
    private String fechaHora;  // Fecha y hora del registro

    public RegistroAudit() {}

    public RegistroAudit(String tipoAccion, String detalle, String usuario, String fechaHora) {
        this.tipoAccion = tipoAccion;
        this.detalle = detalle;
        this.usuario = usuario;
        this.fechaHora = fechaHora;
    }

    public String getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(String tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }
}
