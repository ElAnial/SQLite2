package com.example.sqlite;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

public class EditarProductoActivity extends AppCompatActivity {

    private TextView tvTituloHeader, tvEstadoProductoLabel;
    private LinearLayout layoutEstadoProducto;
    private TextInputEditText etSku, etCodigoBarras, etNombre, etDescripcion, etUnidadMedida, etStockInicial, etStockActual, etStockMinimo;
    private Spinner spnCategoria;
    private MaterialButton btnGenerarSku, btnEscanearCodigo, btnToggleEstadoDetalle, btnGuardar, btnEliminar, btnVolver;

    private DBHelper dbHelper;
    private ArrayList<Categoria> listaCategorias;
    private ArrayAdapter<Categoria> adapterCategorias;

    private int idProducto = -1; // -1 indica creación de nuevo producto
    private int activo = 1;      // 1 = activo, 0 = deshabilitado
    private boolean ignorarAutoSkuWatcher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_producto);

        dbHelper = new DBHelper(this);

        tvTituloHeader = findViewById(R.id.tvTituloHeader);
        layoutEstadoProducto = findViewById(R.id.layoutEstadoProducto);
        tvEstadoProductoLabel = findViewById(R.id.tvEstadoProductoLabel);
        btnToggleEstadoDetalle = findViewById(R.id.btnToggleEstadoDetalle);

        etSku = findViewById(R.id.etSku);
        btnGenerarSku = findViewById(R.id.btnGenerarSku);
        etCodigoBarras = findViewById(R.id.etCodigoBarras);
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etUnidadMedida = findViewById(R.id.etUnidadMedida);
        etStockInicial = findViewById(R.id.etStockInicial);
        etStockActual = findViewById(R.id.etStockActual);
        etStockMinimo = findViewById(R.id.etStockMinimo);
        spnCategoria = findViewById(R.id.spnCategoria);

        btnEscanearCodigo = findViewById(R.id.btnEscanearCodigo);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnVolver = findViewById(R.id.btnVolver);

        listaCategorias = new ArrayList<>();
        adapterCategorias = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaCategorias);
        spnCategoria.setAdapter(adapterCategorias);

        cargarCategoriasSpinner();

        // Recuperar Extras del Intent o consulta desde DB si existe id_producto
        if (getIntent() != null && getIntent().hasExtra("id_producto")) {
            idProducto = getIntent().getIntExtra("id_producto", -1);
            tvTituloHeader.setText("Editar Producto");
            btnEliminar.setVisibility(View.VISIBLE);

            Producto p = dbHelper.obtenerProductoPorId(idProducto);
            if (p != null) {
                activo = p.getActivo();
                etSku.setText(p.getSkuCodigo());
                etCodigoBarras.setText(p.getCodigoBarras());
                etNombre.setText(p.getNombre());
                etDescripcion.setText(p.getDescripcion());
                etUnidadMedida.setText(p.getUnidadMedida() != null && !p.getUnidadMedida().isEmpty() ? p.getUnidadMedida() : "Unidad");
                etStockInicial.setText(String.valueOf(p.getStockInicial()));
                etStockInicial.setEnabled(false); // Stock inicial no se altera tras creación
                etStockActual.setText(String.valueOf(p.getStockActual()));
                etStockMinimo.setText(String.valueOf(p.getStockMinimo()));
                seleccionarCategoriaEnSpinner(p.getIdCategoria());
            } else {
                cargarDesdeExtras();
            }
        } else {
            tvTituloHeader.setText("Nuevo Producto");
            btnEliminar.setVisibility(View.GONE);
            activo = 1;
            if (getIntent() != null && getIntent().hasExtra("scanned_barcode")) {
                String barcode = getIntent().getStringExtra("scanned_barcode");
                if (barcode != null) {
                    etCodigoBarras.setText(barcode);
                }
            }
        }

        actualizarUiEstadoProducto();

        // Toggle rápido de habilitar / deshabilitar producto
        btnToggleEstadoDetalle.setOnClickListener(v -> toggleEstadoProducto());

        // Botón manual para autogenerar SKU y Código de Barras
        btnGenerarSku.setOnClickListener(v -> {
            generarSkuYCbAutomatico(true);
            Toast.makeText(this, "SKU y Código de Barras generados automáticamente", Toast.LENGTH_SHORT).show();
        });

        // Generación reactiva al escribir el nombre (solo en creación)
        etNombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (idProducto == -1 && !ignorarAutoSkuWatcher) {
                    generarSkuYCbAutomatico(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Generación reactiva al cambiar la categoría en el Spinner (solo en creación)
        spnCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (idProducto == -1 && !ignorarAutoSkuWatcher) {
                    generarSkuYCbAutomatico(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnEscanearCodigo.setOnClickListener(v -> escanearCodigoBarras());
        btnGuardar.setOnClickListener(v -> guardarOActualizar());
        btnEliminar.setOnClickListener(v -> eliminarBajaLogica());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void actualizarUiEstadoProducto() {
        if (idProducto == -1) {
            layoutEstadoProducto.setVisibility(View.GONE);
            return;
        }

        layoutEstadoProducto.setVisibility(View.VISIBLE);
        if (activo == 1) {
            tvEstadoProductoLabel.setText("Estado: ACTIVO (En Stock)");
            tvEstadoProductoLabel.setTextColor(Color.parseColor("#1565C0"));
            btnToggleEstadoDetalle.setText("Deshabilitar (Sin Stock)");
            btnToggleEstadoDetalle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            btnToggleEstadoDetalle.setTextColor(Color.parseColor("#C62828"));
        } else {
            tvEstadoProductoLabel.setText("Estado: DESHABILITADO (Sin Stock)");
            tvEstadoProductoLabel.setTextColor(Color.parseColor("#C62828"));
            btnToggleEstadoDetalle.setText("Habilitar");
            btnToggleEstadoDetalle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            btnToggleEstadoDetalle.setTextColor(Color.parseColor("#2E7D32"));
        }
    }

    private void toggleEstadoProducto() {
        if (idProducto == -1) return;

        int nuevoEstado = (activo == 1) ? 0 : 1;
        boolean exito = dbHelper.actualizarEstadoProducto(idProducto, nuevoEstado);
        if (exito) {
            activo = nuevoEstado;
            actualizarUiEstadoProducto();
            String msg = (nuevoEstado == 0)
                    ? "Producto deshabilitado (aparecerá como sin stock)"
                    : "Producto habilitado correctamente";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al cambiar el estado del producto", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Genera automáticamente el SKU relacionado al nombre y la categoría asignada,
     * garantizando que no se repita.
     * También genera automáticamente el código de barras relacionado al SKU.
     */
    private void generarSkuYCbAutomatico(boolean forzar) {
        if (idProducto != -1 && !forzar) {
            return;
        }

        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        if (nombre.isEmpty() && !forzar) {
            return;
        }

        String nombreCat = "";
        if (spnCategoria.getSelectedItem() != null) {
            Categoria cat = (Categoria) spnCategoria.getSelectedItem();
            nombreCat = cat.getNombreCategoria();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String skuGenerado = InventarioUtils.generarSkuUnico(db, nombre, nombreCat, idProducto);

        ignorarAutoSkuWatcher = true;
        etSku.setText(skuGenerado);

        String cbGenerado = InventarioUtils.generarCodigoBarrasDesdeSku(db, skuGenerado, idProducto);
        etCodigoBarras.setText(cbGenerado);
        ignorarAutoSkuWatcher = false;
    }

    private void cargarDesdeExtras() {
        if (getIntent() == null) return;
        String sku = getIntent().getStringExtra("sku_codigo");
        String cb = getIntent().getStringExtra("codigo_barras");
        String nombre = getIntent().getStringExtra("nombre");
        String desc = getIntent().getStringExtra("descripcion");
        String um = getIntent().getStringExtra("unidad_medida");
        int stIni = getIntent().getIntExtra("stock_inicial", 0);
        int stockAct = getIntent().getIntExtra("stock_actual", 0);
        int stockMin = getIntent().getIntExtra("stock_minimo", 0);
        int idCat = getIntent().getIntExtra("id_categoria", -1);
        activo = getIntent().getIntExtra("activo", 1);

        etSku.setText(sku);
        if (cb != null) etCodigoBarras.setText(cb);
        etNombre.setText(nombre);
        etDescripcion.setText(desc);
        etUnidadMedida.setText(um != null && !um.isEmpty() ? um : "Unidad");
        etStockInicial.setText(String.valueOf(stIni));
        etStockActual.setText(String.valueOf(stockAct));
        etStockMinimo.setText(String.valueOf(stockMin));
        seleccionarCategoriaEnSpinner(idCat);
    }

    private void escanearCodigoBarras() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Enfoque el código de barras");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                etCodigoBarras.setText(result.getContents());
                Toast.makeText(this, "Código detectado: " + result.getContents(), Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void cargarCategoriasSpinner() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        listaCategorias.clear();

        try (Cursor c = db.rawQuery("SELECT id_categoria, nombre_categoria, descripcion, activo FROM categoria WHERE activo = 1 ORDER BY nombre_categoria ASC", null)) {
            if (c.moveToFirst()) {
                int idIdx = c.getColumnIndexOrThrow("id_categoria");
                int nomIdx = c.getColumnIndexOrThrow("nombre_categoria");
                int descIdx = c.getColumnIndexOrThrow("descripcion");

                do {
                    Categoria cat = new Categoria();
                    cat.setIdCategoria(c.getInt(idIdx));
                    cat.setNombreCategoria(c.getString(nomIdx));
                    cat.setDescripcion(c.getString(descIdx));
                    cat.setActivo(1);
                    listaCategorias.add(cat);
                } while (c.moveToNext());
            }
            adapterCategorias.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar categorías: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    private void seleccionarCategoriaEnSpinner(int idCategoriaTarget) {
        for (int i = 0; i < listaCategorias.size(); i++) {
            if (listaCategorias.get(i).getIdCategoria() == idCategoriaTarget) {
                spnCategoria.setSelection(i);
                break;
            }
        }
    }

    private void guardarOActualizar() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String sku = etSku.getText() != null ? etSku.getText().toString().trim() : "";
        String codigoBarras = etCodigoBarras.getText() != null ? etCodigoBarras.getText().toString().trim() : "";
        String desc = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        String unidadMedida = etUnidadMedida.getText() != null ? etUnidadMedida.getText().toString().trim() : "Unidad";
        String stockIniStr = etStockInicial.getText() != null ? etStockInicial.getText().toString().trim() : "";
        String stockActStr = etStockActual.getText() != null ? etStockActual.getText().toString().trim() : "";
        String stockMinStr = etStockMinimo.getText() != null ? etStockMinimo.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Ingrese el nombre del producto");
            return;
        }

        // Si el SKU está vacío, se autogenera de inmediato
        if (TextUtils.isEmpty(sku)) {
            generarSkuYCbAutomatico(true);
            sku = etSku.getText() != null ? etSku.getText().toString().trim() : "";
            if (TextUtils.isEmpty(sku)) {
                etSku.setError("Ingrese o genere el código SKU");
                return;
            }
        }

        // Si el código de barras está vacío, se autogenera desde el SKU
        if (TextUtils.isEmpty(codigoBarras)) {
            SQLiteDatabase rdb = dbHelper.getReadableDatabase();
            codigoBarras = InventarioUtils.generarCodigoBarrasDesdeSku(rdb, sku, idProducto);
            etCodigoBarras.setText(codigoBarras);
        }

        if (idProducto == -1 && TextUtils.isEmpty(stockIniStr)) {
            etStockInicial.setError("Ingrese el stock inicial");
            return;
        }

        if (TextUtils.isEmpty(stockActStr)) {
            etStockActual.setError("Ingrese el stock actual");
            return;
        }

        if (TextUtils.isEmpty(stockMinStr)) {
            etStockMinimo.setError("Ingrese el stock mínimo");
            return;
        }

        int stockInicial = idProducto == -1 ? Integer.parseInt(stockIniStr) : 0;
        int stockActual = Integer.parseInt(stockActStr);
        int stockMinimo = Integer.parseInt(stockMinStr);

        if (stockInicial < 0) {
            etStockInicial.setError("El stock inicial no puede ser negativo");
            return;
        }

        if (stockActual < 0) {
            etStockActual.setError("El stock actual no puede ser negativo");
            return;
        }

        if (stockMinimo < 0) {
            etStockMinimo.setError("El stock mínimo no puede ser negativo");
            return;
        }

        if (spnCategoria.getSelectedItem() == null) {
            Toast.makeText(this, "Debe registrar o seleccionar una categoría válida", Toast.LENGTH_SHORT).show();
            return;
        }

        Categoria categoriaSeleccionada = (Categoria) spnCategoria.getSelectedItem();
        int idCategoria = categoriaSeleccionada.getIdCategoria();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Validar unicidad de SKU
            String checkSkuSql = "SELECT COUNT(*) FROM producto WHERE sku_codigo = ? AND id_producto != ?";
            try (Cursor c = db.rawQuery(checkSkuSql, new String[]{sku, String.valueOf(idProducto)})) {
                if (c.moveToFirst() && c.getInt(0) > 0) {
                    etSku.setError("Este código SKU ya está registrado en otro producto");
                    return;
                }
            }

            // Validar unicidad de Código de Barras (si se proporcionó)
            if (!TextUtils.isEmpty(codigoBarras)) {
                String checkCbSql = "SELECT COUNT(*) FROM producto WHERE codigo_barras = ? AND id_producto != ?";
                try (Cursor c = db.rawQuery(checkCbSql, new String[]{codigoBarras, String.valueOf(idProducto)})) {
                    if (c.moveToFirst() && c.getInt(0) > 0) {
                        etCodigoBarras.setError("Este código de barras ya está asignado a otro producto");
                        return;
                    }
                }
            }

            if (idProducto == -1) {
                // Nuevo producto
                ContentValues cv = new ContentValues();
                cv.put("sku_codigo", sku);
                cv.put("codigo_barras", codigoBarras);
                cv.put("nombre", nombre);
                cv.put("descripcion", desc);
                cv.put("unidad_medida", TextUtils.isEmpty(unidadMedida) ? "Unidad" : unidadMedida);
                cv.put("stock_inicial", stockInicial);
                cv.put("stock_actual", stockActual);
                cv.put("stock_minimo", stockMinimo);
                cv.put("id_categoria", idCategoria);
                cv.put("activo", 1);

                long newId = db.insert("producto", null, cv);
                if (newId != -1) {
                    Toast.makeText(this, "Producto registrado correctamente (SKU: " + sku + ")", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error al registrar producto", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Actualizar producto
                ContentValues cv = new ContentValues();
                cv.put("sku_codigo", sku);
                cv.put("codigo_barras", codigoBarras);
                cv.put("nombre", nombre);
                cv.put("descripcion", desc);
                cv.put("unidad_medida", TextUtils.isEmpty(unidadMedida) ? "Unidad" : unidadMedida);
                cv.put("stock_actual", stockActual);
                cv.put("stock_minimo", stockMinimo);
                cv.put("id_categoria", idCategoria);
                cv.put("activo", activo);

                int filasAfectadas = db.update("producto", cv, "id_producto = ?", new String[]{String.valueOf(idProducto)});
                if (filasAfectadas > 0) {
                    Toast.makeText(this, "Producto actualizado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "No se pudo actualizar el producto", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando producto: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    private void eliminarBajaLogica() {
        if (idProducto == -1) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String sqlBaja = "UPDATE producto SET activo = 0 WHERE id_producto = ?";
            try (SQLiteStatement stmt = db.compileStatement(sqlBaja)) {
                stmt.bindLong(1, idProducto);
                int filas = stmt.executeUpdateDelete();
                if (filas > 0) {
                    activo = 0;
                    Toast.makeText(this, "Producto deshabilitado (dado de baja)", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "No se encontró el producto", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al dar de baja: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }
}
