package com.example.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.Normalizer;
import java.util.Locale;

public class InventarioUtils {

    /**
     * Limpia un texto removiendo acentos, espacios y caracteres no alfanuméricos.
     */
    public static String limpiarTexto(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        String sinAcentos = normalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return sinAcentos.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT).trim();
    }

    /**
     * Genera un prefijo para la categoría (ej. Electrónica -> ELE).
     */
    public static String obtenerPrefijoCategoria(String nombreCategoria) {
        String limpio = limpiarTexto(nombreCategoria);
        if (limpio.isEmpty()) return "CAT";
        if (limpio.length() < 3) {
            StringBuilder sb = new StringBuilder(limpio);
            while (sb.length() < 3) sb.append("X");
            return sb.toString();
        }
        return limpio.substring(0, 3);
    }

    /**
     * Genera un prefijo para el producto (ej. Laptop Lenovo -> LAP).
     */
    public static String obtenerPrefijoProducto(String nombreProducto) {
        if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
            return "PROD";
        }
        String[] palabras = nombreProducto.trim().split("\\s+");
        String primeraLimpia = limpiarTexto(palabras[0]);
        if (primeraLimpia.length() >= 3) {
            return primeraLimpia.substring(0, 3);
        }

        StringBuilder prefijo = new StringBuilder();
        for (String palabra : palabras) {
            String limpia = limpiarTexto(palabra);
            if (!limpia.isEmpty() && prefijo.length() < 3) {
                prefijo.append(limpia.charAt(0));
            }
        }
        while (prefijo.length() < 3) {
            prefijo.append("X");
        }
        return prefijo.toString();
    }

    /**
     * Genera un SKU automático único basado en Nombre y Categoría.
     * Formato: [CAT]-[NOM]-[CORRELATIVO] (ej. ELE-LAP-001)
     * Garantiza que NO se repita consultando la base de datos SQLite.
     */
    public static String generarSkuUnico(SQLiteDatabase db, String nombreProducto, String nombreCategoria, int idProductoExcluir) {
        String prefijoCat = obtenerPrefijoCategoria(nombreCategoria);
        String prefijoProd = obtenerPrefijoProducto(nombreProducto);
        String baseSku = prefijoCat + "-" + prefijoProd + "-";

        int siguienteNumero = 1;
        if (db != null && db.isOpen()) {
            String sql = "SELECT sku_codigo FROM producto WHERE sku_codigo LIKE ? AND id_producto != ?";
            try (Cursor c = db.rawQuery(sql, new String[]{baseSku + "%", String.valueOf(idProductoExcluir)})) {
                while (c.moveToNext()) {
                    String skuExistente = c.getString(0);
                    if (skuExistente != null && skuExistente.startsWith(baseSku)) {
                        String sufijo = skuExistente.substring(baseSku.length());
                        try {
                            int num = Integer.parseInt(sufijo);
                            if (num >= siguienteNumero) {
                                siguienteNumero = num + 1;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        String skuGenerado = String.format(Locale.ROOT, "%s%03d", baseSku, siguienteNumero);
        if (db != null && db.isOpen()) {
            while (existeSku(db, skuGenerado, idProductoExcluir)) {
                siguienteNumero++;
                skuGenerado = String.format(Locale.ROOT, "%s%03d", baseSku, siguienteNumero);
            }
        }
        return skuGenerado;
    }

    public static boolean existeSku(SQLiteDatabase db, String sku, int idProductoExcluir) {
        if (db == null || !db.isOpen() || sku == null) return false;
        String sql = "SELECT COUNT(*) FROM producto WHERE sku_codigo = ? AND id_producto != ?";
        try (Cursor c = db.rawQuery(sql, new String[]{sku.trim(), String.valueOf(idProductoExcluir)})) {
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Genera un código de barras EAN-13 válido (13 dígitos) relacionado al SKU.
     * Garantiza unicidad en la tabla producto.
     */
    public static String generarCodigoBarrasDesdeSku(SQLiteDatabase db, String sku, int idProductoExcluir) {
        if (sku == null || sku.trim().isEmpty()) {
            sku = "PROD-001";
        }

        // Generamos un hash numérico positivo derivado directamente del SKU
        long numHash = Math.abs((long) sku.hashCode());
        long semilla9Digitos = numHash % 1_000_000_000L;

        int offset = 0;
        String codigoBarrasFinal = null;

        while (codigoBarrasFinal == null || (db != null && db.isOpen() && existeCodigoBarras(db, codigoBarrasFinal, idProductoExcluir))) {
            long numeroAjustado = (semilla9Digitos + offset) % 1_000_000_000L;
            // Prefijo 780 (o 200) + 9 dígitos de semilla = 12 dígitos de datos
            String datos12 = String.format(Locale.ROOT, "780%09d", numeroAjustado);
            int digitoVerificador = calcularDigitoVerificadorEAN13(datos12);
            codigoBarrasFinal = datos12 + digitoVerificador;
            offset++;
        }

        return codigoBarrasFinal;
    }

    public static boolean existeCodigoBarras(SQLiteDatabase db, String codigoBarras, int idProductoExcluir) {
        if (db == null || !db.isOpen() || codigoBarras == null) return false;
        String sql = "SELECT COUNT(*) FROM producto WHERE codigo_barras = ? AND id_producto != ?";
        try (Cursor c = db.rawQuery(sql, new String[]{codigoBarras.trim(), String.valueOf(idProductoExcluir)})) {
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Calcula el dígito verificador estándar EAN-13 para una cadena de 12 dígitos numéricos.
     */
    public static int calcularDigitoVerificadorEAN13(String doceDigitos) {
        if (doceDigitos == null || doceDigitos.length() != 12) {
            return 0;
        }
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(doceDigitos.charAt(i));
            if (i % 2 == 1) {
                suma += digito * 3;
            } else {
                suma += digito;
            }
        }
        int residuo = suma % 10;
        return (residuo == 0) ? 0 : (10 - residuo);
    }
}

