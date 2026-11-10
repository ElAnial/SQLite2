package com.example.sqlite;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private SessionManager session;
    private DBHelper dbHelper;

    private TextView tvUsuarioNombre, tvUsuarioRol, tvTotalProductos, tvStockBajoCount;
    private MaterialButton btnCerrarSesion;
    private MaterialCardView cardProductos, cardCategorias, cardUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Control de Sesión Inicial (CU-01)
        if (!session.isLoggedIn()) {
            irALogin();
            return;
        }

        // Si es rol CONSULTA (id_rol == 3), redirigir directamente a la interfaz única de tienda
        if (session.getRoleId() == 3) {
            Intent intent = new Intent(MainActivity.this, TiendaProductosActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        tvUsuarioNombre = findViewById(R.id.tvUsuarioNombre);
        tvUsuarioRol = findViewById(R.id.tvUsuarioRol);
        tvTotalProductos = findViewById(R.id.tvTotalProductos);
        tvStockBajoCount = findViewById(R.id.tvStockBajoCount);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        cardProductos = findViewById(R.id.cardProductos);
        cardCategorias = findViewById(R.id.cardCategorias);
        cardUsuarios = findViewById(R.id.cardUsuarios);

        // Control de Vista según el Rol: Solo Administrador (id_rol == 1) puede gestionar Usuarios y Roles
        if (session.getRoleId() == 1) {
            cardUsuarios.setVisibility(View.VISIBLE);
            cardUsuarios.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, UsuariosActivity.class)));
        } else {
            // Si es BODEGUERO (id_rol != 1), la opción de Usuarios y Roles se oculta
            cardUsuarios.setVisibility(View.GONE);
        }

        // Mostrar datos de la sesión actual
        HashMap<String, String> userDetails = session.getUserDetails();
        String nombre = userDetails.get(SessionManager.KEY_FULL_NAME);
        String rol = userDetails.get(SessionManager.KEY_ROLE_NAME);

        tvUsuarioNombre.setText("Bienvenido, " + nombre);
        tvUsuarioRol.setText("Rol: " + rol);

        // Cierre de Sesión Seguro (CU-02)
        btnCerrarSesion.setOnClickListener(v -> {
            session.logoutUser();
            Toast.makeText(MainActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            irALogin();
        });

        // Eventos de Navegación
        cardProductos.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductosActivity.class)));
        cardCategorias.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CategoriasActivity.class)));
        findViewById(R.id.cardProveedores).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProveedoresActivity.class)));
        View cardRegistros = findViewById(R.id.cardRegistros);
        if (cardRegistros != null) {
            cardRegistros.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegistrosActivity.class)));
        }

        // Entradas a bodega (solo Administrador y Bodeguero)
        View cardEntradas = findViewById(R.id.cardEntradas);
        if (cardEntradas != null) {
            cardEntradas.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EntradasActivity.class)));
        }

        // Informe de Stock
        findViewById(R.id.cardInforme).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, InformeStockActivity.class)));

        // Rol CONSULTA (3): Oculta solo operaciones de modificación (Entradas a Bodega y Gestión de Usuarios)
        int rolActual = session.getRoleId();
        if (rolActual != 1 && rolActual != 2) {
            if (cardEntradas != null) cardEntradas.setVisibility(View.GONE);
            cardUsuarios.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session.isLoggedIn()) {
            actualizarMetricas();
        }
    }

    private void actualizarMetricas() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        int totalProductos = 0;
        int stockBajoCount = 0;

        // Total de productos activos
        try (Cursor cursorTotal = db.rawQuery("SELECT COUNT(*) FROM producto WHERE activo = 1", null)) {
            if (cursorTotal.moveToFirst()) {
                totalProductos = cursorTotal.getInt(0);
            }
        } catch (Exception ignored) {}

        // Productos con stock actual <= stock minimo
        try (Cursor cursorBajo = db.rawQuery("SELECT COUNT(*) FROM producto WHERE activo = 1 AND stock_actual <= stock_minimo", null)) {
            if (cursorBajo.moveToFirst()) {
                stockBajoCount = cursorBajo.getInt(0);
            }
        } catch (Exception ignored) {}

        tvTotalProductos.setText(String.valueOf(totalProductos));
        tvStockBajoCount.setText(String.valueOf(stockBajoCount));
    }

    private void irALogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}