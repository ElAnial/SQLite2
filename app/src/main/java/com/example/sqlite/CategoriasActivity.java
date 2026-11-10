package com.example.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class CategoriasActivity extends AppCompatActivity {

    private TextInputEditText etNombreCategoria, etDescripcionCategoria;
    private MaterialButton btnGuardarCategoria;
    private ListView lstCategorias;

    private DBHelper dbHelper;
    private ArrayList<String> listaCategoriasString;
    private ArrayList<Integer> listaCategoriasIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorias);

        dbHelper = new DBHelper(this);

        etNombreCategoria = findViewById(R.id.etNombreCategoria);
        etDescripcionCategoria = findViewById(R.id.etDescripcionCategoria);
        btnGuardarCategoria = findViewById(R.id.btnGuardarCategoria);
        lstCategorias = findViewById(R.id.lstCategorias);

        listaCategoriasString = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaCategoriasString) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // Texto en negro para que las categorías se lean bien
                ((android.widget.TextView) view.findViewById(android.R.id.text1)).setTextColor(0xFF000000);
                return view;
            }
        };
        lstCategorias.setAdapter(adapter);

        session = new SessionManager(this);

        // Control de permisos por rol: Rol Consulta (id_rol == 3) no puede crear ni editar categorías
        if (session.getRoleId() != 1 && session.getRoleId() != 2) {
            View cardFormulario = findViewById(R.id.cardFormularioCategoria);
            if (cardFormulario != null) {
                cardFormulario.setVisibility(View.GONE);
            }
        }

        btnGuardarCategoria.setOnClickListener(v -> guardarCategoria());

        // Solo el Administrador (id_rol == 1) puede editar o dar de baja categorías
        if (session.getRoleId() == 1) {
            lstCategorias.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < listaCategoriasIds.size()) {
                    mostrarOpcionesCategoria(listaCategoriasIds.get(position));
                }
            });
        }

        // Botón "Volver": cierra esta pantalla y regresa al menú
        findViewById(R.id.btnVolverCategorias).setOnClickListener(v -> finish());

        cargarCategorias();
    }

    private void guardarCategoria() {
        if (session.getRoleId() != 1 && session.getRoleId() != 2) {
            Toast.makeText(this, "Acceso denegado: El rol consulta no puede crear categorías", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = etNombreCategoria.getText() != null ? etNombreCategoria.getText().toString().trim() : "";
        String descripcion = etDescripcionCategoria.getText() != null ? etDescripcionCategoria.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            etNombreCategoria.setError("Ingrese el nombre de la categoría");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre_categoria", nombre);
        values.put("descripcion", descripcion);
        values.put("activo", 1);

        try {
            long id = db.insert("categoria", null, values);
            if (id != -1) {
                Toast.makeText(this, "Categoría registrada correctamente (CU-09)", Toast.LENGTH_SHORT).show();
                etNombreCategoria.setText("");
                etDescripcionCategoria.setText("");
                etNombreCategoria.clearFocus();
                cargarCategorias(); // Recargar lista
            } else {
                Toast.makeText(this, "Error al registrar la categoría", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    // Menú de acciones (solo Administrador): Editar / Dar de baja
    private void mostrarOpcionesCategoria(int idCategoria) {
        new AlertDialog.Builder(this)
                .setTitle("Categoría #" + idCategoria)
                .setItems(new String[]{"Editar", "Dar de baja"}, (dialog, which) -> {
                    if (which == 0) {
                        mostrarDialogoEditarCategoria(idCategoria);
                    } else {
                        confirmarBajaCategoria(idCategoria);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEditarCategoria(int idCategoria) {
        String nombreActual = "";
        String descActual = "";

        SQLiteDatabase dbLectura = dbHelper.getReadableDatabase();
        try (Cursor c = dbLectura.rawQuery(
                "SELECT nombre_categoria, descripcion FROM categoria WHERE id_categoria = ?",
                new String[]{String.valueOf(idCategoria)})) {
            if (c.moveToFirst()) {
                nombreActual = c.getString(0) != null ? c.getString(0) : "";
                descActual = c.getString(1) != null ? c.getString(1) : "";
            }
        } finally {
            if (dbLectura != null && dbLectura.isOpen()) dbLectura.close();
        }

        View vista = getLayoutInflater().inflate(R.layout.dialog_editar_categoria, null);
        TextInputEditText etNombre = vista.findViewById(R.id.etEditNombreCategoria);
        TextInputEditText etDesc = vista.findViewById(R.id.etEditDescripcionCategoria);
        etNombre.setText(nombreActual);
        etDesc.setText(descActual);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar Categoría")
                .setView(vista)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();

        // Se sobrescribe el click para no cerrar el diálogo si la validación falla
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
            String nuevaDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

            if (TextUtils.isEmpty(nuevoNombre)) {
                etNombre.setError("Ingrese el nombre de la categoría");
                return;
            }

            if (actualizarCategoria(idCategoria, nuevoNombre, nuevaDesc)) {
                dialogo.dismiss();
            }
        });
    }

    private boolean actualizarCategoria(int idCategoria, String nombre, String descripcion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre_categoria", nombre);
        values.put("descripcion", descripcion);

        try {
            int filas = db.update("categoria", values, "id_categoria = ?", new String[]{String.valueOf(idCategoria)});
            if (filas > 0) {
                Toast.makeText(this, "Categoría actualizada correctamente", Toast.LENGTH_SHORT).show();
                cargarCategorias();
                return true;
            }
            Toast.makeText(this, "No se pudo actualizar la categoría", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
        return false;
    }

    private void confirmarBajaCategoria(int idCategoria) {
        new AlertDialog.Builder(this)
                .setTitle("Dar de baja categoría")
                .setMessage("La categoría dejará de mostrarse como activa. ¿Desea continuar?")
                .setPositiveButton("Dar de baja", (dialog, which) -> darDeBajaCategoria(idCategoria))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Baja lógica: activo = 0 (no se borra el registro para conservar los productos asociados)
    private void darDeBajaCategoria(int idCategoria) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("activo", 0);

        try {
            int filas = db.update("categoria", values, "id_categoria = ?", new String[]{String.valueOf(idCategoria)});
            if (filas > 0) {
                Toast.makeText(this, "Categoría dada de baja", Toast.LENGTH_SHORT).show();
                cargarCategorias();
            } else {
                Toast.makeText(this, "No se pudo dar de baja la categoría", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    private void cargarCategorias() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        listaCategoriasString.clear();
        listaCategoriasIds.clear();

        String sql = "SELECT id_categoria, nombre_categoria, descripcion FROM categoria WHERE activo = 1 ORDER BY nombre_categoria ASC";

        try (Cursor c = db.rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                int idIdx = c.getColumnIndexOrThrow("id_categoria");
                int nombreIdx = c.getColumnIndexOrThrow("nombre_categoria");
                int descIdx = c.getColumnIndexOrThrow("descripcion");

                do {
                    int id = c.getInt(idIdx);
                    String nombre = c.getString(nombreIdx);
                    String desc = c.getString(descIdx);

                    String item = id + " - " + nombre + (TextUtils.isEmpty(desc) ? "" : " (" + desc + ")");
                    listaCategoriasString.add(item);
                    listaCategoriasIds.add(id);
                } while (c.moveToNext());
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar categorías: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }
}