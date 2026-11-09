package com.example.sqlite;

import org.junit.Test;
import static org.junit.Assert.*;

public class InventarioUtilsTest {

    @Test
    public void testLimpiarTexto() {
        assertEquals("ELECTRONICA", InventarioUtils.limpiarTexto("Electrónica"));
        assertEquals("LINEABLANCA", InventarioUtils.limpiarTexto("Línea Blanca"));
        assertEquals("TEST123", InventarioUtils.limpiarTexto(" Test-123! "));
    }

    @Test
    public void testPrefijos() {
        assertEquals("ELE", InventarioUtils.obtenerPrefijoCategoria("Electrónica"));
        assertEquals("HER", InventarioUtils.obtenerPrefijoCategoria("Herramientas"));
        assertEquals("OFI", InventarioUtils.obtenerPrefijoCategoria("Oficina"));
        assertEquals("LAP", InventarioUtils.obtenerPrefijoProducto("Laptop Lenovo 15"));
        assertEquals("MAR", InventarioUtils.obtenerPrefijoProducto("Martillo carpintero"));
    }

    @Test
    public void testSkuSinDb() {
        String sku = InventarioUtils.generarSkuUnico(null, "Laptop Lenovo", "Electrónica", -1);
        assertEquals("ELE-LAP-001", sku);
    }

    @Test
    public void testCalcularDigitoVerificadorEAN13() {
        // Ejemplo: 789123456789 -> el dígito verificador es 5
        int digito = InventarioUtils.calcularDigitoVerificadorEAN13("789123456789");
        assertEquals(5, digito);

        // Ejemplo estándar EAN-13: 400638133393 -> 1
        int digito2 = InventarioUtils.calcularDigitoVerificadorEAN13("400638133393");
        assertEquals(1, digito2);
    }

    @Test
    public void testCodigoBarrasRelacionadoASku() {
        String sku = "ELE-LAP-001";
        String cb1 = InventarioUtils.generarCodigoBarrasDesdeSku(null, sku, -1);
        String cb2 = InventarioUtils.generarCodigoBarrasDesdeSku(null, sku, -1);

        assertNotNull(cb1);
        assertEquals(13, cb1.length());
        assertTrue(cb1.startsWith("780"));
        // El mismo SKU genera el mismo código de barras base si no hay colisiones
        assertEquals(cb1, cb2);

        // Verificar validez matemática del dígito verificador
        String primeros12 = cb1.substring(0, 12);
        int digitoEsperado = InventarioUtils.calcularDigitoVerificadorEAN13(primeros12);
        assertEquals(String.valueOf(digitoEsperado), cb1.substring(12));
    }
}
