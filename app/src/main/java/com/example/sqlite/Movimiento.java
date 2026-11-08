package com.example.sqlite;

public class Movimiento {
    private int idMovimiento;
    private int idProducto;
    private String nombreProducto;
    private String skuProducto;
    private String tipo; // 'ENTRADA' o 'SALIDA'
    private int cantidad;
    private String fechaHora;
    private int idUsuario;
    private String nombreUsuario;
    private Integer idProveedor;
    private String razonSocialProveedor;
    private String documento;
    private String motivo;
    private String destinatario;
    private String observacion;

    public Movimiento() {}

    public Movimiento(int idMovimiento, int idProducto, String nombreProducto, String skuProducto, String tipo, int cantidad, String fechaHora, int idUsuario, String nombreUsuario, Integer idProveedor, String razonSocialProveedor, String documento, String motivo, String destinatario, String observacion) {
        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.skuProducto = skuProducto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fechaHora = fechaHora;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.idProveedor = idProveedor;
        this.razonSocialProveedor = razonSocialProveedor;
        this.documento = documento;
        this.motivo = motivo;
        this.destinatario = destinatario;
        this.observacion = observacion;
    }

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getSkuProducto() { return skuProducto; }
    public void setSkuProducto(String skuProducto) { this.skuProducto = skuProducto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }

    public String getRazonSocialProveedor() { return razonSocialProveedor; }
    public void setRazonSocialProveedor(String razonSocialProveedor) { this.razonSocialProveedor = razonSocialProveedor; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
