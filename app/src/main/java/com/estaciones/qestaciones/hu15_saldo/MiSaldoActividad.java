package com.estaciones.qestaciones.hu15_saldo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.estaciones.qestaciones.datos.MovimientoSaldoDao;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.MovimientoSaldo;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * "Mi Saldo": muestra el saldo actual del usuario logueado, permite recargar
 * (simulación con tarjeta o PayPal) y lista el historial de movimientos.
 *
 * NO se persiste el número completo de tarjeta ni el CVV — solo el método
 * y los últimos 4 dígitos para identificar la transacción en el historial.
 */
public class MiSaldoActividad extends AppCompatActivity {

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("es", "CO"));
    private static final String[] METODOS = {"TARJETA_CREDITO", "TARJETA_DEBITO", "PAYPAL"};
    private static final String[] METODOS_LBL = {"Tarjeta de Crédito", "Tarjeta de Débito", "PayPal"};

    private BaseDatosEstaciones db;
    private UsuarioDao usuarioDao;
    private MovimientoSaldoDao movDao;

    private String nombreUsuario = "";
    private String nombreCompleto = "";

    private TextView textoSaldoGrande, textoUsuarioSaldo, textoSinMovimientos;
    private RecyclerView listaMovimientos;
    private MovimientosAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_mi_saldo);

        db = BaseDatosEstaciones.obtenerInstancia(this);
        usuarioDao = db.usuarioDao();
        movDao = db.movimientoSaldoDao();

        nombreUsuario = nvl(getIntent().getStringExtra("nombreUsuarioOperador"));
        if (nombreUsuario.isEmpty()) {
            // Fallback: si no viene el del operador, uso el scope.
            nombreUsuario = nvl(getIntent().getStringExtra("nombreUsuario"));
        }
        nombreCompleto = nvl(getIntent().getStringExtra("nombreCompleto"));

        if (nombreUsuario.isEmpty()) {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textoSaldoGrande = findViewById(R.id.textoSaldoGrande);
        textoUsuarioSaldo = findViewById(R.id.textoUsuarioSaldo);
        textoSinMovimientos = findViewById(R.id.textoSinMovimientosSaldo);
        listaMovimientos = findViewById(R.id.listaMovimientosSaldo);

        listaMovimientos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MovimientosAdaptador();
        listaMovimientos.setAdapter(adaptador);

        textoUsuarioSaldo.setText("@" + nombreUsuario
                + (nombreCompleto.isEmpty() ? "" : " · " + nombreCompleto));

        findViewById(R.id.botonRecargar).setOnClickListener(v -> mostrarDialogoRecarga());
        findViewById(R.id.botonVolverSaldo).setOnClickListener(v -> finish());

        recargarVista();
    }

    @Override
    protected void onResume() {
        super.onResume();
        recargarVista();
    }

    private void recargarVista() {
        double saldo = usuarioDao.obtenerSaldo(nombreUsuario);
        textoSaldoGrande.setText("$" + FMT.format(saldo));

        List<MovimientoSaldo> movs = movDao.obtenerPorUsuario(nombreUsuario);
        if (movs.isEmpty()) {
            textoSinMovimientos.setVisibility(View.VISIBLE);
            listaMovimientos.setVisibility(View.GONE);
        } else {
            textoSinMovimientos.setVisibility(View.GONE);
            listaMovimientos.setVisibility(View.VISIBLE);
            adaptador.actualizar(movs);
        }
    }

    // ─── Diálogo de recarga ────────────────────────────────────────────────

    private void mostrarDialogoRecarga() {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialogo_recargar_saldo, null);

        TextInputEditText campoMonto = vista.findViewById(R.id.campoMontoRecarga);
        Spinner spinnerMetodo = vista.findViewById(R.id.spinnerMetodoPago);
        View panelTarjeta = vista.findViewById(R.id.panelTarjeta);
        View panelPayPal = vista.findViewById(R.id.panelPayPal);
        TextInputEditText campoNumero = vista.findViewById(R.id.campoNumeroTarjeta);
        TextInputEditText campoTitular = vista.findViewById(R.id.campoTitularTarjeta);
        TextInputEditText campoVenc = vista.findViewById(R.id.campoVencimiento);
        TextInputEditText campoCvv = vista.findViewById(R.id.campoCvv);
        TextInputEditText campoPayPal = vista.findViewById(R.id.campoCorreoPayPal);

        spinnerMetodo.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, METODOS_LBL));
        spinnerMetodo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean esPayPal = position == 2;
                panelTarjeta.setVisibility(esPayPal ? View.GONE : View.VISIBLE);
                panelPayPal.setVisibility(esPayPal ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("💳  Recargar saldo")
                .setView(vista)
                .setPositiveButton("Recargar", null) // se sobreescribe abajo
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {
                    int posicion = spinnerMetodo.getSelectedItemPosition();
                    String metodo = METODOS[posicion];
                    Double monto = parsearMonto(campoMonto);
                    if (monto == null) return;

                    String ultimos4 = null;
                    if (!"PAYPAL".equals(metodo)) {
                        if (!validarCamposTarjeta(campoNumero, campoTitular, campoVenc, campoCvv)) return;
                        String num = campoNumero.getText().toString().trim();
                        ultimos4 = num.substring(num.length() - 4);
                    } else {
                        if (!validarPayPal(campoPayPal)) return;
                    }

                    db.recargarSaldo(nombreUsuario, monto, metodo, ultimos4);
                    Toast.makeText(MiSaldoActividad.this,
                            "✅ Recarga simulada de $" + FMT.format(monto),
                            Toast.LENGTH_SHORT).show();
                    dialogo.dismiss();
                    recargarVista();
                }));

        dialogo.show();
    }

    private Double parsearMonto(TextInputEditText campo) {
        String texto = campo.getText() != null ? campo.getText().toString().trim() : "";
        if (texto.isEmpty()) {
            Toast.makeText(this, "Ingresa el monto", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            double m = Double.parseDouble(texto);
            if (m <= 0) {
                Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return null;
            }
            return m;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private boolean validarCamposTarjeta(TextInputEditText num, TextInputEditText titular,
                                          TextInputEditText venc, TextInputEditText cvv) {
        String numero = num.getText() != null ? num.getText().toString().trim() : "";
        String titularStr = titular.getText() != null ? titular.getText().toString().trim() : "";
        String vencStr = venc.getText() != null ? venc.getText().toString().trim() : "";
        String cvvStr = cvv.getText() != null ? cvv.getText().toString().trim() : "";

        if (!numero.matches("\\d{16}")) {
            Toast.makeText(this, "Número de tarjeta debe tener 16 dígitos", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (titularStr.isEmpty()) {
            Toast.makeText(this, "Ingresa el titular de la tarjeta", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!vencStr.matches("\\d{2}/\\d{2}")) {
            Toast.makeText(this, "Vencimiento debe ser MM/AA", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            int mes = Integer.parseInt(vencStr.substring(0, 2));
            int anio = 2000 + Integer.parseInt(vencStr.substring(3, 5));
            if (mes < 1 || mes > 12) throw new NumberFormatException();
            // El mes/año debe ser igual o mayor al actual
            java.util.Calendar hoy = java.util.Calendar.getInstance();
            int mesHoy = hoy.get(java.util.Calendar.MONTH) + 1;
            int anioHoy = hoy.get(java.util.Calendar.YEAR);
            if (anio < anioHoy || (anio == anioHoy && mes < mesHoy)) {
                Toast.makeText(this, "La tarjeta está vencida", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Fecha de vencimiento inválida", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!cvvStr.matches("\\d{3}")) {
            Toast.makeText(this, "El CVV debe tener 3 dígitos", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validarPayPal(TextInputEditText campo) {
        String correo = campo.getText() != null ? campo.getText().toString().trim() : "";
        if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Ingresa un correo válido de PayPal", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ─── Adaptador ─────────────────────────────────────────────────────────

    private static String nvl(String s) { return s == null ? "" : s; }

    private static class MovimientosAdaptador extends RecyclerView.Adapter<MovimientosAdaptador.VH> {
        private List<MovimientoSaldo> items = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        void actualizar(List<MovimientoSaldo> nuevos) { items = nuevos; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movimiento_saldo, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MovimientoSaldo m = items.get(position);
            String tipo = m.getTipo() != null ? m.getTipo() : "";

            String icono;
            int color;
            String signo;
            switch (tipo) {
                case "RECARGA":  icono = "💳"; color = R.color.informacion; signo = "+"; break;
                case "INGRESO":  icono = "📥"; color = R.color.exito;       signo = "+"; break;
                case "PAGO":     icono = "📤"; color = R.color.error;       signo = "−"; break;
                default:         icono = "•";  color = R.color.texto_primario; signo = ""; break;
            }
            h.icono.setText(icono);
            h.concepto.setText(m.getConcepto() != null ? m.getConcepto() : tipo);

            StringBuilder detalle = new StringBuilder();
            if (m.getContraparte() != null && !m.getContraparte().isEmpty()) {
                detalle.append("@").append(m.getContraparte());
            }
            if (m.getMetodoPago() != null && !m.getMetodoPago().isEmpty()) {
                if (detalle.length() > 0) detalle.append("  ·  ");
                detalle.append(metodoLegible(m.getMetodoPago()));
                if (m.getTarjetaUltimos4() != null) detalle.append(" ****").append(m.getTarjetaUltimos4());
            }
            h.detalle.setVisibility(detalle.length() == 0 ? View.GONE : View.VISIBLE);
            h.detalle.setText(detalle.toString());

            h.fecha.setText(fmt.format(new Date(m.getFecha())));
            NumberFormat pesos = NumberFormat.getNumberInstance(new Locale("es", "CO"));
            h.monto.setText(signo + "$" + pesos.format(m.getMonto()));
            h.monto.setTextColor(h.itemView.getContext().getColor(color));
        }

        private static String metodoLegible(String m) {
            switch (m) {
                case "TARJETA_CREDITO": return "Tarjeta de Crédito";
                case "TARJETA_DEBITO":  return "Tarjeta de Débito";
                case "PAYPAL":          return "PayPal";
                case "TRANSFERENCIA_INTERNA": return "Transferencia interna";
                default: return m;
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView icono, concepto, detalle, fecha, monto;
            VH(View v) {
                super(v);
                icono = v.findViewById(R.id.textoIconoMov);
                concepto = v.findViewById(R.id.textoConceptoMov);
                detalle = v.findViewById(R.id.textoDetalleMov);
                fecha = v.findViewById(R.id.textoFechaMovSaldo);
                monto = v.findViewById(R.id.textoMontoMov);
            }
        }
    }
}
