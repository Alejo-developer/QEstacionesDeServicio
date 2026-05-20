package com.estaciones.qestaciones.hu05_subsidios;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.ValidacionSubsidioDao;
import com.estaciones.qestaciones.hu18_servicios.NotificadorEventosServicio;
import com.estaciones.qestaciones.modelo.ValidacionSubsidio;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para validar subsidios por placa de vehículo.
 */
public class ValidacionSubsidioActividad extends AppCompatActivity {

    private TextInputEditText campoPlaca;
    private CardView tarjetaResultado;
    private TextView textoResultado;
    private TextView textoPrecio;
    private TextView textoTipoVehiculo;
    private TextView textoMotivo;
    private RecyclerView listaValidaciones;
    private AdaptadorValidaciones adaptador;
    private TextView textoSinValidaciones;

    private GestorSubsidios gestorSubsidios;
    private ValidacionSubsidioDao subsidioDao;
    private String rol;
    private boolean puedeEliminar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_validacion_subsidio);

        gestorSubsidios = new GestorSubsidios(this);
        subsidioDao = BaseDatosEstaciones.obtenerInstancia(this).validacionSubsidioDao();

        rol = getIntent().getStringExtra("rol");
        puedeEliminar = "ADMINISTRADOR".equals(rol) || "REGULADOR".equals(rol);

        inicializarVistas();
        configurarEventos();
        cargarHistorial();
    }

    private void inicializarVistas() {
        campoPlaca = findViewById(R.id.campoPlaca);
        tarjetaResultado = findViewById(R.id.tarjetaResultado);
        textoResultado = findViewById(R.id.textoResultado);
        textoPrecio = findViewById(R.id.textoPrecio);
        textoTipoVehiculo = findViewById(R.id.textoTipoVehiculo);
        textoMotivo = findViewById(R.id.textoMotivo);
        listaValidaciones = findViewById(R.id.listaValidaciones);
        textoSinValidaciones = findViewById(R.id.textoSinValidaciones);

        listaValidaciones.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new AdaptadorValidaciones(puedeEliminar, val -> confirmarEliminacionValidacion(val));
        listaValidaciones.setAdapter(adaptador);

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
    }

    private void configurarEventos() {
        MaterialButton botonValidar = findViewById(R.id.botonValidar);
        botonValidar.setOnClickListener(v -> validarPlaca());
    }

    private void validarPlaca() {
        String placa = campoPlaca.getText() != null ? campoPlaca.getText().toString().trim().toUpperCase() : "";

        if (placa.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_placa_vacia), Toast.LENGTH_SHORT).show();
            return;
        }

        // HU-05: Mostrar indicador de carga mientras consulta API de tránsito
        Toast.makeText(this, "Consultando API de Tránsito...", Toast.LENGTH_SHORT).show();

        GestorSubsidios.ResultadoValidacion resultado = gestorSubsidios.validarSubsidio(placa);

        tarjetaResultado.setVisibility(View.VISIBLE);

        if (resultado.isAplicaSubsidio()) {
            textoResultado.setText(getString(R.string.resultado_aplica));
            textoResultado.setTextColor(getColor(R.color.exito));
            textoMotivo.setVisibility(View.GONE);
            dispararNotificacionSubsidio(placa, resultado);
        } else {
            textoResultado.setText(getString(R.string.resultado_no_aplica));
            textoResultado.setTextColor(getColor(R.color.error));
            if (resultado.getMotivoRechazo() != null && !resultado.getMotivoRechazo().isEmpty()) {
                textoMotivo.setText(getString(R.string.motivo_rechazo, resultado.getMotivoRechazo()));
                textoMotivo.setVisibility(View.VISIBLE);
            }
        }

        textoPrecio.setText(getString(R.string.precio_aplicado,
                String.format(Locale.getDefault(), "%,.0f", resultado.getPrecioAplicado())));
        textoTipoVehiculo.setText("Tipo: " + resultado.getTipoVehiculo() +
                " | Propietario: " + resultado.getPropietario());

        // HU-05: Mostrar fuente de datos (API de tránsito)
        TextView textoFuente = findViewById(R.id.textoFuenteDatos);
        if (textoFuente != null) {
            textoFuente.setText("Fuente: " + resultado.getFuenteDatos());
            textoFuente.setVisibility(View.VISIBLE);
        }

        campoPlaca.setText(""); // Limpiar para el siguiente
        cargarHistorial();
    }

    /**
     * HU-18: al aprobarse un subsidio, encola una notificación diferida (started
     * service) avisando al cliente que el subsidio quedó habilitado para esa placa.
     */
    private void dispararNotificacionSubsidio(String placa,
                                              GestorSubsidios.ResultadoValidacion resultado) {
        String mensaje = String.format(Locale.getDefault(),
                "El vehículo %s aplica al subsidio. Precio aplicado: $%,.0f. Tipo: %s.",
                placa, resultado.getPrecioAplicado(), resultado.getTipoVehiculo());

        Intent svc = new Intent(this, NotificadorEventosServicio.class);
        svc.putExtra(NotificadorEventosServicio.EXTRA_TITULO,
                "💵 Subsidio aprobado para placa " + placa);
        svc.putExtra(NotificadorEventosServicio.EXTRA_MENSAJE, mensaje);
        startService(svc);
    }

    private void confirmarEliminacionValidacion(ValidacionSubsidio val) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar registro")
                .setMessage("¿Eliminar el registro de subsidio para placa \"" + val.getPlaca() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    subsidioDao.eliminarValidacion(val);
                    Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                    cargarHistorial();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarHistorial() {
        List<ValidacionSubsidio> validaciones = gestorSubsidios.obtenerHistorialValidaciones();
        if (validaciones.isEmpty()) {
            textoSinValidaciones.setVisibility(View.VISIBLE);
            listaValidaciones.setVisibility(View.GONE);
        } else {
            textoSinValidaciones.setVisibility(View.GONE);
            listaValidaciones.setVisibility(View.VISIBLE);
            adaptador.actualizarDatos(validaciones);
        }
    }

    private static class AdaptadorValidaciones extends RecyclerView.Adapter<AdaptadorValidaciones.VistaValidacion> {
        interface OnEliminar { void eliminar(ValidacionSubsidio val); }

        private List<ValidacionSubsidio> validaciones = new ArrayList<>();
        private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        private final OnEliminar onEliminar;
        private final boolean puedeEliminar;

        AdaptadorValidaciones(boolean puedeEliminar, OnEliminar onEliminar) {
            this.puedeEliminar = puedeEliminar;
            this.onEliminar = onEliminar;
        }

        public void actualizarDatos(List<ValidacionSubsidio> nuevas) {
            this.validaciones = nuevas;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VistaValidacion onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_validacion, parent, false);
            return new VistaValidacion(vista);
        }

        @Override
        public void onBindViewHolder(@NonNull VistaValidacion holder, int position) {
            ValidacionSubsidio val = validaciones.get(position);
            holder.textoPlaca.setText("🚗 Placa: " + val.getPlaca());
            holder.textoFecha.setText(formatoFecha.format(new Date(val.getFechaValidacion())));
            
            boolean aprobado = "APROBADO".equals(val.getResultado());
            holder.indicadorEstado.setText(aprobado ? "✅" : "❌");
            holder.textoResultado.setText("Estado: " + val.getResultado());
            holder.textoResultado.setTextColor(holder.itemView.getContext().getColor(aprobado ? R.color.exito : R.color.error));
            
            String detalle = String.format(Locale.getDefault(), "Precio: $%,.0f | Tipo: %s", val.getPrecioAplicado(), val.getTipoVehiculo());
            if (val.getMotivoRechazo() != null && !val.getMotivoRechazo().isEmpty()) {
                detalle += "\nMotivo: " + val.getMotivoRechazo();
            }
            holder.textoPrecio.setText(detalle);
            if (puedeEliminar) {
                holder.botonEliminar.setVisibility(View.VISIBLE);
                holder.botonEliminar.setOnClickListener(v -> onEliminar.eliminar(val));
            } else {
                holder.botonEliminar.setVisibility(View.GONE);
                holder.botonEliminar.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return validaciones.size(); }

        static class VistaValidacion extends RecyclerView.ViewHolder {
            TextView indicadorEstado, textoPlaca, textoResultado, textoPrecio, textoFecha;
            MaterialButton botonEliminar;
            VistaValidacion(@NonNull View itemView) {
                super(itemView);
                indicadorEstado = itemView.findViewById(R.id.indicadorEstado);
                textoPlaca = itemView.findViewById(R.id.textoPlaca);
                textoResultado = itemView.findViewById(R.id.textoResultado);
                textoPrecio = itemView.findViewById(R.id.textoPrecio);
                textoFecha = itemView.findViewById(R.id.textoFecha);
                botonEliminar = itemView.findViewById(R.id.botonEliminarValidacion);
            }
        }
    }
}
