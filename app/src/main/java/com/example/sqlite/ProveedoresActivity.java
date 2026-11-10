package com.example.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class ProveedoresActivity extends AppCompatActivity {

    private TextInputEditText etRutProveedor, etRazonSocial, etContacto;
    private MaterialButton btnGuardarProveedor;
    private ListView lstProveedores;

    private DBHelper dbHelper;
    private ArrayList<String> listaProveedoresString;
    private ArrayList<Integer> listaProveedoresIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedores);

        dbHelper = new DBHelper(this);

        etRutProveedor = findViewById(R.id.etRutProveedor);
        etRazonSocial = findViewById(R.id.etRazonSocial);
        etContacto = findViewById(R.id.etContacto);
        btnGuardarProveedor = findViewById(R.id.btnGuardarProveedor);
        lstProveedores = findViewById(R.id.lstProveedores);

        listaProveedoresString = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaProveedoresString);
        lstProveedores.setAdapter(adapter);

        session = new SessionManager(this);

        // Control de permisos por rol: Rol Consulta (id_rol == 3) no puede crear ni editar proveedores
        if (session.getRoleId() != 1 && session.getRoleId() != 2) {
            View cardFormulario = findViewById(R.id.cardFormularioProveedor);
            if (cardFormulario != null) {
                cardFormulario.setVisibility(View.GONE);
            }
        }

        btnGuardarProveedor.setOnClickListener(v -> guardarProveedor());

        // Solo el Administrador (id_rol == 1) puede editar o eliminar proveedores
        if (session.getRoleId() == 1) {
            lstProveedores.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < listaProveedoresIds.size()) {
                    mostrarOpcionesProveedor(listaProveedoresIds.get(position));
                }
            });
        }

        // Botón "Volver": cierra esta pantalla y regresa al menú principal
        findViewById(R.id.btnVolverProveedores).setOnClickListener(v -> finish());

        cargarProveedores();
    }

    private void guardarProveedor() {
        if (session.getRoleId() != 1 && session.getRoleId() != 2) {
            Toast.makeText(this, "Acceso denegado: El rol consulta no puede crear proveedores", Toast.LENGTH_SHORT).show();
            return;
        }

        String rut = etRutProveedor.getText() != null ? etRutProveedor.getText().toString().trim() : "";
        String razonSocial = etRazonSocial.getText() != null ? etRazonSocial.getText().toString().trim() : "";
        String contacto = etContacto.getText() != null ? etContacto.getText().toString().trim() : "";

        // Los tres datos son obligatorios
        if (TextUtils.isEmpty(rut)) {
            etRutProveedor.setError("Ingrese el RUT de la empresa");
            return;
        }

        if (TextUtils.isEmpty(razonSocial)) {
            etRazonSocial.setError("Ingrese la razón social");
            return;
        }

        if (TextUtils.isEmpty(contacto)) {
            etContacto.setError("Ingrese el número de contacto");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // El RUT de la empresa debe ser único
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM proveedor WHERE rut_empresa = ?", new String[]{rut})) {
                if (c.moveToFirst() && c.getInt(0) > 0) {
                    etRutProveedor.setError("Ya existe un proveedor con este RUT");
                    return;
                }
            }

            ContentValues values = new ContentValues();
            values.put("rut_empresa", rut);
            values.put("razon_social", razonSocial);
            values.put("contacto", contacto);

            long id = db.insert("proveedor", null, values);
            if (id != -1) {
                Toast.makeText(this, "Proveedor registrado correctamente", Toast.LENGTH_SHORT).show();
                etRutProveedor.setText("");
                etRazonSocial.setText("");
                etContacto.setText("");
                etRutProveedor.clearFocus();
                cargarProveedores(); // Recargar lista
            } else {
                Toast.makeText(this, "Error al registrar el proveedor", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    // Menú de acciones (solo Administrador): Editar / Eliminar
    private void mostrarOpcionesProveedor(int idProveedor) {
        new AlertDialog.Builder(this)
                .setTitle("Proveedor #" + idProveedor)
                .setItems(new String[]{"Editar", "Eliminar"}, (dialog, which) -> {
                    if (which == 0) {
                        mostrarDialogoEditarProveedor(idProveedor);
                    } else {
                        confirmarEliminarProveedor(idProveedor);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEditarProveedor(int idProveedor) {
        String rutActual = "";
        String razonActual = "";
        String contactoActual = "";

        SQLiteDatabase dbLectura = dbHelper.getReadableDatabase();
        try (Cursor c = dbLectura.rawQuery(
                "SELECT rut_empresa, razon_social, contacto FROM proveedor WHERE id_proveedor = ?",
                new String[]{String.valueOf(idProveedor)})) {
            if (c.moveToFirst()) {
                rutActual = c.getString(0) != null ? c.getString(0) : "";
                razonActual = c.getString(1) != null ? c.getString(1) : "";
                contactoActual = c.getString(2) != null ? c.getString(2) : "";
            }
        } finally {
            if (dbLectura != null && dbLectura.isOpen()) dbLectura.close();
        }

        View vista = getLayoutInflater().inflate(R.layout.dialog_editar_proveedor, null);
        TextInputEditText etRut = vista.findViewById(R.id.etEditRutProveedor);
        TextInputEditText etRazon = vista.findViewById(R.id.etEditRazonSocial);
        TextInputEditText etCont = vista.findViewById(R.id.etEditContacto);
        etRut.setText(rutActual);
        etRazon.setText(razonActual);
        etCont.setText(contactoActual);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar Proveedor")
                .setView(vista)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();

        // Se sobrescribe el click para no cerrar el diálogo si la validación falla
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String rut = etRut.getText() != null ? etRut.getText().toString().trim() : "";
            String razon = etRazon.getText() != null ? etRazon.getText().toString().trim() : "";
            String contacto = etCont.getText() != null ? etCont.getText().toString().trim() : "";

            if (TextUtils.isEmpty(rut)) {
                etRut.setError("Ingrese el RUT de la empresa");
                return;
            }
            if (TextUtils.isEmpty(razon)) {
                etRazon.setError("Ingrese la razón social");
                return;
            }
            if (TextUtils.isEmpty(contacto)) {
                etCont.setError("Ingrese el número de contacto");
                return;
            }

            int resultado = actualizarProveedor(idProveedor, rut, razon, contacto);
            if (resultado == 1) {
                dialogo.dismiss();
            } else if (resultado == -2) {
                etRut.setError("Ya existe otro proveedor con este RUT");
            }
        });
    }

    // Retorna 1 si actualizó, -2 si el RUT está duplicado, 0 en otro error
    private int actualizarProveedor(int idProveedor, String rut, String razonSocial, String contacto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // El RUT debe seguir siendo único (ignorando el propio proveedor)
            try (Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM proveedor WHERE rut_empresa = ? AND id_proveedor <> ?",
                    new String[]{rut, String.valueOf(idProveedor)})) {
                if (c.moveToFirst() && c.getInt(0) > 0) {
                    return -2;
                }
            }

            ContentValues values = new ContentValues();
            values.put("rut_empresa", rut);
            values.put("razon_social", razonSocial);
            values.put("contacto", contacto);

            int filas = db.update("proveedor", values, "id_proveedor = ?", new String[]{String.valueOf(idProveedor)});
            if (filas > 0) {
                Toast.makeText(this, "Proveedor actualizado correctamente", Toast.LENGTH_SHORT).show();
                cargarProveedores();
                return 1;
            }
            Toast.makeText(this, "No se pudo actualizar el proveedor", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
        return 0;
    }

    private void confirmarEliminarProveedor(int idProveedor) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar proveedor")
                .setMessage("Esta acción eliminará el proveedor de forma permanente. ¿Desea continuar?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarProveedor(idProveedor))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarProveedor(int idProveedor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int filas = db.delete("proveedor", "id_proveedor = ?", new String[]{String.valueOf(idProveedor)});
            if (filas > 0) {
                Toast.makeText(this, "Proveedor eliminado", Toast.LENGTH_SHORT).show();
                cargarProveedores();
            } else {
                Toast.makeText(this, "No se pudo eliminar el proveedor", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    private void cargarProveedores() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        listaProveedoresString.clear();
        listaProveedoresIds.clear();

        String sql = "SELECT id_proveedor, rut_empresa, razon_social, contacto FROM proveedor ORDER BY razon_social ASC";

        try (Cursor c = db.rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                int idIdx = c.getColumnIndexOrThrow("id_proveedor");
                int rutIdx = c.getColumnIndexOrThrow("rut_empresa");
                int razonIdx = c.getColumnIndexOrThrow("razon_social");
                int contactoIdx = c.getColumnIndexOrThrow("contacto");

                do {
                    // Se usa el modelo Proveedor que ya existe en el proyecto
                    Proveedor p = new Proveedor();
                    p.setIdProveedor(c.getInt(idIdx));
                    p.setRutEmpresa(c.getString(rutIdx));
                    p.setRazonSocial(c.getString(razonIdx));
                    p.setContacto(c.getString(contactoIdx));

                    String item = p.getIdProveedor() + " - " + p.getRazonSocial()
                            + " | RUT: " + p.getRutEmpresa()
                            + (TextUtils.isEmpty(p.getContacto()) ? "" : " | Contacto: " + p.getContacto());
                    listaProveedoresString.add(item);
                    listaProveedoresIds.add(p.getIdProveedor());
                } while (c.moveToNext());
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar proveedores: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }
}