package com.example.sqlite;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TiendaProductosActivity extends AppCompatActivity {

    private TextInputEditText etBuscarTienda;
    private MaterialButton btnEscanearTienda, btnCerrarSesionTienda;
    private Chip chipSoloEnStock;
    private ListView lstTiendaProductos;
    private TextView tvTiendaSubt;

    private DBHelper dbHelper;
    private SessionManager session;
    private ArrayList<Producto> listaProductos;
    private TiendaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Control de sesión: Si no hay sesión, ir al Login
        if (!session.isLoggedIn()) {
            irALogin();
            return;
        }

        // Si el usuario no es de rol consulta (por si acaso), asegurar acceso o redireccionar
        // Pero esta es la interfaz única para el rol CONSULTA (id_rol == 3)

        setContentView(R.layout.activity_tienda_productos);

        dbHelper = new DBHelper(this);

        etBuscarTienda = findViewById(R.id.etBuscarTienda);
        btnEscanearTienda = findViewById(R.id.btnEscanearTienda);
        btnCerrarSesionTienda = findViewById(R.id.btnCerrarSesionTienda);
        chipSoloEnStock = findViewById(R.id.chipSoloEnStock);
        lstTiendaProductos = findViewById(R.id.lstTiendaProductos);
        tvTiendaSubt = findViewById(R.id.tvTiendaSubt);

        HashMap<String, String> userDetails = session.getUserDetails();
        String nombre = userDetails.get(SessionManager.KEY_FULL_NAME);
        String rol = userDetails.get(SessionManager.KEY_ROLE_NAME);
        tvTiendaSubt.setText("Cliente: " + nombre + " (" + rol + ")");

        listaProductos = new ArrayList<>();
        adapter = new TiendaAdapter(this, listaProductos);
        lstTiendaProductos.setAdapter(adapter);

        // Cierre de Sesión Seguro
        btnCerrarSesionTienda.setOnClickListener(v -> {
            session.logoutUser();
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            irALogin();
        });

        // Escáner de Código de Barras / QR para búsqueda en tienda
        btnEscanearTienda.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escanee el código de barras del producto en tienda");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        // Búsqueda Dinámica
        etBuscarTienda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarProductosTienda(s.toString().trim(), chipSoloEnStock.isChecked());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipSoloEnStock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String filtro = etBuscarTienda.getText() != null ? etBuscarTienda.getText().toString().trim() : "";
            cargarProductosTienda(filtro, isChecked);
        });

        // Clic en producto para ver detalle de tienda (solo lectura)
        lstTiendaProductos.setOnItemClickListener((parent, view, position, id) -> {
            Producto p = listaProductos.get(position);
            mostrarDetalleProductoTienda(p);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String filtro = etBuscarTienda.getText() != null ? etBuscarTienda.getText().toString().trim() : "";
        cargarProductosTienda(filtro, chipSoloEnStock.isChecked());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String barcode = result.getContents();
                etBuscarTienda.setText(barcode);
                Toast.makeText(this, "Producto encontrado por código: " + barcode, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void cargarProductosTienda(String filtro, boolean soloEnStock) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        listaProductos.clear();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id_producto, p.sku_codigo, p.codigo_barras, p.nombre, p.descripcion, p.unidad_medida, p.stock_inicial, p.stock_actual, p.stock_minimo, p.id_categoria, p.activo, c.nombre_categoria ")
                .append("FROM producto p ")
                .append("LEFT JOIN categoria c ON p.id_categoria = c.id_categoria ")
                .append("WHERE p.activo = 1 ");

        if (soloEnStock) {
            sql.append("AND p.stock_actual > 0 ");
        }

        List<String> argsList = new ArrayList<>();
        if (filtro != null && !filtro.isEmpty()) {
            sql.append("AND (p.sku_codigo LIKE ? OR p.nombre LIKE ? OR p.codigo_barras LIKE ?) ");
            String q = "%" + filtro + "%";
            argsList.add(q);
            argsList.add(q);
            argsList.add(q);
        }

        sql.append("ORDER BY p.stock_actual DESC, p.nombre ASC");

        String[] selectionArgs = argsList.isEmpty() ? null : argsList.toArray(new String[0]);

        try (Cursor c = db.rawQuery(sql.toString(), selectionArgs)) {
            if (c.moveToFirst()) {
                int idIdx = c.getColumnIndexOrThrow("id_producto");
                int skuIdx = c.getColumnIndexOrThrow("sku_codigo");
                int cbIdx = c.getColumnIndex("codigo_barras");
                int nomIdx = c.getColumnIndexOrThrow("nombre");
                int descIdx = c.getColumnIndexOrThrow("descripcion");
                int umIdx = c.getColumnIndex("unidad_medida");
                int stIniIdx = c.getColumnIndex("stock_inicial");
                int stockActIdx = c.getColumnIndexOrThrow("stock_actual");
                int stockMinIdx = c.getColumnIndexOrThrow("stock_minimo");
                int catIdIdx = c.getColumnIndexOrThrow("id_categoria");
                int actIdx = c.getColumnIndexOrThrow("activo");
                int catNomIdx = c.getColumnIndexOrThrow("nombre_categoria");

                do {
                    Producto p = new Producto();
                    p.setIdProducto(c.getInt(idIdx));
                    p.setSkuCodigo(c.getString(skuIdx));
                    p.setCodigoBarras(cbIdx != -1 && c.getString(cbIdx) != null ? c.getString(cbIdx) : "");
                    p.setNombre(c.getString(nomIdx));
                    p.setDescripcion(c.getString(descIdx) != null ? c.getString(descIdx) : "");
                    p.setUnidadMedida(umIdx != -1 && c.getString(umIdx) != null ? c.getString(umIdx) : "Unidad");
                    p.setStockInicial(stIniIdx != -1 ? c.getInt(stIniIdx) : 0);
                    p.setStockActual(c.getInt(stockActIdx));
                    p.setStockMinimo(c.getInt(stockMinIdx));
                    p.setIdCategoria(c.getInt(catIdIdx));
                    p.setActivo(c.getInt(actIdx));
                    p.setNombreCategoria(c.getString(catNomIdx) != null ? c.getString(catNomIdx) : "General");

                    listaProductos.add(p);
                } while (c.moveToNext());
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar catálogo de tienda: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    private void mostrarDetalleProductoTienda(Producto p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(p.getNombre());

        StringBuilder sb = new StringBuilder();
        sb.append("SKU: ").append(p.getSkuCodigo()).append("\n");
        sb.append("Categoría: ").append(p.getNombreCategoria()).append("\n");
        if (!p.getCodigoBarras().isEmpty()) {
            sb.append("Código de Barras: ").append(p.getCodigoBarras()).append("\n");
        }
        if (!p.getDescripcion().isEmpty()) {
            sb.append("Descripción: ").append(p.getDescripcion()).append("\n");
        }
        sb.append("Stock Disponible: ").append(p.getStockActual()).append(" ").append(p.getUnidadMedida()).append("\n");
        sb.append("Stock Mínimo: ").append(p.getStockMinimo()).append(" ").append(p.getUnidadMedida()).append("\n");
        sb.append("Estado: ").append(p.getStockActual() > 0 ? "En Stock (Disponible)" : "Sin Stock / Agotado");

        builder.setMessage(sb.toString());
        builder.setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void irALogin() {
        Intent intent = new Intent(TiendaProductosActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Adaptador de Tienda para los Productos
    private class TiendaAdapter extends ArrayAdapter<Producto> {

        public TiendaAdapter(@NonNull Context context, @NonNull List<Producto> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_tienda_producto, parent, false);
            }

            Producto producto = getItem(position);
            if (producto != null) {
                TextView tvSku = convertView.findViewById(R.id.tvTiendaSku);
                TextView tvCategoria = convertView.findViewById(R.id.tvTiendaCategoria);
                TextView tvNombre = convertView.findViewById(R.id.tvTiendaNombre);
                TextView tvDescripcion = convertView.findViewById(R.id.tvTiendaDescripcion);
                TextView tvCodigoBarras = convertView.findViewById(R.id.tvTiendaCodigoBarras);
                TextView tvStockActual = convertView.findViewById(R.id.tvTiendaStockActual);
                TextView tvUnidad = convertView.findViewById(R.id.tvTiendaUnidad);
                TextView tvStockBadge = convertView.findViewById(R.id.tvTiendaStockBadge);

                tvSku.setText(producto.getSkuCodigo());
                tvCategoria.setText(producto.getNombreCategoria());
                tvNombre.setText(producto.getNombre());

                if (producto.getDescripcion() != null && !producto.getDescripcion().isEmpty()) {
                    tvDescripcion.setText(producto.getDescripcion());
                    tvDescripcion.setVisibility(View.VISIBLE);
                } else {
                    tvDescripcion.setVisibility(View.GONE);
                }

                if (producto.getCodigoBarras() != null && !producto.getCodigoBarras().isEmpty()) {
                    tvCodigoBarras.setText("CB: " + producto.getCodigoBarras());
                    tvCodigoBarras.setVisibility(View.VISIBLE);
                } else {
                    tvCodigoBarras.setVisibility(View.GONE);
                }

                tvStockActual.setText("Stock: " + producto.getStockActual());
                tvUnidad.setText(producto.getUnidadMedida());

                if (producto.getStockActual() > 0) {
                    tvStockBadge.setText("EN STOCK");
                    tvStockBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")));
                    tvStockBadge.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                } else {
                    tvStockBadge.setText("SIN STOCK");
                    tvStockBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE")));
                    tvStockBadge.setTextColor(android.graphics.Color.parseColor("#C62828"));
                }
            }

            return convertView;
        }
    }
}