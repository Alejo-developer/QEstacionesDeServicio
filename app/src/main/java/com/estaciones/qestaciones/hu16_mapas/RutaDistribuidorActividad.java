package com.estaciones.qestaciones.hu16_mapas;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lab Maps Intent — el DISTRIBUIDOR selecciona las estaciones a las que va a
 * entregar combustible hoy y lanza Google Maps con la ruta optimizada.
 *
 * Construye una URL del tipo:
 *   https://www.google.com/maps/dir/?api=1
 *     &origin={lat,lng del distribuidor}
 *     &destination={última estación seleccionada}
 *     &waypoints={A|B|C}
 *     &travelmode=driving
 *
 * Google Maps reordena los waypoints para minimizar la ruta cuando se pasa
 * waypoints; aquí pasamos el orden tal cual lo eligió el usuario y dejamos que
 * Maps decida la mejor secuencia visualmente.
 */
public class RutaDistribuidorActividad extends AppCompatActivity {

    private UsuarioDao usuarioDao;
    private Usuario distribuidor;

    private TextView textoOrigen;
    private TextView textoSinEstaciones;
    private TextView textoResumen;
    private RecyclerView lista;
    private EstacionesAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_ruta_distribuidor);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();

        textoOrigen = findViewById(R.id.textoOrigenRuta);
        textoSinEstaciones = findViewById(R.id.textoSinEstaciones);
        textoResumen = findViewById(R.id.textoResumenRuta);
        lista = findViewById(R.id.listaEstacionesRuta);
        lista.setLayoutManager(new LinearLayoutManager(this));

        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario == null) {
            Toast.makeText(this, "Sesión no encontrada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        distribuidor = usuarioDao.buscarPorNombreUsuario(nombreUsuario);
        if (distribuidor == null) {
            Toast.makeText(this, "Distribuidor no encontrado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        actualizarEncabezadoOrigen(nombreUsuario);

        adaptador = new EstacionesAdaptador(this::actualizarResumen);
        lista.setAdapter(adaptador);

        findViewById(R.id.botonIniciarRuta).setOnClickListener(v -> iniciarRuta());
        findViewById(R.id.botonVolverRuta).setOnClickListener(v -> finish());

        cargarEstaciones();
        actualizarResumen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si el distribuidor regresa de editar su ubicación, refrescamos el origen.
        if (distribuidor != null) {
            distribuidor = usuarioDao.buscarPorNombreUsuario(distribuidor.getNombreUsuario());
            actualizarEncabezadoOrigen(distribuidor != null ? distribuidor.getNombreUsuario() : null);
        }
        cargarEstaciones();
        actualizarResumen();
    }

    /**
     * Pinta el encabezado mostrando la dirección legible del distribuidor.
     * Si no tiene dirección pero sí coords, cae a las coords. Si no tiene nada,
     * pide al usuario que registre la ubicación.
     */
    private void actualizarEncabezadoOrigen(String nombreUsuario) {
        if (distribuidor == null) return;
        String nombre = distribuidor.getNombreCompleto() != null
                ? distribuidor.getNombreCompleto()
                : (nombreUsuario != null ? nombreUsuario : "");
        String direccion = direccionLegible(distribuidor);
        if (direccion != null) {
            textoOrigen.setText("Origen: " + nombre + " · " + direccion);
        } else if (distribuidor.getLatitud() != null && distribuidor.getLongitud() != null) {
            textoOrigen.setText("Origen: " + nombre
                    + " (" + distribuidor.getLatitud() + ", " + distribuidor.getLongitud() + ")");
        } else {
            textoOrigen.setText("⚠ Aún no tienes ubicación registrada. Ve a 'Mi ubicación' antes de armar la ruta.");
        }
    }

    /**
     * Devuelve "Dirección, Ciudad" si ambos existen; solo dirección o solo ciudad
     * si falta el otro; null si no hay nada. Maps acepta este texto en sus URLs
     * (lo geocodifica del lado servidor), así que es la forma más legible
     * y robusta de pasar una ubicación.
     */
    private static String direccionLegible(Usuario u) {
        if (u == null) return null;
        String d = u.getDireccion();
        String c = u.getCiudad();
        boolean hayD = d != null && !d.trim().isEmpty();
        boolean hayC = c != null && !c.trim().isEmpty();
        if (hayD && hayC) return d.trim() + ", " + c.trim();
        if (hayD) return d.trim();
        if (hayC) return c.trim();
        return null;
    }

    /**
     * Construye el "punto" a pasar a Maps. Preferencia: dirección + ", Colombia"
     * (forzamos país para que Maps geocodifique siempre en Colombia, no en otro
     * sitio con calle del mismo nombre). Si no hay dirección, cae a coords lat,lng.
     * Si no hay ninguna de las dos, devuelve null.
     */
    private static String ubicacionParaMapa(Usuario u) {
        if (u == null) return null;
        String dir = direccionLegible(u);
        if (dir != null) {
            // Forzar país. Si el usuario ya escribió "Colombia" en la ciudad lo dejamos igual.
            if (!dir.toLowerCase().contains("colombia")) dir = dir + ", Colombia";
            return dir;
        }
        if (u.getLatitud() != null && u.getLongitud() != null) {
            return u.getLatitud() + "," + u.getLongitud();
        }
        return null;
    }

    private void cargarEstaciones() {
        List<Usuario> estaciones = usuarioDao.obtenerEstacionesConCoordenadas();
        adaptador.actualizar(estaciones);
        textoSinEstaciones.setVisibility(estaciones.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void actualizarResumen() {
        int n = adaptador.seleccionadas().size();
        textoResumen.setText(n + (n == 1 ? " parada seleccionada" : " paradas seleccionadas"));
    }

    /**
     * Construye y lanza el intent de Google Maps con la ruta. Reglas:
     *  - Origen = dirección del distribuidor (o coords si no tiene dirección).
     *  - Si hay 1 sola parada: solo destination.
     *  - Si hay 2+: la última es destination, las anteriores son waypoints.
     *  - Cada parada se representa por su dirección+ciudad si existe; si no,
     *    cae a coords. Maps geocodifica el texto del lado servidor, así que
     *    direcciones legibles como "Av. Caracas #50-10, Bogotá" funcionan perfecto.
     *  - travelmode=driving para que cuente kilometraje en auto.
     */
    private void iniciarRuta() {
        String origen = ubicacionParaMapa(distribuidor);
        if (origen == null) {
            Toast.makeText(this, "Registra tu ubicación primero (Mi ubicación o Mi Perfil).",
                    Toast.LENGTH_LONG).show();
            return;
        }
        List<Usuario> paradas = adaptador.seleccionadas();
        if (paradas.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una estación.", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario destinoEstacion = paradas.get(paradas.size() - 1);
        String destino = ubicacionParaMapa(destinoEstacion);
        if (destino == null) {
            Toast.makeText(this, "La estación destino no tiene ubicación registrada.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("www.google.com")
                .appendPath("maps")
                .appendPath("dir")
                .appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("origin", origen)
                .appendQueryParameter("destination", destino)
                .appendQueryParameter("travelmode", "driving")
                // region=co sesga la geocodificación hacia Colombia: si dos calles
                // se llaman igual en distintos países, Maps prefiere la colombiana.
                .appendQueryParameter("region", "co");

        if (paradas.size() > 1) {
            StringBuilder waypoints = new StringBuilder();
            boolean primero = true;
            for (int i = 0; i < paradas.size() - 1; i++) {
                String wp = ubicacionParaMapa(paradas.get(i));
                if (wp == null) continue; // saltamos paradas sin ubicación
                if (!primero) waypoints.append("|");
                waypoints.append(wp);
                primero = false;
            }
            if (waypoints.length() > 0) {
                builder.appendQueryParameter("waypoints", waypoints.toString());
            }
        }

        Uri uri = builder.build();

        // 1) Intentar con Google Maps app explícitamente
        Intent appIntent = new Intent(Intent.ACTION_VIEW, uri);
        appIntent.setPackage("com.google.android.apps.maps");
        if (appIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(appIntent);
            return;
        }

        // 2) Cualquier app que resuelva el URL https de Maps (Maps web, navegador, etc.)
        Intent generico = new Intent(Intent.ACTION_VIEW, uri);
        if (generico.resolveActivity(getPackageManager()) != null) {
            startActivity(generico);
        } else {
            Toast.makeText(this, "No hay una app de mapas ni navegador disponible.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Adaptador ----------

    /** Callback que se dispara cuando cambia la selección de cualquier checkbox. */
    private interface CambioSeleccion { void onCambio(); }

    private static class EstacionesAdaptador extends RecyclerView.Adapter<EstacionesAdaptador.VH> {
        private List<Usuario> items = new ArrayList<>();
        private final Set<Integer> seleccionados = new HashSet<>();
        private final CambioSeleccion callback;

        EstacionesAdaptador(CambioSeleccion callback) { this.callback = callback; }

        void actualizar(List<Usuario> nuevos) {
            items = nuevos;
            // Mantener la selección entre recargas para los IDs que sigan presentes
            Set<Integer> aun = new HashSet<>();
            for (Usuario u : nuevos) {
                if (seleccionados.contains(u.getId())) aun.add(u.getId());
            }
            seleccionados.clear();
            seleccionados.addAll(aun);
            notifyDataSetChanged();
        }

        List<Usuario> seleccionadas() {
            List<Usuario> r = new ArrayList<>();
            for (Usuario u : items) {
                if (seleccionados.contains(u.getId())) r.add(u);
            }
            return r;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_estacion_ruta, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Usuario u = items.get(position);
            h.nombre.setText(u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getNombreUsuario());

            // Dirección legible es el dato principal; las coords quedan como
            // detalle técnico secundario (oculto si la estación no tiene coords).
            String dir = direccionLegible(u);
            h.direccion.setText(dir != null ? "📍 " + dir : "📍 Sin dirección registrada");

            if (u.getLatitud() != null && u.getLongitud() != null) {
                h.coords.setVisibility(View.VISIBLE);
                h.coords.setText("(" + u.getLatitud() + ", " + u.getLongitud() + ")");
            } else {
                h.coords.setVisibility(View.GONE);
            }

            // Evitar listener fantasma en reciclaje
            h.check.setOnCheckedChangeListener(null);
            h.check.setChecked(seleccionados.contains(u.getId()));
            h.check.setOnCheckedChangeListener((cv, checked) -> {
                if (checked) seleccionados.add(u.getId());
                else seleccionados.remove(u.getId());
                callback.onCambio();
            });
            h.itemView.setOnClickListener(v -> h.check.setChecked(!h.check.isChecked()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            MaterialCheckBox check;
            TextView nombre, direccion, coords;
            VH(View v) {
                super(v);
                check = v.findViewById(R.id.checkEstacionRuta);
                nombre = v.findViewById(R.id.textoNombreEstacionRuta);
                direccion = v.findViewById(R.id.textoDireccionEstacionRuta);
                coords = v.findViewById(R.id.textoCoordsEstacionRuta);
            }
        }
    }
}
