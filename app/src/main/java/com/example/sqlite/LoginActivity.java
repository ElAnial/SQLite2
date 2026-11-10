package com.example.sqlite;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private SessionManager session;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Si ya hay una sesión activa (CU-01), omitir login e ir al Dashboard
        if (session.isLoggedIn()) {
            irAHome();
            return;
        }

        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> realizarLogin());
    }

    private void realizarLogin() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Ingrese su usuario");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingrese su contraseña");
            return;
        }

        String passwordHash = DBHelper.hashPassword(password);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Consulta parametrizada uniendo usuario y rol para validar activo = 1
        String sql = "SELECT u.id_usuario, u.username, u.nombre_completo, u.id_rol, r.nombre_rol " +
                "FROM usuario u " +
                "INNER JOIN rol r ON u.id_rol = r.id_rol " +
                "WHERE u.username = ? AND u.password_hash = ? AND u.activo = 1";

        try (Cursor cursor = db.rawQuery(sql, new String[]{username, passwordHash})) {
            if (cursor.moveToFirst()) {
                int idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"));
                String user = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow("nombre_completo"));
                int idRol = cursor.getInt(cursor.getColumnIndexOrThrow("id_rol"));
                String nombreRol = cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol"));

                // Guardar sesión de usuario (CU-01)
                session.createLoginSession(idUsuario, user, nombreCompleto, idRol, nombreRol);

                Toast.makeText(this, "Bienvenido " + nombreCompleto, Toast.LENGTH_SHORT).show();
                irAHome();
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos (o usuario inactivo)", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error en el inicio de sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void irAHome() {
        if (session.getRoleId() == 3) {
            Intent intent = new Intent(LoginActivity.this, TiendaProductosActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
