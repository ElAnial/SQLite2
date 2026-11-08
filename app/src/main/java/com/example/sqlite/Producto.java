package com.example.sqlite;

public class Producto {
    private int idProducto;
    private String skuCodigo;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private int stockInicial;
    private int stockActual;
    private int stockMinimo;
    private int idCategoria;
    private String nombreCategoria;
    private int activo;

    public Producto() {}

    public Producto(int idProducto, String skuCodigo, String nombre, String descripcion, int stockActual, int stockMinimo, int idCategoria, String nombreCategoria, int activo) {
        this(idProducto, skuCodigo, "", nombre, descripcion, "Unidad", stockActual, stockActual, stockMinimo, idCategoria, nombreCategoria, activo);
    }

    public Producto(int idProducto, String skuCodigo, String codigoBarras, String nombre, String descripcion, String unidadMedida, int stockInicial, int stockActual, int stockMinimo, int idCategoria, String nombreCategoria, int activo) {
        this.idProducto = idProducto;
        this.skuCodigo = skuCodigo;
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.unidadMedida = unidadMedida;
        this.stockInicial = stockInicial;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.idCategoria = idCategoria;
        this.nombreCategoria = nombreCategoria;
        this.activo = activo;
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getSkuCodigo() { return skuCodigo; }
    public void setSkuCodigo(String skuCodigo) { this.skuCodigo = skuCodigo; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public int getStockInicial() { return stockInicial; }
    public void setStockInicial(int stockInicial) { this.stockInicial = stockInicial; }

    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }

    public int getActivo() { return activo; }
    public void setActivo(int activo) { this.activo = activo; }

    public boolean isStockBajo() {
        return stockActual <= stockMinimo;
    }

    @Override
    public String toString() {
        return nombre + " (SKU: " + skuCodigo + ")";
    }
}

