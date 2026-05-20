package com.estaciones.qestaciones.hu08_inventario;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.hu01_autenticacion.ServidorCorreo;
import com.google.android.material.textfield.TextInputLayout;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.MargenEstacionDao;
import com.estaciones.qestaciones.datos.MovimientoInventarioDao;
import com.estaciones.qestaciones.datos.PrecioReferenciaDao;
import com.estaciones.qestaciones.datos.TransaccionDao;
import com.estaciones.qestaciones.datos.ValidacionSubsidioDao;
import com.estaciones.qestaciones.hu05_subsidios.GestorSubsidios;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.estaciones.qestaciones.modelo.PrecioReferencia;
import com.estaciones.qestaciones.modelo.Transaccion;
import com.estaciones.qestaciones.modelo.ValidacionSubsidio;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Flujo completo de venta de combustible:
 * 1. Seleccionar combustible y litros
 * 2. Verificar subsidio por placa (llama a GestorSubsidios)
 * 3. Mostrar precio aplicado (subsidiado o precio de venta de la estación)
 * 4. Confirmar → registra Transaccion + MovimientoInventario SALIDA
 * 5. Muestra factura
 */
public class VentaActividad extends AppCompatActivity {

    private static final String[] COMBUSTIBLES = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private GestorSubsidios gestorSubsidios;
    private MovimientoInventarioDao movimientoDao;
    private TransaccionDao transaccionDao;
    private MargenEstacionDao margenDao;
    private PrecioReferenciaDao refDao;
    private ValidacionSubsidioDao validacionDao;

    private TextInputEditText campoEstacion, campoPlaca, campoLitros;
    private TextInputEditText campoCorreoFactura;
    private TextInputLayout contenedorCorreoFactura;
    private CheckBox checkEnviarFactura;
    private Spinner spinnerCombustible;
    private LinearLayout panelResultado;
    private TextView textoResultadoSubsidio, textoTipoVehiculo;
    private TextView textoPrecioUnitario, textoTotal;

    private double precioUnitarioActual = 0;
    private boolean subsidioAplicado = false;
    private String tipoVehiculoActual = "—";
    private GestorSubsidios.ResultadoValidacion ultimaValidacion = null;

    private String rolUsuario = "";
    private String nombreUsuario = "";
    // Bug 5: la ESTACION no puede vender a una placa que no fue validada previamente
    // por un OPERADOR/REGULADOR/ADMIN. Este flag se pone en false cuando ese caso se detecta.
    private boolean ventaPermitida = true;
    // true tras hacer clic en "Verificar"; false si aún no verificó la placa actual.
    private boolean placaVerificada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_venta);

        BaseDatosEstaciones db = BaseDatosEstaciones.obtenerInstancia(this);
        gestorSubsidios = new GestorSubsidios(this);
        movimientoDao = db.movimientoInventarioDao();
        transaccionDao = db.transaccionDao();
        margenDao = db.margenEstacionDao();
        refDao = db.precioReferenciaDao();
        validacionDao = db.validacionSubsidioDao();

        String rolIntent = getIntent().getStringExtra("rol");
        rolUsuario = rolIntent != null ? rolIntent : "";
        String usuarioIntent = getIntent().getStringExtra("nombreUsuario");
        nombreUsuario = usuarioIntent != null ? usuarioIntent : "";

        vincularVistas();
        configurarEventos();

        // Auto-rellenar el campo de estación con el usuario logueado (no editable)
        if (!nombreUsuario.isEmpty()) {
            campoEstacion.setText(nombreUsuario);
            campoEstacion.setEnabled(false);
            campoEstacion.setFocusable(false);
        }
    }

    private void vincularVistas() {
        campoEstacion = findViewById(R.id.campoEstacionVenta);
        campoPlaca = findViewById(R.id.campoPlacaVenta);
        campoLitros = findViewById(R.id.campoLitrosVenta);
        spinnerCombustible = findViewById(R.id.spinnerCombustibleVenta);
        panelResultado = findViewById(R.id.panelResultadoSubsidio);
        textoResultadoSubsidio = findViewById(R.id.textoResultadoSubsidio);
        textoTipoVehiculo = findViewById(R.id.textoTipoVehiculoVenta);
        textoPrecioUnitario = findViewById(R.id.textoPrecioUnitarioVenta);
        textoTotal = findViewById(R.id.textoTotalVenta);
        checkEnviarFactura = findViewById(R.id.checkEnviarFactura);
        contenedorCorreoFactura = findViewById(R.id.contenedorCorreoFactura);
        campoCorreoFactura = findViewById(R.id.campoCorreoFactura);

        spinnerCombustible.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COMBUSTIBLES));
    }

    private void configurarEventos() {
        findViewById(R.id.botonVerificarSubsidio).setOnClickListener(v -> verificarSubsidio());
        findViewById(R.id.botonConfirmarVenta).setOnClickListener(v -> confirmarVenta());
        findViewById(R.id.botonVolverVenta).setOnClickListener(v -> finish());

        checkEnviarFactura.setOnCheckedChangeListener((btn, checked) ->
                contenedorCorreoFactura.setVisibility(checked ? View.VISIBLE : View.GONE));

        campoLitros.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                recalcularTotal();
            }
        });

        // Bug 5: si cambian la placa tras verificar, invalidar la autorización previa
        campoPlaca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                precioUnitarioActual = 0;
                subsidioAplicado = false;
                tipoVehiculoActual = "—";
                // Con placa vacía la venta es válida (precio normal); con placa nueva, requiere verificar
                ventaPermitida = s.length() == 0;
                placaVerificada = false;
                panelResultado.setVisibility(View.GONE);
                recalcularTotal();
            }
        });
    }

    private void verificarSubsidio() {
        String placa = campoPlaca.getText() != null ? campoPlaca.getText().toString().trim().toUpperCase() : "";
        String combustible = spinnerCombustible.getSelectedItem().toString();

        // A partir de este punto el usuario ya hizo clic en "Verificar".
        placaVerificada = true;

        if (placa.isEmpty()) {
            // Sin placa: venta normal al precio de la estación (permitido a todos los roles)
            precioUnitarioActual = obtenerPrecioVentaEstacion(combustible);
            subsidioAplicado = false;
            tipoVehiculoActual = "Sin placa";
            ventaPermitida = true;
            mostrarResultadoSubsidio(false, "SIN VERIFICACIÓN DE PLACA",
                    "Precio de venta de la estación", precioUnitarioActual);
            return;
        }

        // Bug 5: La ESTACION NO invoca la API de tránsito. Solo consulta validaciones
        // previamente aprobadas por un OPERADOR/REGULADOR/ADMIN.
        if ("ESTACION".equals(rolUsuario)) {
            ValidacionSubsidio aprobada = validacionDao.obtenerUltimaAprobadaPorPlaca(placa);
            if (aprobada != null && aprobada.getPrecioAplicado() > 0) {
                subsidioAplicado = true;
                precioUnitarioActual = aprobada.getPrecioAplicado();
                tipoVehiculoActual = aprobada.getTipoVehiculo();
                ventaPermitida = true;
                mostrarResultadoSubsidio(true, "✅ SUBSIDIO APLICA",
                        "Placa validada previamente | Tipo: " + tipoVehiculoActual,
                        precioUnitarioActual);
            } else {
                subsidioAplicado = false;
                precioUnitarioActual = 0;
                tipoVehiculoActual = "No validado";
                ventaPermitida = false;
                mostrarResultadoSubsidio(false, "❌ LA PLACA NO TIENE SUBSIDIO REGISTRADO",
                        "No se puede vender: esta placa no ha sido validada por un operador o regulador.",
                        0);
            }
            recalcularTotal();
            return;
        }

        // OPERADOR/REGULADOR/ADMINISTRADOR: consulta a la API de tránsito (RUNT)
        Toast.makeText(this, "Consultando API de Tránsito...", Toast.LENGTH_SHORT).show();
        ultimaValidacion = gestorSubsidios.validarSubsidio(placa);

        subsidioAplicado = ultimaValidacion.isAplicaSubsidio();
        tipoVehiculoActual = ultimaValidacion.getTipoVehiculo();
        ventaPermitida = true;

        if (subsidioAplicado && ultimaValidacion.getPrecioAplicado() > 0) {
            precioUnitarioActual = ultimaValidacion.getPrecioAplicado();
            mostrarResultadoSubsidio(true, "✅ SUBSIDIO APLICA",
                    "Tipo: " + tipoVehiculoActual + " | " + ultimaValidacion.getFuenteDatos(),
                    precioUnitarioActual);
        } else {
            precioUnitarioActual = obtenerPrecioVentaEstacion(combustible);
            String motivo = ultimaValidacion.getMotivoRechazo() != null
                    ? ultimaValidacion.getMotivoRechazo() : "No aplica";
            mostrarResultadoSubsidio(false, "❌ SUBSIDIO NO APLICA",
                    "Motivo: " + motivo + " | Tipo: " + tipoVehiculoActual,
                    precioUnitarioActual);
        }
        recalcularTotal();
    }

    private void mostrarResultadoSubsidio(boolean aplica, String titulo, String detalle, double precio) {
        panelResultado.setVisibility(View.VISIBLE);
        textoResultadoSubsidio.setText(titulo);
        textoResultadoSubsidio.setTextColor(getColor(aplica ? R.color.exito : R.color.error));
        textoTipoVehiculo.setText(detalle);
        textoPrecioUnitario.setText("$" + FMT.format(precio) + "/L");
        recalcularTotal();
    }

    private double obtenerPrecioVentaEstacion(String combustible) {
        String estacion = campoEstacion.getText() != null
                ? campoEstacion.getText().toString().trim() : "";
        if (!estacion.isEmpty()) {
            double precioVenta = margenDao.obtenerPrecioVenta(estacion, combustible);
            if (precioVenta > 0) return precioVenta;
        }
        // Fallback: precio de referencia del regulador
        PrecioReferencia ref = refDao.obtenerUltimoPorTipo(combustible);
        return ref != null ? ref.getPrecioPorLitro() : 0;
    }

    private void recalcularTotal() {
        if (precioUnitarioActual <= 0) {
            textoTotal.setText("$—");
            return;
        }
        String litrosStr = campoLitros.getText() != null ? campoLitros.getText().toString().trim() : "";
        if (litrosStr.isEmpty()) {
            textoTotal.setText("$—");
            return;
        }
        try {
            double litros = Double.parseDouble(litrosStr);
            double total = litros * precioUnitarioActual;
            textoPrecioUnitario.setText("$" + FMT.format(precioUnitarioActual) + "/L");
            textoTotal.setText("$" + FMT.format(total));
        } catch (NumberFormatException e) {
            textoTotal.setText("$—");
        }
    }

    private void confirmarVenta() {
        String estacion = campoEstacion.getText() != null ? campoEstacion.getText().toString().trim() : "";
        String litrosStr = campoLitros.getText() != null ? campoLitros.getText().toString().trim() : "";
        String combustible = spinnerCombustible.getSelectedItem().toString();
        String placa = campoPlaca.getText() != null
                ? campoPlaca.getText().toString().trim().toUpperCase() : "SIN_PLACA";
        if (placa.isEmpty()) placa = "SIN_PLACA";

        if (litrosStr.isEmpty()) {
            Toast.makeText(this, "Ingrese los litros a despachar", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hayPlaca = !"SIN_PLACA".equals(placa) && !placa.isEmpty();

        // Si ingresó placa pero aún no tocó "Verificar", no trates eso como bloqueo de subsidio:
        // simplemente pídele que la verifique.
        if (hayPlaca && !placaVerificada) {
            Toast.makeText(this,
                    "Primero toca \"Verificar\" para validar la placa antes de confirmar la venta.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Bug 5: si ya verificó y la placa no tiene subsidio aprobado (solo ESTACION), bloquear.
        if (!ventaPermitida) {
            Toast.makeText(this,
                    "Venta bloqueada: la placa no tiene subsidio registrado. Solicite al operador que valide la placa primero.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (precioUnitarioActual <= 0) {
            Toast.makeText(this, "Primero verifique el subsidio o configure el precio de venta", Toast.LENGTH_LONG).show();
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

        // Validar correo si el cliente pidió factura electrónica
        boolean enviarFactura = checkEnviarFactura.isChecked();
        String correoFactura = campoCorreoFactura.getText() != null
                ? campoCorreoFactura.getText().toString().trim() : "";
        if (enviarFactura) {
            if (correoFactura.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correoFactura).matches()) {
                Toast.makeText(this, "Ingrese un correo válido para enviar la factura",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Verificar saldo en inventario (aislado por empresa)
        double saldo = nombreUsuario.isEmpty()
                ? movimientoDao.obtenerSaldoActual(combustible)
                : movimientoDao.obtenerSaldoPorEmpresa(combustible, nombreUsuario);
        if (litros > saldo) {
            Toast.makeText(this, String.format(Locale.getDefault(),
                    "Saldo insuficiente. Disponible: %.0f L", saldo), Toast.LENGTH_LONG).show();
            return;
        }

        double total = litros * precioUnitarioActual;

        // 1) Registrar movimiento de inventario (SALIDA)
        double saldoNuevo = saldo - litros;
        MovimientoInventario mov = new MovimientoInventario();
        mov.setTipoMovimiento("SALIDA");
        mov.setTipoCombustible(combustible);
        mov.setVolumenLitros(litros);
        mov.setSaldoAnterior(saldo);
        mov.setSaldoNuevo(saldoNuevo);
        mov.setMotivo("Venta a placa " + placa + (subsidioAplicado ? " [SUBSIDIADO]" : ""));
        mov.setResponsable(estacion.isEmpty() ? "estacion" : estacion);
        mov.setEsAuditable(true);
        mov.setEmpresaUsuario(nombreUsuario.isEmpty() ? null : nombreUsuario);
        movimientoDao.insertar(mov);

        // 2) Registrar transacción
        Transaccion tx = new Transaccion();
        tx.setPlaca(placa);
        tx.setTipoVehiculo(tipoVehiculoActual);
        tx.setTipoCombustible(combustible);
        tx.setVolumenLitros(litros);
        tx.setPrecioUnitario(precioUnitarioActual);
        tx.setTotalCop(total);
        tx.setSubsidioAplicado(subsidioAplicado);
        tx.setNombreEstacion(estacion.isEmpty() ? "Estación" : estacion);
        tx.setOperador(nombreUsuario.isEmpty() ? "operador" : nombreUsuario);
        tx.setEmpresaVendedoraUsuario(nombreUsuario.isEmpty() ? null : nombreUsuario);
        tx.setTipoVendedor("ESTACION");
        transaccionDao.insertar(tx);

        // 3) Mostrar factura (y enviar por correo si el cliente lo pidió)
        String correoDestino = enviarFactura ? correoFactura : null;
        mostrarFactura(placa, combustible, litros, precioUnitarioActual, total, saldoNuevo,
                correoDestino);
    }

    private void mostrarFactura(String placa, String combustible, double litros,
                                 double precio, double total, double saldoNuevo,
                                 String correoDestino) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String fechaHora = fmt.format(new Date());

        String factura = "════════════════════════\n"
                + "     FACTURA DE VENTA\n"
                + "════════════════════════\n"
                + "Fecha: " + fechaHora + "\n"
                + "Placa: " + placa + "\n"
                + "Vehículo: " + tipoVehiculoActual + "\n"
                + "Combustible: " + combustible + "\n"
                + "Litros: " + String.format(Locale.getDefault(), "%.2f L", litros) + "\n"
                + "Precio/L: $" + FMT.format(precio) + "\n"
                + (subsidioAplicado ? "Subsidio: APLICADO ✅\n" : "")
                + "────────────────────────\n"
                + "TOTAL: $" + FMT.format(total) + "\n"
                + "════════════════════════\n"
                + "Saldo estación: " + String.format(Locale.getDefault(), "%.0f L", saldoNuevo);

        if (correoDestino != null && !correoDestino.isEmpty()) {
            Toast.makeText(this, "Enviando factura a " + correoDestino + "...",
                    Toast.LENGTH_SHORT).show();
            new ServidorCorreo().enviarFactura(correoDestino,
                    "Factura de Venta — Q-Estaciones", factura,
                    new ServidorCorreo.ResultadoEnvio() {
                        @Override public void onExito() {
                            Toast.makeText(VentaActividad.this,
                                    "✉ Factura enviada a " + correoDestino,
                                    Toast.LENGTH_LONG).show();
                        }
                        @Override public void onError(String mensajeError) {
                            Toast.makeText(VentaActividad.this,
                                    "No se pudo enviar la factura: " + mensajeError,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }

        new AlertDialog.Builder(this)
                .setTitle("Venta Registrada")
                .setMessage(factura)
                .setPositiveButton("Cerrar", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}
