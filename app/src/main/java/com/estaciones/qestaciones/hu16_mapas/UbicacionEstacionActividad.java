package com.estaciones.qestaciones.hu16_mapas;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Lab Maps Intent — pantalla para que una ESTACION (o DISTRIBUIDOR, USUARIO_PARTICULAR)
 * registre su ubicación geográfica.
 *
 * Tres formas de fijar la ubicación:
 *  1) Entrada manual de latitud/longitud.
 *  2) Botón "Usar mi ubicación actual" → lee del GPS con permiso runtime.
 *  3) Botón "Ver en Google Maps" → abre el Intent del mapa con lo que esté en pantalla
 *     (útil para confirmar visualmente antes de guardar).
 */
public class UbicacionEstacionActividad extends AppCompatActivity {

    private static final int CODIGO_PERMISO_GPS = 2101;

    private UsuarioDao usuarioDao;
    private LocationManager locationManager;
    private Usuario usuarioActual;

    private TextInputEditText campoLatitud;
    private TextInputEditText campoLongitud;
    private TextView textoEstadoGps;
    private TextView textoUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_ubicacion_estacion);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        campoLatitud = findViewById(R.id.campoLatitud);
        campoLongitud = findViewById(R.id.campoLongitud);
        textoEstadoGps = findViewById(R.id.textoEstadoGps);
        textoUsuario = findViewById(R.id.textoUsuarioUbicacion);

        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario == null) {
            Toast.makeText(this, "Sesión no encontrada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        usuarioActual = usuarioDao.buscarPorNombreUsuario(nombreUsuario);
        if (usuarioActual == null) {
            Toast.makeText(this, "Usuario no encontrado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        textoUsuario.setText((usuarioActual.getNombreCompleto() != null
                ? usuarioActual.getNombreCompleto() : nombreUsuario)
                + " · @" + nombreUsuario);

        if (usuarioActual.getLatitud() != null) {
            campoLatitud.setText(String.valueOf(usuarioActual.getLatitud()));
        }
        if (usuarioActual.getLongitud() != null) {
            campoLongitud.setText(String.valueOf(usuarioActual.getLongitud()));
        }

        MaterialButton botonGps = findViewById(R.id.botonUsarGps);
        MaterialButton botonMapa = findViewById(R.id.botonVerEnMapa);
        MaterialButton botonGuardar = findViewById(R.id.botonGuardarUbicacion);
        MaterialButton botonVolver = findViewById(R.id.botonVolverUbicacion);

        botonGps.setOnClickListener(v -> solicitarUbicacionGps());
        botonMapa.setOnClickListener(v -> abrirEnGoogleMaps());
        botonGuardar.setOnClickListener(v -> guardarUbicacion());
        botonVolver.setOnClickListener(v -> finish());
    }

    /**
     * Si ya tenemos el permiso, leemos del GPS. Si no, lo pedimos; el resultado
     * regresa por onRequestPermissionsResult y desde allí reintentamos.
     */
    private void solicitarUbicacionGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    CODIGO_PERMISO_GPS);
            return;
        }
        leerGps();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CODIGO_PERMISO_GPS) return;

        boolean concedido = false;
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_GRANTED) { concedido = true; break; }
        }
        if (concedido) {
            leerGps();
        } else {
            mostrarEstadoGps("Permiso de ubicación denegado. Puedes ingresar las coordenadas manualmente.");
        }
    }

    /**
     * Intenta primero con la última ubicación conocida (instantánea). Si no hay,
     * se suscribe a una sola actualización por GPS y por NETWORK; el primer
     * proveedor que responda gana y nos desuscribimos.
     */
    private void leerGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mostrarEstadoGps("Falta el permiso de ubicación.");
            return;
        }

        boolean gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean redActiva = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsActivo && !redActiva) {
            mostrarEstadoGps("Activa el GPS o la ubicación por red en los ajustes del dispositivo.");
            return;
        }

        // Última ubicación conocida (no pega al GPS, instantánea)
        Location ultima = null;
        try {
            if (gpsActivo) ultima = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (ultima == null && redActiva) {
                ultima = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException ignored) {}

        if (ultima != null) {
            aplicarUbicacion(ultima, "última conocida");
            return;
        }

        mostrarEstadoGps("Buscando señal del GPS… (puede tardar unos segundos)");

        final LocationListener[] listenerRef = new LocationListener[1];
        listenerRef[0] = new LocationListener() {
            @Override public void onLocationChanged(@NonNull Location location) {
                try { locationManager.removeUpdates(listenerRef[0]); } catch (SecurityException ignored) {}
                aplicarUbicacion(location, location.getProvider());
            }
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        try {
            if (gpsActivo) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listenerRef[0], null);
            }
            if (redActiva) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listenerRef[0], null);
            }
        } catch (SecurityException e) {
            mostrarEstadoGps("No se pudo acceder al GPS: " + e.getMessage());
        }
    }

    private void aplicarUbicacion(@NonNull Location loc, @Nullable String proveedor) {
        campoLatitud.setText(String.valueOf(loc.getLatitude()));
        campoLongitud.setText(String.valueOf(loc.getLongitude()));
        String prov = proveedor != null ? proveedor : "GPS";
        mostrarEstadoGps("✅ Ubicación obtenida (" + prov + ", ±"
                + Math.round(loc.getAccuracy()) + " m). Revisa y guarda.");
    }

    private void mostrarEstadoGps(String mensaje) {
        textoEstadoGps.setVisibility(View.VISIBLE);
        textoEstadoGps.setText(mensaje);
    }

    /**
     * Lanza un intent para ver las coords en mapas. Cascada de fallbacks:
     *  1) geo:lat,lng?q=lat,lng(Nombre) con setPackage de Google Maps.
     *  2) Mismo geo: pero sin package — cualquier app de mapas instalada.
     *  3) https://www.google.com/maps/search/?api=1&query=lat,lng — abre en el
     *     navegador. Útil para emuladores sin Google Play (no tienen Maps).
     */
    private void abrirEnGoogleMaps() {
        Double[] coords = parsearCoords();
        if (coords == null) return;

        String etiqueta = usuarioActual.getNombreCompleto() != null
                ? usuarioActual.getNombreCompleto() : usuarioActual.getNombreUsuario();

        // 1) Google Maps app (geo:)
        String geoUri = "geo:" + coords[0] + "," + coords[1]
                + "?q=" + coords[0] + "," + coords[1] + "(" + Uri.encode(etiqueta) + ")";
        Intent mapsApp = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        mapsApp.setPackage("com.google.android.apps.maps");
        if (mapsApp.resolveActivity(getPackageManager()) != null) {
            startActivity(mapsApp);
            return;
        }

        // 2) Cualquier app de mapas que entienda geo:
        Intent cualquierMapa = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        if (cualquierMapa.resolveActivity(getPackageManager()) != null) {
            startActivity(cualquierMapa);
            return;
        }

        // 3) Fallback al navegador con Google Maps web
        String webUri = "https://www.google.com/maps/search/?api=1&query="
                + coords[0] + "," + coords[1];
        Intent navegador = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
        if (navegador.resolveActivity(getPackageManager()) != null) {
            startActivity(navegador);
        } else {
            Toast.makeText(this, "No hay una app de mapas ni navegador disponible.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void guardarUbicacion() {
        Double[] coords = parsearCoords();
        if (coords == null) return;

        usuarioDao.actualizarUbicacion(usuarioActual.getId(), coords[0], coords[1]);
        usuarioActual.setLatitud(coords[0]);
        usuarioActual.setLongitud(coords[1]);
        Toast.makeText(this, "Ubicación guardada.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Valida y parsea los dos EditText. Devuelve {lat, lng} o null si hay error
     * (ya mostró el Toast).
     */
    @Nullable
    private Double[] parsearCoords() {
        String latStr = campoLatitud.getText() != null ? campoLatitud.getText().toString().trim() : "";
        String lngStr = campoLongitud.getText() != null ? campoLongitud.getText().toString().trim() : "";
        if (TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lngStr)) {
            Toast.makeText(this, "Ingresa latitud y longitud.", Toast.LENGTH_SHORT).show();
            return null;
        }
        double lat, lng;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Latitud y longitud deben ser números.", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            Toast.makeText(this, "Coordenadas fuera de rango (lat: -90..90, lng: -180..180).",
                    Toast.LENGTH_LONG).show();
            return null;
        }
        return new Double[]{lat, lng};
    }
}
