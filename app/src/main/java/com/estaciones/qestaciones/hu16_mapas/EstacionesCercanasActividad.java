package com.estaciones.qestaciones.hu16_mapas;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Lab Maps Intent — pantalla para USUARIO_PARTICULAR.
 *
 * Muestra las estaciones registradas y permite:
 *  - 📍 Ver en mapa: abre Google Maps con un pin sobre la estación.
 *  - 🧭 Cómo llegar: abre Google Maps con la ruta desde la ubicación del
 *    cliente (si la tiene registrada) o desde la ubicación actual del
 *    dispositivo (omitiendo origin, Maps usa el GPS).
 */
public class EstacionesCercanasActividad extends AppCompatActivity {

    private UsuarioDao usuarioDao;
    private Usuario cliente;

    private TextView textoUbicacionCliente;
    private TextView textoSinEstaciones;
    private RecyclerView lista;
    private EstacionesCercanasAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_estaciones_cercanas);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();

        textoUbicacionCliente = findViewById(R.id.textoUbicacionCliente);
        textoSinEstaciones = findViewById(R.id.textoSinEstacionesCercanas);
        lista = findViewById(R.id.listaEstacionesCercanas);
        lista.setLayoutManager(new LinearLayoutManager(this));

        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario == null) {
            Toast.makeText(this, "Sesión no encontrada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        cliente = usuarioDao.buscarPorNombreUsuario(nombreUsuario);

        adaptador = new EstacionesCercanasAdaptador(
                this::verEstacionEnMapa,
                this::comoLlegarA);
        lista.setAdapter(adaptador);

        ((MaterialButton) findViewById(R.id.botonVolverEstacionesCercanas))
                .setOnClickListener(v -> finish());

        actualizarEncabezadoCliente();
        cargarEstaciones();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Por si el cliente editó su perfil/ubicación en otra pantalla
        if (cliente != null) {
            cliente = usuarioDao.buscarPorNombreUsuario(cliente.getNombreUsuario());
        }
        actualizarEncabezadoCliente();
        cargarEstaciones();
    }

    private void actualizarEncabezadoCliente() {
        if (cliente == null) return;
        String dir = direccionLegible(cliente);
        if (dir != null) {
            textoUbicacionCliente.setText("Tu ubicación: " + dir);
        } else if (cliente.getLatitud() != null && cliente.getLongitud() != null) {
            textoUbicacionCliente.setText("Tu ubicación: ("
                    + cliente.getLatitud() + ", " + cliente.getLongitud() + ")");
        } else {
            textoUbicacionCliente.setText("Sin ubicación propia · Maps usará el GPS del teléfono al iniciar la ruta.");
        }
    }

    private void cargarEstaciones() {
        List<Usuario> estaciones = usuarioDao.obtenerEstacionesConCoordenadas();
        adaptador.actualizar(estaciones);
        textoSinEstaciones.setVisibility(estaciones.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- Acciones de Maps ----------

    /**
     * Abre Maps con un pin sobre la estación. Si la estación tiene coords usamos
     * geo:lat,lng?q=lat,lng(Nombre) — con etiqueta visible en el pin. Si solo
     * tiene dirección (sin coords), caemos a la búsqueda por texto.
     */
    private void verEstacionEnMapa(Usuario estacion) {
        String etiqueta = estacion.getNombreCompleto() != null
                ? estacion.getNombreCompleto() : estacion.getNombreUsuario();

        Uri uri;
        if (estacion.getLatitud() != null && estacion.getLongitud() != null) {
            uri = Uri.parse("geo:" + estacion.getLatitud() + "," + estacion.getLongitud()
                    + "?q=" + estacion.getLatitud() + "," + estacion.getLongitud()
                    + "(" + Uri.encode(etiqueta) + ")");
        } else {
            String dir = ubicacionParaMapa(estacion);
            if (dir == null) {
                Toast.makeText(this, "Esa estación no tiene ubicación registrada.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            uri = Uri.parse("geo:0,0?q=" + Uri.encode(dir));
        }
        abrirEnMaps(uri, "https://www.google.com/maps/search/?api=1&query="
                + Uri.encode(ubicacionParaMapa(estacion)));
    }

    /**
     * Abre Maps con una ruta desde la ubicación del cliente hasta la estación.
     * Si el cliente no tiene ubicación registrada, omitimos el origin y Maps
     * usa automáticamente la ubicación actual del dispositivo (GPS).
     */
    private void comoLlegarA(Usuario estacion) {
        String destino = ubicacionParaMapa(estacion);
        if (destino == null) {
            Toast.makeText(this, "Esa estación no tiene ubicación registrada.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Uri.Builder b = new Uri.Builder()
                .scheme("https")
                .authority("www.google.com")
                .appendPath("maps")
                .appendPath("dir")
                .appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", destino)
                .appendQueryParameter("travelmode", "driving")
                // region=co sesga la geocodificación hacia Colombia.
                .appendQueryParameter("region", "co");

        String origen = ubicacionParaMapa(cliente);
        if (origen != null) b.appendQueryParameter("origin", origen);
        // Si no hay origen, Maps usa la ubicación actual del dispositivo automáticamente.

        Uri uri = b.build();
        abrirEnMaps(uri, uri.toString()); // mismo URL https sirve como fallback web
    }

    /**
     * Cascada de intents. Primero intenta abrir en Google Maps app explícitamente,
     * luego en cualquier app que entienda el URI, luego cae al navegador con la
     * URL https equivalente.
     */
    private void abrirEnMaps(Uri uri, String urlWebFallback) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, uri);
        appIntent.setPackage("com.google.android.apps.maps");
        if (appIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(appIntent);
            return;
        }
        Intent generico = new Intent(Intent.ACTION_VIEW, uri);
        if (generico.resolveActivity(getPackageManager()) != null) {
            startActivity(generico);
            return;
        }
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(urlWebFallback));
        if (web.resolveActivity(getPackageManager()) != null) {
            startActivity(web);
        } else {
            Toast.makeText(this, "No hay una app de mapas ni navegador disponible.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Helpers de dirección (duplicados de RutaDistribuidorActividad
    //            para mantener cada activity independiente) ----------

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

    private static String ubicacionParaMapa(Usuario u) {
        if (u == null) return null;
        String dir = direccionLegible(u);
        if (dir != null) {
            if (!dir.toLowerCase().contains("colombia")) dir = dir + ", Colombia";
            return dir;
        }
        if (u.getLatitud() != null && u.getLongitud() != null) {
            return u.getLatitud() + "," + u.getLongitud();
        }
        return null;
    }

    // ---------- Adaptador ----------

    private interface AccionEstacion { void ejecutar(Usuario u); }

    private static class EstacionesCercanasAdaptador
            extends RecyclerView.Adapter<EstacionesCercanasAdaptador.VH> {

        private List<Usuario> items = new ArrayList<>();
        private final AccionEstacion onVerMapa;
        private final AccionEstacion onComoLlegar;

        EstacionesCercanasAdaptador(AccionEstacion onVerMapa, AccionEstacion onComoLlegar) {
            this.onVerMapa = onVerMapa;
            this.onComoLlegar = onComoLlegar;
        }

        void actualizar(List<Usuario> nuevos) { items = nuevos; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_estacion_cercana, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Usuario u = items.get(position);
            h.nombre.setText(u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getNombreUsuario());
            String dir = direccionLegible(u);
            h.direccion.setText(dir != null ? "📍 " + dir : "📍 Sin dirección registrada");
            if (u.getTelefono() != null && !u.getTelefono().isEmpty()) {
                h.telefono.setVisibility(View.VISIBLE);
                h.telefono.setText("📞 " + u.getTelefono());
            } else {
                h.telefono.setVisibility(View.GONE);
            }

            h.botonVerMapa.setOnClickListener(v -> onVerMapa.ejecutar(u));
            h.botonComoLlegar.setOnClickListener(v -> onComoLlegar.ejecutar(u));
            // Tap en la tarjeta = ver en mapa (acción no destructiva, "peek")
            h.itemView.setOnClickListener(v -> onVerMapa.ejecutar(u));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView nombre, direccion, telefono;
            MaterialButton botonVerMapa, botonComoLlegar;
            VH(View v) {
                super(v);
                nombre = v.findViewById(R.id.textoNombreEstacionCercana);
                direccion = v.findViewById(R.id.textoDireccionEstacionCercana);
                telefono = v.findViewById(R.id.textoTelefonoEstacionCercana);
                botonVerMapa = v.findViewById(R.id.botonVerEnMapaEstacionCercana);
                botonComoLlegar = v.findViewById(R.id.botonComoLlegarEstacionCercana);
            }
        }
    }
}
