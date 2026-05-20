package com.estaciones.qestaciones.hu13_inventarios;

import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para control de inventarios con historial auditable.
 */
public class ControlInventarioActividad extends AppCompatActivity {

    private GestorInventarios gestorInventarios;
    private Spinner spinnerTipoMovimiento;
    private Spinner spinnerCombustibleInv;
    private TextInputEditText campoVolumenInv;
    private TextInputEditText campoMotivo;
    private TextView textoSaldoActual;
    private RecyclerView listaMovimientos;
    private MovimientosAdaptador adaptador;
    private TextView textoSinMovimientos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_control_inventario);

        gestorInventarios = new GestorInventarios(this);

        // HU-13: Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        inicializarVistas();
        configurarEventos();
        actualizarSaldo();
        cargarMovimientos();
    }

    private void inicializarVistas() {
        spinnerTipoMovimiento = findViewById(R.id.spinnerTipoMovimiento);
        spinnerCombustibleInv = findViewById(R.id.spinnerCombustibleInv);
        campoVolumenInv = findViewById(R.id.campoVolumenInv);
        campoMotivo = findViewById(R.id.campoMotivo);
        textoSaldoActual = findViewById(R.id.textoSaldoActual);
        listaMovimientos = findViewById(R.id.listaMovimientos);
        textoSinMovimientos = findViewById(R.id.textoSinMovimientos);

        // Spinner tipo movimiento
        String[] tiposMovimiento = {"ENTRADA", "SALIDA", "AJUSTE"};
        ArrayAdapter<String> adapterMov = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tiposMovimiento);
        spinnerTipoMovimiento.setAdapter(adapterMov);

        // Spinner combustible
        String[] tiposCombustible = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};
        ArrayAdapter<String> adapterComb = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tiposCombustible);
        spinnerCombustibleInv.setAdapter(adapterComb);

        listaMovimientos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MovimientosAdaptador();
        listaMovimientos.setAdapter(adaptador);
    }

    private void configurarEventos() {
        MaterialButton botonRegistrar = findViewById(R.id.botonRegistrarMovimiento);
        botonRegistrar.setOnClickListener(v -> registrarMovimiento());
        findViewById(R.id.botonVolverInventario).setOnClickListener(v -> finish());
    }

    private void registrarMovimiento() {
        String tipo = spinnerTipoMovimiento.getSelectedItem().toString();
        String combustible = spinnerCombustibleInv.getSelectedItem().toString();
        String volumen = campoVolumenInv.getText() != null ? campoVolumenInv.getText().toString() : "";
        String motivo = campoMotivo.getText() != null ? campoMotivo.getText().toString() : "";

        GestorInventarios.ResultadoInventario resultado =
                gestorInventarios.registrarMovimiento(tipo, combustible, volumen, motivo, "estacion");

        if (resultado.isExitoso()) {
            Toast.makeText(this, resultado.getMensaje(), Toast.LENGTH_SHORT).show();
            limpiarFormulario();
            actualizarSaldo();
            cargarMovimientos(); // Refrescar historial
        } else {
            Toast.makeText(this, resultado.getMensaje(), Toast.LENGTH_LONG).show();
        }
    }

    private void limpiarFormulario() {
        campoVolumenInv.setText("");
        campoMotivo.setText("");
        spinnerTipoMovimiento.setSelection(0);
        campoVolumenInv.clearFocus();
        campoMotivo.clearFocus();
    }

    private void actualizarSaldo() {
        String combustible = spinnerCombustibleInv.getSelectedItem() != null ?
                spinnerCombustibleInv.getSelectedItem().toString() : "ACPM";
        double saldo = gestorInventarios.obtenerSaldoActual(combustible);
        textoSaldoActual.setText(String.format(Locale.getDefault(), "⛽ Saldo actual: %.0f litros", saldo));
    }

    private void cargarMovimientos() {
        List<MovimientoInventario> movimientos = gestorInventarios.obtenerTodosLosMovimientos();
        if (movimientos.isEmpty()) {
            textoSinMovimientos.setVisibility(View.VISIBLE);
            listaMovimientos.setVisibility(View.GONE);
        } else {
            textoSinMovimientos.setVisibility(View.GONE);
            listaMovimientos.setVisibility(View.VISIBLE);
            adaptador.actualizarDatos(movimientos);
        }
    }

    private static class MovimientosAdaptador extends RecyclerView.Adapter<MovimientosAdaptador.MovimientoViewHolder> {
        private List<MovimientoInventario> movimientos = new ArrayList<>();
        private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public void actualizarDatos(List<MovimientoInventario> nuevosMovimientos) {
            this.movimientos = nuevosMovimientos;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MovimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movimiento, parent, false);
            return new MovimientoViewHolder(vista);
        }

        @Override
        public void onBindViewHolder(@NonNull MovimientoViewHolder holder, int position) {
            MovimientoInventario mov = movimientos.get(position);
            String icono = mov.getTipoMovimiento().equals("ENTRADA") ? "📥" : 
                          (mov.getTipoMovimiento().equals("SALIDA") ? "📤" : "🔄");
            
            holder.textoTipoMovimiento.setText(icono + " " + mov.getTipoMovimiento());
            holder.textoVolumenMov.setText(String.format(Locale.getDefault(), "%.0f L — %s", mov.getVolumenLitros(), mov.getTipoCombustible()));
            holder.textoSaldos.setText(String.format(Locale.getDefault(), "Saldo: %.0f → %.0f L", mov.getSaldoAnterior(), mov.getSaldoNuevo()));
            holder.textoMotivoMov.setText("📝 " + mov.getMotivo());
            holder.textoFechaMov.setText(formatoFecha.format(new Date(mov.getFechaMovimiento())));
        }

        @Override
        public int getItemCount() { return movimientos.size(); }

        static class MovimientoViewHolder extends RecyclerView.ViewHolder {
            TextView textoTipoMovimiento, textoVolumenMov, textoSaldos, textoMotivoMov, textoFechaMov;
            MovimientoViewHolder(@NonNull View itemView) {
                super(itemView);
                textoTipoMovimiento = itemView.findViewById(R.id.textoTipoMovimiento);
                textoVolumenMov = itemView.findViewById(R.id.textoVolumenMov);
                textoSaldos = itemView.findViewById(R.id.textoSaldos);
                textoMotivoMov = itemView.findViewById(R.id.textoMotivoMov);
                textoFechaMov = itemView.findViewById(R.id.textoFechaMov);
            }
        }
    }
}
