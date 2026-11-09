package com.example.sqlite;

import android.content.Context;

/**
 * Clase alias de DBHelper para compatibilidad con la denominación 'DatabaseHelper'.
 */
public class DatabaseHelper extends DBHelper {

    public DatabaseHelper(Context context) {
        super(context);
    }
}
