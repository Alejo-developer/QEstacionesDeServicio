package com.estaciones.qestaciones.hu15_saldo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla "Pagar a empleado": la ESTACION logueada lista a sus OPERADORES asignados
 * (rol=OPERADOR, empresaAsignada=su nombreUsuario) y puede transferirles saldo.
 *
 * La transferencia se hace con BaseDatosEstaciones.transferirSaldo, que es atómica:
 * descuenta del saldo de la estación + suma al operador + registra ambos movimientos.
 * Si la estación no tiene fondos, no pasa nada.
 */
public class PagarEmpleadoActividad extends AppCompatActivity {

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private BaseDatosEstaciones db;
    private UsuarioDao usuarioDao;
    private String empresaUsuario = "";

    private TextView textoSaldoEstacion, textoEstacionPagador, textoSinOperadores;
    private RecyclerView lista;
    private OperadoresAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_pagar_empleado);

        db = BaseDatosEstaciones.obtenerInstancia(this);
        usuarioDao = db.usuarioDao();

        empresaUsuario = nvl(getIntent().getStringExtra("nombreUsuario"));
        if (empresaUsuario.isEmpty()) {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textoSaldoEstacion = findViewById(R.id.textoSaldoEstacion);
        textoEstacionPagador = findViewById(R.id.textoEstacionPagador);
        textoSinOperadores = findViewById(R.id.textoSinOperadores);
        lista = findViewById(R.id.listaOperadores);

        Usuario empresa = usuarioDao.buscarPorNombreUsuario(empresaUsuario);
        if (empresa != null) {
            String etiqueta = "@" + empresaUsuario;
            if (empresa.getNombreCompleto() != null && !empresa.getNombreCompleto().isEmpty()) {
                etiqueta = empresa.getNombreCompleto() + " · " + etiqueta;
            }
            textoEstacionPagador.setText(etiqueta);
        }

        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new OperadoresAdaptador(this::pedirMonto);
        lista.setAdapter(adaptador);

        findViewById(R.id.botonVolverPago).setOnClickListener(v -> finish());

        recargarVista();
    }

    @Override
    protected void onResume() {
        super.onResume();
        recargarVista();
    }

    private void recargarVista() {
        double saldo = usuarioDao.obtenerSaldo(empresaUsuario);
        textoSaldoEstacion.setText("$" + FMT.format(saldo));

        List<Usuario> ops = usuarioDao.obtenerOperadoresPorEmpresa(empresaUsuario);
        if (ops.isEmpty()) {
            textoSinOperadores.setVisibility(View.VISIBLE);
            lista.setVisibility(View.GONE);
        } else {
            textoSinOperadores.setVisibility(View.GONE);
            lista.setVisibility(View.VISIBLE);
            adaptador.actualizar(ops);
        }
    }

    private void pedirMonto(Usuario operador) {
        // Diálogo simple: monto + concepto
        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(pad, pad / 2, pad, 0);

        TextInputLayout layMonto = new TextInputLayout(this);
        layMonto.setHint("Monto a pagar");
        TextInputEditText campoMonto = new TextInputEditText(this);
        campoMonto.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layMonto.addView(campoMonto);

        TextInputLayout layConcepto = new TextInputLayout(this);
        layConcepto.setHint("Concepto (ej. Sueldo abril, comisión)");
        TextInputEditText campoConcepto = new TextInputEditText(this);
        layConcepto.addView(campoConcepto);

        contenedor.addView(layMonto);
        LinearLayout.LayoutParams sep = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sep.topMargin = pad;
        contenedor.addView(layConcepto, sep);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Pagar a " + (operador.getNombreCompleto() != null
                        ? operador.getNombreCompleto() : "@" + operador.getNombreUsuario()))
                .setView(contenedor)
                .setPositiveButton("Pagar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {
                    String montoStr = campoMonto.getText() != null
                            ? campoMonto.getText().toString().trim() : "";
                    String concepto = campoConcepto.getText() != null
                            ? campoConcepto.getText().toString().trim() : "";
                    if (montoStr.isEmpty()) {
                        Toast.makeText(this, "Ingresa el monto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double monto;
                    try {
                        monto = Double.parseDouble(montoStr);
                        if (monto <= 0) {
                            Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (concepto.isEmpty()) concepto = "Pago a empleado";

                    boolean ok = db.transferirSaldo(empresaUsuario, operador.getNombreUsuario(),
                            monto, concepto, "TRANSFERENCIA_INTERNA");

                    if (ok) {
                        Toast.makeText(this, "✅ Pago de $" + FMT.format(monto)
                                        + " a @" + operador.getNombreUsuario(),
                                Toast.LENGTH_SHORT).show();
                        dlg.dismiss();
                        recargarVista();
                    } else {
                        Toast.makeText(this,
                                "Saldo insuficiente. Tu saldo es $"
                                        + FMT.format(usuarioDao.obtenerSaldo(empresaUsuario)),
                                Toast.LENGTH_LONG).show();
                    }
                }));
        dlg.show();
    }

    // ─── Adaptador ─────────────────────────────────────────────────────────

    private interface OnPagar { void pagar(Usuario operador); }

    private static String nvl(String s) { return s == null ? "" : s; }

    private static class OperadoresAdaptador extends RecyclerView.Adapter<OperadoresAdaptador.VH> {
        private List<Usuario> items = new ArrayList<>();
        private final OnPagar listener;

        OperadoresAdaptador(OnPagar listener) { this.listener = listener; }

        void actualizar(List<Usuario> nuevos) { items = nuevos; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_operador_pago, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Usuario u = items.get(position);
            h.nombre.setText(u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getNombreUsuario());
            h.usuario.setText("@" + u.getNombreUsuario());
            h.saldo.setText("Saldo actual: $" + FMT.format(u.getSaldoCop()));
            h.boton.setOnClickListener(v -> listener.pagar(u));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView nombre, usuario, saldo;
            com.google.android.material.button.MaterialButton boton;
            VH(View v) {
                super(v);
                nombre = v.findViewById(R.id.textoNombreOperador);
                usuario = v.findViewById(R.id.textoUsuarioOperador);
                saldo = v.findViewById(R.id.textoSaldoOperador);
                boton = v.findViewById(R.id.botonPagarOperador);
            }
        }
    }
}
