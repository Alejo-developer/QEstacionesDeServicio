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
import com.estaciones.qestaciones.datos.PrecioReferenciaDao;
import com.estaciones.qestaciones.modelo.PrecioReferencia;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HU-Precios: El REGULADOR fija el precio de referencia nacional por tipo de combustible.
 * Las estaciones lo usan como base para calcular su precio de venta.
 */
public class PrecioReferenciaActividad extends AppCompatActivity {

    private static final String[] COMBUSTIBLES = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};
    private static final NumberFormat FMT_MONEDA = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private PrecioReferenciaDao dao;
    private Spinner spinnerCombustible;
    private TextInputEditText campoPrecio;
    private TextInputEditText campoDecreto;
    private RecyclerView lista;
    private PreciosAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_precio_referencia);

        dao = BaseDatosEstaciones.obtenerInstancia(this).precioReferenciaDao();

        spinnerCombustible = findViewById(R.id.spinnerCombustibleRef);
        campoPrecio = findViewById(R.id.campoPrecioRef);
        campoDecreto = findViewById(R.id.campoDecretoRef);
        lista = findViewById(R.id.listaPrecios);

        spinnerCombustible.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COMBUSTIBLES));

        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new PreciosAdaptador(precio -> confirmarEliminacionPrecio(precio));
        lista.setAdapter(adaptador);

        findViewById(R.id.botonGuardarRef).setOnClickListener(v -> guardarPrecio());
        findViewById(R.id.botonVolverRef).setOnClickListener(v -> finish());

        cargarHistorial();
    }

    private void guardarPrecio() {
        String tipo = spinnerCombustible.getSelectedItem().toString();
        String precioStr = campoPrecio.getText() != null ? campoPrecio.getText().toString().trim() : "";
        String decreto = campoDecreto.getText() != null ? campoDecreto.getText().toString().trim() : "";

        if (precioStr.isEmpty()) {
            Toast.makeText(this, "Ingrese el precio de referencia", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio <= 0) {
                Toast.makeText(this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        PrecioReferencia ref = new PrecioReferencia();
        ref.setTipoCombustible(tipo);
        ref.setPrecioPorLitro(precio);
        ref.setDecreto(decreto.isEmpty() ? "Sin decreto" : decreto);
        ref.setFijadoPor("regulador");
        dao.insertar(ref);

        Toast.makeText(this, "Precio de referencia publicado: $"
                + FMT_MONEDA.format(precio) + "/L para " + tipo, Toast.LENGTH_SHORT).show();

        campoPrecio.setText("");
        campoDecreto.setText("");
        cargarHistorial();
    }

    private void cargarHistorial() {
        adaptador.actualizarDatos(dao.obtenerHistorial());
    }

    private void confirmarEliminacionPrecio(PrecioReferencia precio) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar precio de referencia")
                .setMessage("¿Eliminar el precio de " + precio.getTipoCombustible()
                        + " ($" + FMT_MONEDA.format(precio.getPrecioPorLitro()) + "/L)?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    dao.eliminarPrecio(precio);
                    Toast.makeText(this, "Precio eliminado", Toast.LENGTH_SHORT).show();
                    cargarHistorial();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─── Adaptador ────────────────────────────────────────────────────────────

    private static class PreciosAdaptador extends RecyclerView.Adapter<PreciosAdaptador.VH> {
        interface OnEliminar { void eliminar(PrecioReferencia precio); }

        private List<PrecioReferencia> items = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        private final NumberFormat fmtNum = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        private final OnEliminar onEliminar;

        PreciosAdaptador(OnEliminar onEliminar) { this.onEliminar = onEliminar; }

        void actualizarDatos(List<PrecioReferencia> nuevos) {
            this.items = nuevos;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_precio_referencia, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            PrecioReferencia p = items.get(position);
            h.combustible.setText("⛽ " + p.getTipoCombustible());
            h.precio.setText("$" + fmtNum.format(p.getPrecioPorLitro()) + "/L");
            h.decreto.setText("📋 " + p.getDecreto());
            h.fecha.setText(fmt.format(new Date(p.getFechaCreacion())));
            h.botonEliminar.setOnClickListener(v -> onEliminar.eliminar(p));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView combustible, precio, decreto, fecha;
            MaterialButton botonEliminar;
            VH(View v) {
                super(v);
                combustible = v.findViewById(R.id.textoCombustibleRef);
                precio = v.findViewById(R.id.textoPrecioRef);
                decreto = v.findViewById(R.id.textoDecretoRef);
                fecha = v.findViewById(R.id.textoFechaRef);
                botonEliminar = v.findViewById(R.id.botonEliminarPrecio);
            }
        }
    }
}
