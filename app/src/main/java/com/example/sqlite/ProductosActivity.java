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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class ProductosActivity extends AppCompatActivity {

    private TextInputEditText etBuscarProducto;
    private MaterialButton btnEscanearBuscar, btnNuevoProducto;
    private ListView lstProductos;

    private DBHelper dbHelper;
    private ArrayList<Producto> listaProductos;
    private ProductoAdapter adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        dbHelper = new DBHelper(this);
        session = new SessionManager(this);

        etBuscarProducto = findViewById(R.id.etBuscarProducto);
        btnEscanearBuscar = findViewById(R.id.btnEscanearBuscar);
        btnNuevoProducto = findViewById(R.id.btnNuevoProducto);
        lstProductos = findViewById(R.id.lstProductos);

        // Control de permisos por rol: Usuario de consulta (id_rol == 3) no puede crear/editar productos
        if ((session.getRoleId() != 1 && session.getRoleId() != 2)) {
            btnNuevoProducto.setVisibility(View.GONE);
        }

        listaProductos = new ArrayList<>();
        adapter = new ProductoAdapter(this, listaProductos);
        lstProductos.setAdapter(adapter);

        // Botón "Volver": cierra esta pantalla y regresa al menú
        findViewById(R.id.btnVolverProductos).setOnClickListener(v -> finish());

        btnNuevoProducto.setOnClickListener(v -> {
            Intent i = new Intent(ProductosActivity.this, EditarProductoActivity.class);
            startActivity(i);
        });

        btnEscanearBuscar.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escanee un código para buscar");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        // Búsqueda Dinámica y Reactiva por SKU, Nombre o Código de Barras
        etBuscarProducto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarProductos(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clic en fila para Editar (Solo si no es Usuario de Consulta)
        lstProductos.setOnItemClickListener((parent, view, position, id) -> {
            if ((session.getRoleId() != 1 && session.getRoleId() != 2)) {
                Toast.makeText(this, "Usuario de consulta: Solo lectura", Toast.LENGTH_SHORT).show();
                return;
            }
            Producto producto = listaProductos.get(position);
            Intent i = new Intent(ProductosActivity.this, EditarProductoActivity.class);
            i.putExtra("id_producto", producto.getIdProducto());
            i.putExtra("sku_codigo", producto.getSkuCodigo());
            i.putExtra("codigo_barras", producto.getCodigoBarras());
            i.putExtra("nombre", producto.getNombre());
            i.putExtra("descripcion", producto.getDescripcion());
            i.putExtra("unidad_medida", producto.getUnidadMedida());
            i.putExtra("stock_inicial", producto.getStockInicial());
            i.putExtra("stock_actual", producto.getStockActual());
            i.putExtra("stock_minimo", producto.getStockMinimo());
            i.putExtra("id_categoria", producto.getIdCategoria());
            i.putExtra("activo", producto.getActivo());
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String barcode = result.getContents();
                etBuscarProducto.setText(barcode);
                Toast.makeText(this, "Buscando código: " + barcode, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String filtro = etBuscarProducto.getText() != null ? etBuscarProducto.getText().toString().trim() : "";
        cargarProductos(filtro);
    }

    private void cargarProductos(String filtro) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        listaProductos.clear();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id_producto, p.sku_codigo, p.codigo_barras, p.nombre, p.descripcion, p.unidad_medida, p.stock_inicial, p.stock_actual, p.stock_minimo, p.id_categoria, p.activo, c.nombre_categoria ")
                .append("FROM producto p ")
                .append("LEFT JOIN categoria c ON p.id_categoria = c.id_categoria ")
                .append("WHERE 1=1 ");

        String[] selectionArgs = null;
        if (filtro != null && !filtro.isEmpty()) {
            sql.append("AND (p.sku_codigo LIKE ? OR p.nombre LIKE ? OR p.codigo_barras LIKE ?) ");
            String q = "%" + filtro + "%";
            selectionArgs = new String[]{q, q, q};
        }

        sql.append("ORDER BY p.activo DESC, p.nombre ASC");

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
                    p.setDescripcion(c.getString(descIdx));
                    p.setUnidadMedida(umIdx != -1 && c.getString(umIdx) != null ? c.getString(umIdx) : "Unidad");
                    p.setStockInicial(stIniIdx != -1 ? c.getInt(stIniIdx) : 0);
                    p.setStockActual(c.getInt(stockActIdx));
                    p.setStockMinimo(c.getInt(stockMinIdx));
                    p.setIdCategoria(c.getInt(catIdIdx));
                    p.setActivo(c.getInt(actIdx));
                    p.setNombreCategoria(c.getString(catNomIdx) != null ? c.getString(catNomIdx) : "Sin Categoría");

                    listaProductos.add(p);
                } while (c.moveToNext());
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar inventario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    // Adaptador Personalizado para Ítems de Producto
    private class ProductoAdapter extends ArrayAdapter<Producto> {

        public ProductoAdapter(@NonNull Context context, @NonNull List<Producto> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_producto, parent, false);
            }

            Producto producto = getItem(position);
            if (producto != null) {
                TextView tvSku = convertView.findViewById(R.id.tvItemSku);
                TextView tvCategoria = convertView.findViewById(R.id.tvItemCategoria);
                TextView tvNombre = convertView.findViewById(R.id.tvItemNombre);
                TextView tvCodigoBarras = convertView.findViewById(R.id.tvItemCodigoBarras);
                TextView tvStockActual = convertView.findViewById(R.id.tvItemStockActual);
                TextView tvUnidadMedida = convertView.findViewById(R.id.tvItemUnidadMedida);
                TextView tvStockMinimo = convertView.findViewById(R.id.tvItemStockMinimo);
                TextView tvBajoStockAlert = convertView.findViewById(R.id.tvItemBajoStockAlert);
                TextView tvDeshabilitadoAlert = convertView.findViewById(R.id.tvItemDeshabilitadoAlert);
                MaterialButton btnToggleEstado = convertView.findViewById(R.id.btnItemToggleEstado);

                tvSku.setText(producto.getSkuCodigo());
                tvCategoria.setText(producto.getNombreCategoria());
                tvNombre.setText(producto.getNombre());

                if (tvCodigoBarras != null) {
                    if (producto.getCodigoBarras() != null && !producto.getCodigoBarras().isEmpty()) {
                        tvCodigoBarras.setText("CB: " + producto.getCodigoBarras());
                        tvCodigoBarras.setVisibility(View.VISIBLE);
                    } else {
                        tvCodigoBarras.setVisibility(View.GONE);
                    }
                }

                if (tvUnidadMedida != null) {
                    tvUnidadMedida.setText("(" + producto.getUnidadMedida() + ")");
                }
                tvStockMinimo.setText("Mínimo: " + producto.getStockMinimo() + " " + producto.getUnidadMedida());

                boolean isDeshabilitado = producto.getActivo() == 0;

                if (isDeshabilitado) {
                    // Vista como deshabilitado / sin stock inmediatamente
                    if (tvDeshabilitadoAlert != null) tvDeshabilitadoAlert.setVisibility(View.VISIBLE);
                    tvBajoStockAlert.setVisibility(View.GONE);
                    tvStockActual.setText("Stock: 0 (Deshabilitado)");
                    tvStockActual.setTextColor(android.graphics.Color.parseColor("#C62828"));
                } else {
                    if (tvDeshabilitadoAlert != null) tvDeshabilitadoAlert.setVisibility(View.GONE);
                    tvStockActual.setText("Stock: " + producto.getStockActual());
                    tvStockActual.setTextColor(android.graphics.Color.parseColor("#1E293B"));

                    // Alerta de Bajo Stock
                    if (producto.isStockBajo()) {
                        tvBajoStockAlert.setVisibility(View.VISIBLE);
                    } else {
                        tvBajoStockAlert.setVisibility(View.GONE);
                    }
                }

                // Control del botón de acción rápida de deshabilitar/habilitar
                if (btnToggleEstado != null) {
                    if ((session.getRoleId() != 1 && session.getRoleId() != 2)) {
                        btnToggleEstado.setVisibility(View.GONE);
                    } else {
                        btnToggleEstado.setVisibility(View.VISIBLE);
                        if (isDeshabilitado) {
                            btnToggleEstado.setText("Habilitar");
                            btnToggleEstado.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")));
                            btnToggleEstado.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                            btnToggleEstado.setOnClickListener(v -> {
                                boolean ok = dbHelper.actualizarEstadoProducto(producto.getIdProducto(), 1);
                                if (ok) {
                                    Toast.makeText(getContext(), "Producto habilitado: " + producto.getNombre(), Toast.LENGTH_SHORT).show();
                                    String f = etBuscarProducto.getText() != null ? etBuscarProducto.getText().toString().trim() : "";
                                    cargarProductos(f);
                                } else {
                                    Toast.makeText(getContext(), "Error al habilitar producto", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            btnToggleEstado.setText("Deshabilitar");
                            btnToggleEstado.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE")));
                            btnToggleEstado.setTextColor(android.graphics.Color.parseColor("#C62828"));
                            btnToggleEstado.setOnClickListener(v -> {
                                boolean ok = dbHelper.actualizarEstadoProducto(producto.getIdProducto(), 0);
                                if (ok) {
                                    Toast.makeText(getContext(), "Producto deshabilitado (sin stock): " + producto.getNombre(), Toast.LENGTH_SHORT).show();
                                    String f = etBuscarProducto.getText() != null ? etBuscarProducto.getText().toString().trim() : "";
                                    cargarProductos(f);
                                } else {
                                    Toast.makeText(getContext(), "Error al deshabilitar producto", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            }

            return convertView;
        }
    }
}