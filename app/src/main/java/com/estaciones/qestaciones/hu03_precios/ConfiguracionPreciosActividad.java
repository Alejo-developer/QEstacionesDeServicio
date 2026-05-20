package com.estaciones.qestaciones.hu03_precios;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.ReglaPreciosDao;
import com.estaciones.qestaciones.modelo.ReglaPrecios;
import com.estaciones.qestaciones.modelo.TipoVehiculo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para configurar reglas de precios ACPM.
 */
public class ConfiguracionPreciosActividad extends AppCompatActivity {

    private TextInputEditText campoZona;
    private Spinner selectorTipoVehiculo;
    private TextInputEditText campoPrecioBase;
    private TextInputEditText campoPrecioSubsidiado;
    private TextInputEditText campoDecreto;
    private TextView textoError;
    private RecyclerView listaReglas;
    private TextView textoSinReglas;
    private AdaptadorReglas adaptador;

    private GestorPrecios gestorPrecios;
    private ReglaPreciosDao reglaPreciosDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_configuracion_precios);

        gestorPrecios = new GestorPrecios(this);
        reglaPreciosDao = BaseDatosEstaciones.obtenerInstancia(this).reglaPreciosDao();

        inicializarVistas();
        configurarSelectorVehiculo();
        configurarEventos();
        cargarReglas();
    }

    private void inicializarVistas() {
        campoZona = findViewById(R.id.campoZona);
        selectorTipoVehiculo = findViewById(R.id.selectorTipoVehiculo);
        campoPrecioBase = findViewById(R.id.campoPrecioBase);
        campoPrecioSubsidiado = findViewById(R.id.campoPrecioSubsidiado);
        campoDecreto = findViewById(R.id.campoDecreto);
        textoError = findViewById(R.id.textoError);
        listaReglas = findViewById(R.id.listaReglas);
        textoSinReglas = findViewById(R.id.textoSinReglas);

        listaReglas.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new AdaptadorReglas(regla -> confirmarEliminacionRegla(regla));
        listaReglas.setAdapter(adaptador);

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
    }

    private void configurarSelectorVehiculo() {
        String[] tipos = {
            TipoVehiculo.PARTICULAR.getDescripcion(),
            TipoVehiculo.CARGA.getDescripcion(),
            TipoVehiculo.SERVICIO_PUBLICO.getDescripcion(),
            TipoVehiculo.MOTOCICLETA.getDescripcion()
        };
        ArrayAdapter<String> adaptadorSp = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipos);
        adaptadorSp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectorTipoVehiculo.setAdapter(adaptadorSp);
    }

    private void configurarEventos() {
        MaterialButton botonGuardar = findViewById(R.id.botonGuardarRegla);
        botonGuardar.setOnClickListener(v -> guardarRegla());
    }

    private void guardarRegla() {
        String zona = obtenerTexto(campoZona);
        String precioBaseStr = obtenerTexto(campoPrecioBase);
        String precioSubStr = obtenerTexto(campoPrecioSubsidiado);
        String decreto = obtenerTexto(campoDecreto);

        if (zona.isEmpty() || precioBaseStr.isEmpty() || decreto.isEmpty()) {
            mostrarError(getString(R.string.error_campos_vacios));
            return;
        }

        try {
            double precioBase = Double.parseDouble(precioBaseStr);
            double precioSubsidiado = precioSubStr.isEmpty() ? precioBase : Double.parseDouble(precioSubStr);
            TipoVehiculo[] tipos = TipoVehiculo.values();
            String tipoVehiculo = tipos[selectorTipoVehiculo.getSelectedItemPosition()].name();

            gestorPrecios.crearRegla(zona, tipoVehiculo, precioBase, precioSubsidiado, decreto);
            
            Toast.makeText(this, getString(R.string.exito_regla_guardada), Toast.LENGTH_SHORT).show();
            limpiarFormulario();
            cargarReglas(); // Forzar recarga del historial
        } catch (NumberFormatException e) {
            mostrarError(getString(R.string.error_precio_invalido));
        }
    }

    private void cargarReglas() {
        List<ReglaPrecios> reglas = gestorPrecios.obtenerReglasActivas();
        if (reglas.isEmpty()) {
            textoSinReglas.setVisibility(View.VISIBLE);
            listaReglas.setVisibility(View.GONE);
        } else {
            textoSinReglas.setVisibility(View.GONE);
            listaReglas.setVisibility(View.VISIBLE);
            adaptador.actualizarDatos(reglas);
        }
    }

    private void limpiarFormulario() {
        campoZona.setText("");
        campoPrecioBase.setText("");
        campoPrecioSubsidiado.setText("");
        campoDecreto.setText("");
        selectorTipoVehiculo.setSelection(0);
        textoError.setVisibility(View.GONE);
        campoZona.clearFocus();
    }

    private String obtenerTexto(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }

    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }

    private void confirmarEliminacionRegla(ReglaPrecios regla) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar decreto")
                .setMessage("¿Eliminar la regla \"" + regla.getDecreto() + "\" (Zona: " + regla.getZona() + ")?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    reglaPreciosDao.eliminarRegla(regla);
                    Toast.makeText(this, "Decreto eliminado", Toast.LENGTH_SHORT).show();
                    cargarReglas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static class AdaptadorReglas extends RecyclerView.Adapter<AdaptadorReglas.VistaRegla> {
        interface OnEliminar { void eliminar(ReglaPrecios regla); }

        private List<ReglaPrecios> reglas = new ArrayList<>();
        private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        private final OnEliminar onEliminar;

        AdaptadorReglas(OnEliminar onEliminar) { this.onEliminar = onEliminar; }

        public void actualizarDatos(List<ReglaPrecios> nuevasReglas) {
            this.reglas = nuevasReglas;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VistaRegla onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_regla_precios, parent, false);
            return new VistaRegla(vista);
        }

        @Override
        public void onBindViewHolder(@NonNull VistaRegla holder, int position) {
            ReglaPrecios regla = reglas.get(position);
            holder.textoZona.setText("Zona: " + regla.getZona());
            holder.textoTipoVehiculo.setText(regla.getTipoVehiculo());
            holder.textoPrecioBase.setText("Base: $" + String.format(Locale.getDefault(), "%,.0f", regla.getPrecioBase()));
            holder.textoPrecioSubsidiado.setText("Sub.: $" + String.format(Locale.getDefault(), "%,.0f", regla.getPrecioSubsidiado()));
            holder.textoDecreto.setText("Norma: " + regla.getDecreto());
            holder.textoFecha.setText(formatoFecha.format(new Date(regla.getFechaCreacion())));
            holder.botonEliminar.setOnClickListener(v -> onEliminar.eliminar(regla));
        }

        @Override
        public int getItemCount() { return reglas.size(); }

        static class VistaRegla extends RecyclerView.ViewHolder {
            TextView textoZona, textoTipoVehiculo, textoPrecioBase, textoPrecioSubsidiado, textoDecreto, textoFecha;
            MaterialButton botonEliminar;
            VistaRegla(@NonNull View itemView) {
                super(itemView);
                textoZona = itemView.findViewById(R.id.textoZona);
                textoTipoVehiculo = itemView.findViewById(R.id.textoTipoVehiculo);
                textoPrecioBase = itemView.findViewById(R.id.textoPrecioBase);
                textoPrecioSubsidiado = itemView.findViewById(R.id.textoPrecioSubsidiado);
                textoDecreto = itemView.findViewById(R.id.textoDecreto);
                textoFecha = itemView.findViewById(R.id.textoFecha);
                botonEliminar = itemView.findViewById(R.id.botonEliminarRegla);
            }
        }
    }
}
