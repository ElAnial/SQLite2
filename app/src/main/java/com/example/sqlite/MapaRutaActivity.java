package com.example.sqlite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class MapaRutaActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    // Coordenadas fijas de la Empresa / Bodega Central
    private static final double EMPRESA_LAT = -33.4489;
    private static final double EMPRESA_LNG = -70.6693;
    private static final String EMPRESA_NOMBRE = "Bodega Central (Empresa)";

    private TextView tvEstadoUbicacion, tvDetalleRuta;
    private MaterialButton btnVolverMapa, btnMiUbicacion, btnVerEmpresa;
    private WebView webViewMapa;

    private LocationManager locationManager;
    private Location userLocation;
    private boolean isMapLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_ruta);

        tvEstadoUbicacion = findViewById(R.id.tvEstadoUbicacion);
        tvDetalleRuta = findViewById(R.id.tvDetalleRuta);
        btnVolverMapa = findViewById(R.id.btnVolverMapa);
        btnMiUbicacion = findViewById(R.id.btnMiUbicacion);
        btnVerEmpresa = findViewById(R.id.btnVerEmpresa);
        webViewMapa = findViewById(R.id.webViewMapa);

        btnVolverMapa.setOnClickListener(v -> finish());
        btnMiUbicacion.setOnClickListener(v -> centrarEnUsuario());
        btnVerEmpresa.setOnClickListener(v -> centrarEnEmpresa());

        configurarWebView();
        solicitarPermisosYUbicacion();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarWebView() {
        WebSettings settings = webViewMapa.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        webViewMapa.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isMapLoaded = true;
                if (userLocation != null) {
                    actualizarMapaConRuta();
                }
            }
        });

        cargarMapaInicial();
    }

    private void cargarMapaInicial() {
        String html = generarHTMLMapa(EMPRESA_LAT, EMPRESA_LNG, EMPRESA_LAT, EMPRESA_LNG, false);
        webViewMapa.loadDataWithBaseURL("https://openstreetmap.org/", html, "text/html", "UTF-8", null);
    }

    private void solicitarPermisosYUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            obtenerUbicacionGPS();
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionGPS() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            tvEstadoUbicacion.setText("GPS no disponible en este dispositivo");
            return;
        }

        Location lastKnown = null;
        try {
            lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception ignored) {}

        if (lastKnown != null) {
            procesarNuevaUbicacion(lastKnown);
        }

        try {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    procesarNuevaUbicacion(location);
                }

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            };

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al solicitar actualizaciones de GPS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarNuevaUbicacion(Location location) {
        this.userLocation = location;

        double userLat = location.getLatitude();
        double userLng = location.getLongitude();

        Location locEmpresa = new Location("empresa");
        locEmpresa.setLatitude(EMPRESA_LAT);
        locEmpresa.setLongitude(EMPRESA_LNG);

        float distanciaMetros = location.distanceTo(locEmpresa);
        float distanciaKm = distanciaMetros / 1000f;

        int tiempoEstimadoMin = Math.round((distanciaKm / 35f) * 60f); // estimado a 35 km/h
        if (tiempoEstimadoMin < 1) tiempoEstimadoMin = 1;

        tvEstadoUbicacion.setText(String.format(Locale.getDefault(), "Tu ubicación: Lat %.4f, Lng %.4f", userLat, userLng));
        tvDetalleRuta.setText(String.format(Locale.getDefault(), "Ruta hacia Empresa: %.2f km (Tiempo estimado: %d min)", distanciaKm, tiempoEstimadoMin));

        if (isMapLoaded) {
            actualizarMapaConRuta();
        }
    }

    private void actualizarMapaConRuta() {
        if (userLocation == null) return;
        String html = generarHTMLMapa(userLocation.getLatitude(), userLocation.getLongitude(), EMPRESA_LAT, EMPRESA_LNG, true);
        webViewMapa.loadDataWithBaseURL("https://openstreetmap.org/", html, "text/html", "UTF-8", null);
    }

    private void centrarEnUsuario() {
        if (userLocation != null) {
            webViewMapa.evaluateJavascript("if (typeof map !== 'undefined' && typeof userMarker !== 'undefined') { map.setView([ " + userLocation.getLatitude() + ", " + userLocation.getLongitude() + " ], 15); userMarker.openPopup(); }", null);
        } else {
            Toast.makeText(this, "Aún no se ha obtenido la ubicación GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private void centrarEnEmpresa() {
        webViewMapa.evaluateJavascript("if (typeof map !== 'undefined' && typeof empresaMarker !== 'undefined') { map.setView([ " + EMPRESA_LAT + ", " + EMPRESA_LNG + " ], 15); empresaMarker.openPopup(); }", null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionGPS();
            } else {
                tvEstadoUbicacion.setText("Permiso de GPS denegado. Mostrando solo ubicación de Empresa.");
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String generarHTMLMapa(double userLat, double userLng, double empLat, double empLng, boolean conRuta) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "   <meta charset=\"utf-8\" />\n" +
                "   <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "   <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "   <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "   <style>\n" +
                "       html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }\n" +
                "   </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "   <div id=\"map\"></div>\n" +
                "   <script>\n" +
                "       var map = L.map('map');\n" +
                "       L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "           maxZoom: 19,\n" +
                "           attribution: '© OpenStreetMap'\n" +
                "       }).addTo(map);\n" +
                "\n" +
                "       var empresaMarker = L.marker([" + empLat + ", " + empLng + "]).addTo(map).bindPopup('<b>" + EMPRESA_NOMBRE + "</b><br>Destino de la Bodega');\n" +
                "\n" +
                (conRuta ?
                        "       var userMarker = L.marker([" + userLat + ", " + userLng + "]).addTo(map).bindPopup('<b>Tu Ubicación Actual</b>');\n" +
                                "       var latlngs = [ [" + userLat + ", " + userLng + "], [" + empLat + ", " + empLng + "] ];\n" +
                                "       var polyline = L.polyline(latlngs, {color: '#1E88E5', weight: 4, opacity: 0.8, dashArray: '8, 8'}).addTo(map);\n" +
                                "       map.fitBounds(polyline.getBounds(), {padding: [50, 50]});\n" :
                        "       map.setView([" + empLat + ", " + empLng + "], 14);\n") +
                "   </script>\n" +
                "</body>\n" +
                "</html>";
    }
}