package com.example.sqlite;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Actividad para visualizar el Historial de Auditoría del Sistema de Bodega.
 * Aplica Control de Acceso basado en Roles (RBAC).
 */
public class RegistrosActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DBHelper dbHelper;

    private RecyclerView rvRegistros;
    private TextView tvVacioRegistros;
    private MaterialButton btnVolverRegistros;
    private RegistrosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Control de Acceso por Roles (RBAC)
        sessionManager = new SessionManager(this);

        // Verificar si la sesión está activa (Todos los roles autenticados pueden ver el historial)
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Debe iniciar sesión para acceder", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Cargar vista e inicializar componentes
        setContentView(R.layout.activity_registros);

        dbHelper = new DBHelper(this);

        rvRegistros = findViewById(R.id.rvRegistros);
        tvVacioRegistros = findViewById(R.id.tvVacioRegistros);
        btnVolverRegistros = findViewById(R.id.btnVolverRegistros);

        // Configurar RecyclerView
        rvRegistros.setLayoutManager(new LinearLayoutManager(this));

        // Evento botón "Volver": Cierra la actividad y regresa al dashboard
        btnVolverRegistros.setOnClickListener(v -> finish());

        // Cargar registros desde la base de datos
        cargarHistorial();
    }

    /**
     * Recupera el historial completo desde la base de datos y lo asigna al RecyclerView.
     */
    private void cargarHistorial() {
        List<RegistroAudit> lista = dbHelper.obtenerHistorialCompleto();

        if (lista == null || lista.isEmpty()) {
            tvVacioRegistros.setVisibility(View.VISIBLE);
            rvRegistros.setVisibility(View.GONE);
        } else {
            tvVacioRegistros.setVisibility(View.GONE);
            rvRegistros.setVisibility(View.VISIBLE);

            adapter = new RegistrosAdapter(this, lista);
            rvRegistros.setAdapter(adapter);
        }
    }
}