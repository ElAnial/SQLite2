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

    /**
     * Valida si un RUT tiene formato válido (hasta 8 dígitos base, guión, y 1 dígito verificador 0-9 o K).
     */
    public static boolean validarFormatoRut(String rut) {
        if (rut == null) return false;
        String limpio = rut.trim().toUpperCase(Locale.ROOT);
        String sinPuntos = limpio.replace(".", "");
        if (!sinPuntos.contains("-")) return false;
        String[] partes = sinPuntos.split("-");
        if (partes.length != 2) return false;

        String cuerpo = partes[0];
        String dv = partes[1];

        // Cuerpo: entre 1 y 8 dígitos numéricos
        if (!cuerpo.matches("^\\d{1,8}$")) return false;
        // DV: exactamente 1 caracter (0-9 o K)
        if (!dv.matches("^[0-9K]$")) return false;

        return true;
    }

    /**
     * Formatea un RUT al estándar XX.XXX.XXX-X o X.XXX.XXX-X.
     */
    public static String formatearRut(String rut) {
        if (rut == null) return "";
        String limpio = rut.trim().toUpperCase(Locale.ROOT).replace(".", "").replace(" ", "");
        if (!limpio.contains("-")) return limpio;
        String[] partes = limpio.split("-");
        if (partes.length != 2) return limpio;

        String cuerpo = partes[0].replaceAll("\\D", "");
        String dv = partes[1].replaceAll("[^0-9K]", "");
        if (cuerpo.isEmpty() || dv.isEmpty()) return limpio;

        if (cuerpo.length() > 8) {
            cuerpo = cuerpo.substring(cuerpo.length() - 8);
        }
        if (dv.length() > 1) {
            dv = dv.substring(0, 1);
        }

        StringBuilder sb = new StringBuilder();
        int len = cuerpo.length();
        for (int i = 0; i < len; i++) {
            sb.append(cuerpo.charAt(i));
            int resto = len - 1 - i;
            if (resto > 0 && resto % 3 == 0) {
                sb.append(".");
            }
        }
        sb.append("-").append(dv);
        return sb.toString();
    }

    /**
     * Calcula el Dígito Verificador de un RUT chileno usando el algoritmo del Módulo 11.
     */
    public static String calcularDigitoVerificador(String cuerpoStr) {
        if (cuerpoStr == null || cuerpoStr.trim().isEmpty()) return "";
        try {
            int cuerpo = Integer.parseInt(cuerpoStr.trim().replaceAll("\\D", ""));
            int suma = 0;
            int multiplicador = 2;
            while (cuerpo > 0) {
                suma += (cuerpo % 10) * multiplicador;
                cuerpo /= 10;
                multiplicador++;
                if (multiplicador > 7) {
                    multiplicador = 2;
                }
            }
            int resto = suma % 11;
            int resultado = 11 - resto;
            if (resultado == 11) {
                return "0";
            } else if (resultado == 10) {
                return "K";
            } else {
                return String.valueOf(resultado);
            }
        } catch (NumberFormatException e) {
            return "";
        }
    }
}

