package com.example.sqlite;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InformeStockActivity extends AppCompatActivity {

    private TextInputEditText etBuscarInforme;
    private Spinner spCategoriaFilter, spEstadoFilter;
    private TextView tvTotalCountInforme;
    private MaterialButton btnExportarInforme, btnVolverInforme;
    private ListView lstInformeStock;

    private DBHelper dbHelper;
    private List<Producto> listaProductos;
    private InformeAdapter adapter;

    private List<Integer> categoriaIds = new ArrayList<>();
    private List<String> categoriaNombres = new ArrayList<>();
    private final String[] estadosCodigos = {"TODOS", "DISPONIBLE", "CRITICO", "SIN_STOCK"};
    private final String[] estadosNombres = {"Todos los Estados", "Disponible (Normal)", "Stock Crítico", "Sin Stock (Agotado)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informe_stock);

        dbHelper = new DBHelper(this);

        etBuscarInforme = findViewById(R.id.etBuscarInforme);
        spCategoriaFilter = findViewById(R.id.spCategoriaFilter);
        spEstadoFilter = findViewById(R.id.spEstadoFilter);
        tvTotalCountInforme = findViewById(R.id.tvTotalCountInforme);
        btnExportarInforme = findViewById(R.id.btnExportarInforme);
        btnVolverInforme = findViewById(R.id.btnVolverInforme);
        lstInformeStock = findViewById(R.id.lstInformeStock);

        listaProductos = new ArrayList<>();
        adapter = new InformeAdapter(this, listaProductos);
        lstInformeStock.setAdapter(adapter);

        btnVolverInforme.setOnClickListener(v -> finish());
        btnExportarInforme.setOnClickListener(v -> exportarInformeCSV());

        cargarCategoriasSpinner();
        cargarEstadosSpinner();

        // Listeners para filtros dinámicos
        etBuscarInforme.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spCategoriaFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltros();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spEstadoFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltros();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        aplicarFiltros();
    }

    private void cargarCategoriasSpinner() {
        categoriaIds.clear();
        categoriaNombres.clear();

        categoriaIds.add(0);
        categoriaNombres.add("Todas las Categorías");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id_categoria, nombre_categoria FROM categoria WHERE activo = 1 ORDER BY nombre_categoria ASC", null)) {
            if (c.moveToFirst()) {
                do {
                    categoriaIds.add(c.getInt(0));
                    categoriaNombres.add(c.getString(1));
                } while (c.moveToNext());
            }
        } catch (Exception ignored) {}

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoriaNombres);
        spCategoriaFilter.setAdapter(catAdapter);
    }

    private void cargarEstadosSpinner() {
        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, estadosNombres);
        spEstadoFilter.setAdapter(estadoAdapter);
    }

    private void aplicarFiltros() {
        int catIndex = spCategoriaFilter.getSelectedItemPosition();
        int catId = (catIndex >= 0 && catIndex < categoriaIds.size()) ? categoriaIds.get(catIndex) : 0;

        int estadoIndex = spEstadoFilter.getSelectedItemPosition();
        String estadoCodigo = (estadoIndex >= 0 && estadoIndex < estadosCodigos.length) ? estadosCodigos[estadoIndex] : "TODOS";

        String busqueda = etBuscarInforme.getText() != null ? etBuscarInforme.getText().toString() : "";

        List<Producto> resultados = dbHelper.obtenerInformeStock(catId, busqueda, estadoCodigo);
        listaProductos.clear();
        if (resultados != null) {
            listaProductos.addAll(resultados);
        }
        adapter.notifyDataSetChanged();

        tvTotalCountInforme.setText("Productos encontrados: " + listaProductos.size());
    }

    private void exportarInformeCSV() {
        if (listaProductos == null || listaProductos.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("SKU;Nombre;Categoría;Stock Actual;Stock Mínimo;Estado;Código de Barras\n");

            for (Producto p : listaProductos) {
                String estado = "DISPONIBLE";
                if (p.getStockActual() == 0) {
                    estado = "SIN STOCK";
                } else if (p.getStockActual() <= p.getStockMinimo()) {
                    estado = "CRÍTICO";
                }

                csv.append(p.getSkuCodigo()).append(";")
                        .append(p.getNombre()).append(";")
                        .append(p.getNombreCategoria() != null ? p.getNombreCategoria() : "").append(";")
                        .append(p.getStockActual()).append(";")
                        .append(p.getStockMinimo()).append(";")
                        .append(estado).append(";")
                        .append(p.getCodigoBarras() != null ? p.getCodigoBarras() : "").append("\n");
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Informe_Stock_" + timeStamp + ".csv";

            File cacheDir = new File(getCacheDir(), "exports");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            File file = new File(cacheDir, fileName);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(csv.toString());
            }

            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Informe de Stock - Sistema de Bodega");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Adjunto informe de stock generado el " + timeStamp);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Exportar Informe de Stock via"));

        } catch (Exception e) {
            Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static class InformeAdapter extends ArrayAdapter<Producto> {
        private final Context context;
        private final List<Producto> productos;

        public InformeAdapter(@NonNull Context context, @NonNull List<Producto> objects) {
            super(context, 0, objects);
            this.context = context;
            this.productos = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_informe_stock, parent, false);
            }

            Producto p = productos.get(position);

            TextView tvNombre = convertView.findViewById(R.id.tvItemNombreInforme);
            TextView tvEstado = convertView.findViewById(R.id.tvItemEstadoInforme);
            TextView tvSkuCat = convertView.findViewById(R.id.tvItemSkuCategoriaInforme);
            TextView tvStock = convertView.findViewById(R.id.tvItemStockInforme);
            TextView tvCodigo = convertView.findViewById(R.id.tvItemCodigoBarrasInforme);

            tvNombre.setText(p.getNombre());
            tvSkuCat.setText("SKU: " + p.getSkuCodigo() + " | Categoría: " + (p.getNombreCategoria() != null ? p.getNombreCategoria() : "Sin Cat."));
            tvStock.setText("Stock Actual: " + p.getStockActual() + " / Mínimo: " + p.getStockMinimo());
            tvCodigo.setText("Cód: " + (p.getCodigoBarras() != null ? p.getCodigoBarras() : "-"));

            // Estado y color de badge
            String estadoTexto;
            int colorFondo;

            if (p.getStockActual() == 0) {
                estadoTexto = "SIN STOCK";
                colorFondo = Color.parseColor("#D32F2F"); // Rojo
            } else if (p.getStockActual() <= p.getStockMinimo()) {
                estadoTexto = "CRÍTICO";
                colorFondo = Color.parseColor("#F57C00"); // Naranja
            } else {
                estadoTexto = "DISPONIBLE";
                colorFondo = Color.parseColor("#2E7D32"); // Verde
            }

            tvEstado.setText(estadoTexto);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(16f);
            drawable.setColor(colorFondo);
            tvEstado.setBackground(drawable);

            return convertView;
        }
    }
}