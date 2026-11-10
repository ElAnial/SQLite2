package com.example.sqlite;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.List;

public class EntradasActivity extends AppCompatActivity {

    private Spinner spnProductoEntrada, spnProveedorEntrada;
    private TextView tvProductoDetalle, tvStockDisponible;
    private TextInputEditText etCantidadEntrada, etDocumentoEntrada, etObservacionEntrada;
    private MaterialButton btnEscanearEntrada, btnConfirmarEntrada, btnVolverEntradas;

    private DBHelper dbHelper;
    private SessionManager session;
    private List<Producto> listaProductos;
    private List<Proveedor> listaProveedores;
    private ArrayAdapter<Producto> adapterProductos;
    private ArrayAdapter<String> adapterProveedores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Roles y permisos: Consulta (3) no puede realizar transacciones
        if (!session.isLoggedIn()) {
            finish();
            return;
        }

        if ((session.getRoleId() != 1 && session.getRoleId() != 2)) {
            Toast.makeText(this, "Acceso no permitido para usuario de consulta", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_entradas);

        dbHelper = new DBHelper(this);

        spnProductoEntrada = findViewById(R.id.spnProductoEntrada);
        spnProveedorEntrada = findViewById(R.id.spnProveedorEntrada);
        tvProductoDetalle = findViewById(R.id.tvProductoDetalle);
        tvStockDisponible = findViewById(R.id.tvStockDisponible);

        etCantidadEntrada = findViewById(R.id.etCantidadEntrada);
        etDocumentoEntrada = findViewById(R.id.etDocumentoEntrada);
        etObservacionEntrada = findViewById(R.id.etObservacionEntrada);

        btnEscanearEntrada = findViewById(R.id.btnEscanearEntrada);
        btnConfirmarEntrada = findViewById(R.id.btnConfirmarEntrada);
        btnVolverEntradas = findViewById(R.id.btnVolverEntradas);

        listaProductos = new ArrayList<>();
        listaProveedores = new ArrayList<>();

        adapterProductos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaProductos);
        spnProductoEntrada.setAdapter(adapterProductos);

        btnVolverEntradas.setOnClickListener(v -> finish());
        btnEscanearEntrada.setOnClickListener(v -> escanearCodigoBarras());
        btnConfirmarEntrada.setOnClickListener(v -> confirmarEntrada());

        spnProductoEntrada.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarDetalleProductoSeleccionado();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        cargarProductos();
        cargarProveedores();
    }

    private void escanearCodigoBarras() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Escanee el código de barras del producto");
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
                String barcode = result.getContents();
                seleccionarProductoPorBarcode(barcode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void seleccionarProductoPorBarcode(String barcode) {
        Producto p = dbHelper.obtenerProductoPorCodigoBarras(barcode);
        if (p == null) {
            Toast.makeText(this, "Producto no encontrado con código: " + barcode, Toast.LENGTH_LONG).show();
            return;
        }

        if (p.getActivo() == 0) {
            Toast.makeText(this, "El producto '" + p.getNombre() + "' se encuentra deshabilitado", Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < listaProductos.size(); i++) {
            if (listaProductos.get(i).getIdProducto() == p.getIdProducto()) {
                spnProductoEntrada.setSelection(i);
                Toast.makeText(this, "Producto seleccionado: " + p.getNombre(), Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private void cargarProductos() {
        listaProductos.clear();
        listaProductos.addAll(dbHelper.obtenerProductosActivos());
        adapterProductos.notifyDataSetChanged();
        if (!listaProductos.isEmpty()) {
            spnProductoEntrada.setSelection(0);
            actualizarDetalleProductoSeleccionado();
        } else {
            tvProductoDetalle.setText("No hay productos registrados o activos");
            tvStockDisponible.setText("Stock: -");
        }
    }

    private void cargarProveedores() {
        listaProveedores.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> nombresProveedores = new ArrayList<>();
        nombresProveedores.add("Sin Proveedor Especificado");

        try (Cursor c = db.rawQuery("SELECT id_proveedor, rut_empresa, razon_social, contacto FROM proveedor ORDER BY razon_social ASC", null)) {
            if (c.moveToFirst()) {
                int idIdx = c.getColumnIndexOrThrow("id_proveedor");
                int rutIdx = c.getColumnIndexOrThrow("rut_empresa");
                int razonIdx = c.getColumnIndexOrThrow("razon_social");
                int contactoIdx = c.getColumnIndexOrThrow("contacto");

                do {
                    Proveedor prov = new Proveedor();
                    prov.setIdProveedor(c.getInt(idIdx));
                    prov.setRutEmpresa(c.getString(rutIdx));
                    prov.setRazonSocial(c.getString(razonIdx));
                    prov.setContacto(c.getString(contactoIdx));
                    listaProveedores.add(prov);
                    nombresProveedores.add(prov.getRazonSocial() + " (" + prov.getRutEmpresa() + ")");
                } while (c.moveToNext());
            }

            adapterProveedores = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresProveedores);
            spnProveedorEntrada.setAdapter(adapterProveedores);

        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar proveedores: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarDetalleProductoSeleccionado() {
        Producto p = (Producto) spnProductoEntrada.getSelectedItem();
        if (p != null) {
            tvProductoDetalle.setText("Producto: " + p.getNombre() + " | SKU: " + p.getSkuCodigo());
            tvStockDisponible.setText("Stock Actual: " + p.getStockActual() + " " + p.getUnidadMedida());
        }
    }

    private void confirmarEntrada() {
        Producto p = (Producto) spnProductoEntrada.getSelectedItem();
        if (p == null) {
            Toast.makeText(this, "Seleccione un producto válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String cantStr = etCantidadEntrada.getText() != null ? etCantidadEntrada.getText().toString().trim() : "";
        if (TextUtils.isEmpty(cantStr)) {
            etCantidadEntrada.setError("Ingrese la cantidad recibida");
            return;
        }

        int cantidad = 0;
        try {
            cantidad = Integer.parseInt(cantStr);
        } catch (NumberFormatException e) {
            etCantidadEntrada.setError("Cantidad debe ser un número entero válido");
            return;
        }

        if (cantidad <= 0) {
            etCantidadEntrada.setError("La cantidad debe ser mayor que cero");
            return;
        }

        Integer idProveedor = null;
        int provIndex = spnProveedorEntrada.getSelectedItemPosition();
        if (provIndex > 0 && provIndex - 1 < listaProveedores.size()) {
            idProveedor = listaProveedores.get(provIndex - 1).getIdProveedor();
        }

        String documento = etDocumentoEntrada.getText() != null ? etDocumentoEntrada.getText().toString().trim() : "";
        String observacion = etObservacionEntrada.getText() != null ? etObservacionEntrada.getText().toString().trim() : "";
        int idUsuario = Integer.parseInt(session.getUserDetails().get(SessionManager.KEY_USER_ID));

        boolean exito = dbHelper.registrarEntradaTransaccional(p.getIdProducto(), cantidad, idUsuario, idProveedor, documento, observacion);

        if (exito) {
            Toast.makeText(this, "Entrada de stock registrada exitosamente", Toast.LENGTH_SHORT).show();
            etCantidadEntrada.setText("");
            etDocumentoEntrada.setText("");
            etObservacionEntrada.setText("");
            etCantidadEntrada.clearFocus();
            cargarProductos(); // Recargar productos para reflejar nuevo stock
        } else {
            Toast.makeText(this, "Error al registrar la entrada (Transacción cancelada)", Toast.LENGTH_LONG).show();
        }
    }
}