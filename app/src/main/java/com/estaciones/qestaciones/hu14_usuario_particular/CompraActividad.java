package com.estaciones.qestaciones.hu14_usuario_particular;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.InventarioDistribuidorDao;
import com.estaciones.qestaciones.datos.MargenEstacionDao;
import com.estaciones.qestaciones.datos.MovimientoInventarioDao;
import com.estaciones.qestaciones.datos.MovimientoSaldoDao;
import com.estaciones.qestaciones.datos.PrecioReferenciaDao;
import com.estaciones.qestaciones.datos.TransaccionDao;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.datos.ValidacionSubsidioDao;
import com.estaciones.qestaciones.hu01_autenticacion.ServidorCorreo;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.estaciones.qestaciones.modelo.MovimientoSaldo;
import com.estaciones.qestaciones.modelo.PrecioReferencia;
import com.estaciones.qestaciones.modelo.Transaccion;
import com.estaciones.qestaciones.modelo.Usuario;
import com.estaciones.qestaciones.modelo.ValidacionSubsidio;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Flujo de compra desde un USUARIO_PARTICULAR a una empresa (estación o distribuidor).
 *
 * - Muestra el inventario disponible en la empresa vendedora (scope por nombreUsuario).
 * - Permite cantidades/combustible + validar placa para subsidio (solo consulta local;
 *   la API de tránsito la corren OPERADOR/REGULADOR/ADMIN previamente).
 * - Al confirmar:
 *     ESTACION  → SALIDA en MovimientoInventario (con empresaUsuario = empresaVendedora)
 *     DISTRIBUIDOR → reducirStock en InventarioDistribuidor
 * - Registra una Transaccion con compradorUsuario + empresaVendedoraUsuario + tipoVendedor
 *   + correoCliente, y envía la factura al correo ingresado (obligatorio).
 */
public class CompraActividad extends AppCompatActivity {

    private static final String[] COMBUSTIBLES = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private UsuarioDao usuarioDao;
    private MargenEstacionDao margenDao;
    private PrecioReferenciaDao refDao;
    private MovimientoInventarioDao movimientoDao;
    private InventarioDistribuidorDao invDistDao;
    private TransaccionDao transaccionDao;
    private ValidacionSubsidioDao validacionDao;
    private MovimientoSaldoDao movSaldoDao;

    // Extras del intent
    private String nombreUsuario = "";        // comprador (cliente particular)
    private String empresaVendedora = "";     // nombreUsuario de la empresa (estación o distribuidor)
    private String empresaVendedoraNombre = "";
    private String tipoVendedor = "ESTACION"; // ESTACION o DISTRIBUIDOR

    // Vistas
    private Spinner spinnerCombustible;
    private TextInputEditText campoLitros, campoPlaca, campoCorreo;
    private TextView stockAcpm, stockCorriente, stockExtra;
    private TextView textoResultado;
    private TextView textoPrecioUnitario, textoTotal;
    private TextView textoTituloVendedor, textoTipoVendedor;
    private TextView textoSaldoCliente;
    private BaseDatosEstaciones db;

    // Estado
    private double precioUnitarioActual = 0;
    private boolean subsidioAplicado = false;
    private String tipoVehiculoActual = "—";
    private String combustibleActual = "ACPM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_compra);

        db = BaseDatosEstaciones.obtenerInstancia(this);
        usuarioDao = db.usuarioDao();
        margenDao = db.margenEstacionDao();
        refDao = db.precioReferenciaDao();
        movimientoDao = db.movimientoInventarioDao();
        invDistDao = db.inventarioDistribuidorDao();
        transaccionDao = db.transaccionDao();
        validacionDao = db.validacionSubsidioDao();
        movSaldoDao = db.movimientoSaldoDao();

        nombreUsuario = nvl(getIntent().getStringExtra("nombreUsuario"));
        empresaVendedora = nvl(getIntent().getStringExtra("empresaVendedora"));
        empresaVendedoraNombre = nvl(getIntent().getStringExtra("empresaVendedoraNombre"));
        String tipo = getIntent().getStringExtra("tipoVendedor");
        if (tipo != null && !tipo.isEmpty()) tipoVendedor = tipo;

        if (empresaVendedora.isEmpty() || nombreUsuario.isEmpty()) {
            Toast.makeText(this, "Datos de compra inválidos", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        vincular();
        configurarEventos();
        precargarCorreoComprador();
        actualizarStockDisponible();
        actualizarPrecio();
    }

    private void vincular() {
        textoTituloVendedor = findViewById(R.id.textoTituloVendedor);
        textoTipoVendedor = findViewById(R.id.textoTipoVendedor);
        stockAcpm = findViewById(R.id.stockAcpmCompra);
        stockCorriente = findViewById(R.id.stockCorrienteCompra);
        stockExtra = findViewById(R.id.stockExtraCompra);
        spinnerCombustible = findViewById(R.id.spinnerCombustibleCompra);
        campoLitros = findViewById(R.id.campoLitrosCompra);
        campoPlaca = findViewById(R.id.campoPlacaCompra);
        campoCorreo = findViewById(R.id.campoCorreoCompra);
        textoResultado = findViewById(R.id.textoResultadoCompra);
        textoPrecioUnitario = findViewById(R.id.textoPrecioUnitarioCompra);
        textoTotal = findViewById(R.id.textoTotalCompra);
        textoSaldoCliente = findViewById(R.id.textoSaldoCliente);

        textoTituloVendedor.setText("⛽ " + (empresaVendedoraNombre.isEmpty()
                ? empresaVendedora : empresaVendedoraNombre));
        textoTipoVendedor.setText(("ESTACION".equals(tipoVendedor) ? "Estación" : "Distribuidor")
                + " · @" + empresaVendedora);

        spinnerCombustible.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COMBUSTIBLES));
    }

    private void configurarEventos() {
        findViewById(R.id.botonConfirmarCompra).setOnClickListener(v -> confirmarCompra());
        findViewById(R.id.botonVolverCompra).setOnClickListener(v -> finish());
        findViewById(R.id.botonVerificarSubsidioCompra).setOnClickListener(v -> verificarSubsidioLocal());

        spinnerCombustible.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                combustibleActual = COMBUSTIBLES[position];
                actualizarPrecio();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        campoLitros.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { recalcularTotal(); }
        });

        // Si cambian la placa, invalidar la aprobación (como en VentaActividad)
        campoPlaca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                subsidioAplicado = false;
                tipoVehiculoActual = "—";
                textoResultado.setVisibility(View.GONE);
                actualizarPrecio();
            }
        });
    }

    private void precargarCorreoComprador() {
        Usuario u = usuarioDao.buscarPorNombreUsuario(nombreUsuario);
        if (u != null && u.getCorreo() != null) {
            campoCorreo.setText(u.getCorreo());
        }
    }

    private void verificarSubsidioLocal() {
        String placa = campoPlaca.getText() != null
                ? campoPlaca.getText().toString().trim().toUpperCase() : "";
        if (placa.isEmpty()) {
            Toast.makeText(this, "Ingresa una placa para verificar subsidio", Toast.LENGTH_SHORT).show();
            return;
        }

        ValidacionSubsidio aprobada = validacionDao.obtenerUltimaAprobadaPorPlaca(placa);
        if (aprobada != null && aprobada.getPrecioAplicado() > 0) {
            subsidioAplicado = true;
            tipoVehiculoActual = aprobada.getTipoVehiculo();
            textoResultado.setVisibility(View.VISIBLE);
            textoResultado.setText("✅ Subsidio aplicado · Tipo: " + tipoVehiculoActual);
            textoResultado.setTextColor(getColor(R.color.exito));
        } else {
            subsidioAplicado = false;
            textoResultado.setVisibility(View.VISIBLE);
            textoResultado.setText("❌ Esta placa no tiene subsidio registrado. Se te cobrará el precio normal.");
            textoResultado.setTextColor(getColor(R.color.error));
        }
        actualizarPrecio();
    }

    private void actualizarStockDisponible() {
        stockAcpm.setText(formatoLitros(saldoDeEmpresa("ACPM")));
        stockCorriente.setText(formatoLitros(saldoDeEmpresa("Gasolina Corriente")));
        stockExtra.setText(formatoLitros(saldoDeEmpresa("Gasolina Extra")));
        actualizarSaldoCliente();
    }

    private void actualizarSaldoCliente() {
        double saldo = usuarioDao.obtenerSaldo(nombreUsuario);
        textoSaldoCliente.setText("$" + FMT.format(saldo));
        // Color del saldo según si alcanza para el total actual
        try {
            String litrosStr = campoLitros.getText() != null ? campoLitros.getText().toString().trim() : "";
            if (!litrosStr.isEmpty() && precioUnitarioActual > 0) {
                double total = Double.parseDouble(litrosStr) * precioUnitarioActual;
                textoSaldoCliente.setTextColor(getColor(saldo >= total ? R.color.exito : R.color.error));
                return;
            }
        } catch (NumberFormatException ignored) {}
        textoSaldoCliente.setTextColor(getColor(R.color.texto_primario));
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarSaldoCliente();
    }

    private double saldoDeEmpresa(String combustible) {
        if ("ESTACION".equals(tipoVendedor)) {
            return movimientoDao.obtenerSaldoPorEmpresa(combustible, empresaVendedora);
        }
        return invDistDao.obtenerStock(empresaVendedora, combustible);
    }

    private void actualizarPrecio() {
        if (subsidioAplicado) {
            ValidacionSubsidio aprobada = obtenerAprobada();
            if (aprobada != null && aprobada.getPrecioAplicado() > 0) {
                precioUnitarioActual = aprobada.getPrecioAplicado();
                recalcularTotal();
                return;
            }
            // Si la aprobada ya no existe (p.ej. la borró un admin), desaplicar
            subsidioAplicado = false;
        }
        precioUnitarioActual = obtenerPrecioEmpresa(combustibleActual);
        recalcularTotal();
    }

    private ValidacionSubsidio obtenerAprobada() {
        String placa = campoPlaca.getText() != null
                ? campoPlaca.getText().toString().trim().toUpperCase() : "";
        if (placa.isEmpty()) return null;
        return validacionDao.obtenerUltimaAprobadaPorPlaca(placa);
    }

    private double obtenerPrecioEmpresa(String combustible) {
        if ("ESTACION".equals(tipoVendedor)) {
            double p = margenDao.obtenerPrecioVenta(empresaVendedora, combustible);
            if (p > 0) return p;
        }
        // Fallback: precio de referencia del regulador
        PrecioReferencia ref = refDao.obtenerUltimoPorTipo(combustible);
        return ref != null ? ref.getPrecioPorLitro() : 0;
    }

    private void recalcularTotal() {
        textoPrecioUnitario.setText(precioUnitarioActual > 0
                ? "$" + FMT.format(precioUnitarioActual) + "/L" : "$—");
        String litrosStr = campoLitros.getText() != null ? campoLitros.getText().toString().trim() : "";
        if (litrosStr.isEmpty() || precioUnitarioActual <= 0) {
            textoTotal.setText("$—");
            actualizarSaldoCliente();
            return;
        }
        try {
            double litros = Double.parseDouble(litrosStr);
            textoTotal.setText("$" + FMT.format(litros * precioUnitarioActual));
        } catch (NumberFormatException e) {
            textoTotal.setText("$—");
        }
        actualizarSaldoCliente();
    }

    private void confirmarCompra() {
        String litrosStr = campoLitros.getText() != null ? campoLitros.getText().toString().trim() : "";
        String correo = campoCorreo.getText() != null ? campoCorreo.getText().toString().trim() : "";
        String placa = campoPlaca.getText() != null
                ? campoPlaca.getText().toString().trim().toUpperCase() : "";
        if (placa.isEmpty()) placa = "SIN_PLACA";

        if (litrosStr.isEmpty()) {
            Toast.makeText(this, "Ingresa la cantidad de litros", Toast.LENGTH_SHORT).show();
            return;
        }
        double litros;
        try {
            litros = Double.parseDouble(litrosStr);
            if (litros <= 0) {
                Toast.makeText(this, "Los litros deben ser mayores a 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Litros inválidos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (precioUnitarioActual <= 0) {
            Toast.makeText(this, "La empresa no tiene precio configurado para este combustible",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Ingresa un correo válido (obligatorio para facturación)",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double stockEmpresa = saldoDeEmpresa(combustibleActual);
        if (litros > stockEmpresa) {
            Toast.makeText(this, String.format(Locale.getDefault(),
                    "Stock insuficiente. Disponible: %.0f L", stockEmpresa), Toast.LENGTH_LONG).show();
            return;
        }

        final double total = litros * precioUnitarioActual;

        // Pre-check de saldo (UX): aviso amable antes de tirar la transacción.
        // El check real es atómico dentro del runInTransaction (decrementarSaldoSiAlcanza).
        double saldoCliente = usuarioDao.obtenerSaldo(nombreUsuario);
        if (saldoCliente < total) {
            Toast.makeText(this, "Saldo insuficiente. Tu saldo es $" + FMT.format(saldoCliente)
                    + " y el total es $" + FMT.format(total)
                    + ". Recarga desde 'Mi Saldo' antes de comprar.", Toast.LENGTH_LONG).show();
            return;
        }

        final double stockEmpresaFinal = stockEmpresa;
        final double saldoNuevoCombustible = stockEmpresa - litros;
        final long ahora = System.currentTimeMillis();
        final double litrosFinal = litros;
        final String placaFinal = placa;
        final String correoFinal = correo;

        // ── Operación atómica: inventario + transacción + transferencia de saldo ──
        // Si la transferencia falla por saldo, ningún cambio queda persistido.
        final boolean[] saldoInsuficiente = {false};
        try {
            db.runInTransaction(() -> {
                // 1) Descontar inventario
                if ("ESTACION".equals(tipoVendedor)) {
                    MovimientoInventario mov = new MovimientoInventario();
                    mov.setTipoMovimiento("SALIDA");
                    mov.setTipoCombustible(combustibleActual);
                    mov.setVolumenLitros(litrosFinal);
                    mov.setSaldoAnterior(stockEmpresaFinal);
                    mov.setSaldoNuevo(saldoNuevoCombustible);
                    mov.setMotivo("Venta a cliente @" + nombreUsuario
                            + (subsidioAplicado ? " [SUBSIDIADO placa " + placaFinal + "]" : ""));
                    mov.setResponsable(empresaVendedora);
                    mov.setEsAuditable(true);
                    mov.setEmpresaUsuario(empresaVendedora);
                    movimientoDao.insertar(mov);
                } else {
                    invDistDao.reducirStock(empresaVendedora, combustibleActual, litrosFinal, ahora);
                }

                // 2) Transferir saldo (atómico). Si comprador no alcanza, abortamos toda la transacción.
                double saldoCompradorAntes = usuarioDao.obtenerSaldo(nombreUsuario);
                double saldoVendedorAntes = usuarioDao.obtenerSaldo(empresaVendedora);
                int filas = usuarioDao.decrementarSaldoSiAlcanza(nombreUsuario, total);
                if (filas == 0) {
                    saldoInsuficiente[0] = true;
                    throw new RuntimeException("SALDO_INSUFICIENTE_ROLLBACK");
                }
                usuarioDao.incrementarSaldo(empresaVendedora, total);

                String concepto = "Compra de " + String.format(Locale.getDefault(),
                        "%.2f L %s", litrosFinal, combustibleActual)
                        + " a " + (empresaVendedoraNombre.isEmpty()
                                ? "@" + empresaVendedora : empresaVendedoraNombre);

                MovimientoSaldo pago = new MovimientoSaldo();
                pago.setUsuarioNombre(nombreUsuario);
                pago.setTipo("PAGO");
                pago.setMonto(total);
                pago.setSaldoAnterior(saldoCompradorAntes);
                pago.setSaldoNuevo(saldoCompradorAntes - total);
                pago.setConcepto(concepto);
                pago.setContraparte(empresaVendedora);
                pago.setMetodoPago("TRANSFERENCIA_INTERNA");
                movSaldoDao.insertar(pago);

                MovimientoSaldo ingreso = new MovimientoSaldo();
                ingreso.setUsuarioNombre(empresaVendedora);
                ingreso.setTipo("INGRESO");
                ingreso.setMonto(total);
                ingreso.setSaldoAnterior(saldoVendedorAntes);
                ingreso.setSaldoNuevo(saldoVendedorAntes + total);
                ingreso.setConcepto("Venta a @" + nombreUsuario);
                ingreso.setContraparte(nombreUsuario);
                ingreso.setMetodoPago("TRANSFERENCIA_INTERNA");
                movSaldoDao.insertar(ingreso);

                // 3) Registrar la transacción comercial
                Transaccion tx = new Transaccion();
                tx.setPlaca(placaFinal);
                tx.setTipoVehiculo(tipoVehiculoActual);
                tx.setTipoCombustible(combustibleActual);
                tx.setVolumenLitros(litrosFinal);
                tx.setPrecioUnitario(precioUnitarioActual);
                tx.setTotalCop(total);
                tx.setSubsidioAplicado(subsidioAplicado);
                tx.setNombreEstacion(empresaVendedoraNombre.isEmpty() ? empresaVendedora : empresaVendedoraNombre);
                tx.setOperador(empresaVendedora);
                tx.setEmpresaVendedoraUsuario(empresaVendedora);
                tx.setTipoVendedor(tipoVendedor);
                tx.setCompradorUsuario(nombreUsuario);
                tx.setCorreoCliente(correoFinal);
                transaccionDao.insertar(tx);
            });
        } catch (RuntimeException e) {
            if (saldoInsuficiente[0]) {
                Toast.makeText(this,
                        "Saldo insuficiente al procesar el pago. No se realizó la compra.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            throw e; // otra falla: que el crash sea visible
        }

        // 4) Factura + refresco UI
        mostrarFactura(placaFinal, combustibleActual, litrosFinal, precioUnitarioActual, total, correoFinal);
        actualizarStockDisponible();
    }

    private void mostrarFactura(String placa, String combustible, double litros,
                                 double precio, double total, String correoDestino) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String factura = "════════════════════════\n"
                + "    FACTURA DE COMPRA\n"
                + "════════════════════════\n"
                + "Fecha: " + fmt.format(new Date()) + "\n"
                + "Vendedor: " + (empresaVendedoraNombre.isEmpty() ? empresaVendedora : empresaVendedoraNombre) + "\n"
                + "Tipo: " + ("ESTACION".equals(tipoVendedor) ? "Estación" : "Distribuidor") + "\n"
                + "Cliente: @" + nombreUsuario + "\n"
                + (subsidioAplicado ? "Placa: " + placa + "\n" : "")
                + "Combustible: " + combustible + "\n"
                + "Litros: " + String.format(Locale.getDefault(), "%.2f L", litros) + "\n"
                + "Precio/L: $" + FMT.format(precio) + "\n"
                + (subsidioAplicado ? "Subsidio: APLICADO ✅\n" : "")
                + "────────────────────────\n"
                + "TOTAL PAGADO: $" + FMT.format(total) + "\n"
                + "════════════════════════";

        Toast.makeText(this, "Enviando factura a " + correoDestino + "...",
                Toast.LENGTH_SHORT).show();
        new ServidorCorreo().enviarFactura(correoDestino,
                "Factura de Compra — Q-Estaciones", factura,
                new ServidorCorreo.ResultadoEnvio() {
                    @Override public void onExito() {
                        Toast.makeText(CompraActividad.this,
                                "✉ Factura enviada a " + correoDestino,
                                Toast.LENGTH_LONG).show();
                    }
                    @Override public void onError(String mensajeError) {
                        Toast.makeText(CompraActividad.this,
                                "No se pudo enviar la factura: " + mensajeError,
                                Toast.LENGTH_LONG).show();
                    }
                });

        new AlertDialog.Builder(this)
                .setTitle("Compra Registrada")
                .setMessage(factura)
                .setPositiveButton("Cerrar", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private static String formatoLitros(double litros) {
        return String.format(Locale.getDefault(), "%.0f L", litros);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
