package com.estaciones.qestaciones.hu09_entregas;

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
import com.estaciones.qestaciones.datos.InventarioDistribuidorDao;
import com.estaciones.qestaciones.datos.MovimientoInventarioDao;
import com.estaciones.qestaciones.datos.OrdenDespachoDao;
import com.estaciones.qestaciones.datos.RegistroEntregaDao;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.InventarioDistribuidor;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.estaciones.qestaciones.modelo.OrdenDespacho;
import com.estaciones.qestaciones.modelo.RegistroEntrega;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HU-Distribuidor: Gestión del inventario propio del distribuidor.
 * - Agregar stock (combustible recibido de refinería)
 * - Crear órdenes de despacho hacia estaciones
 * - Al confirmar entrega: baja del inventario propio + ENTRADA automática en la estación
 */
public class InventarioDistribuidorActividad extends AppCompatActivity {

    private static final String[] COMBUSTIBLES = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};

    private InventarioDistribuidorDao inventarioDao;
    private OrdenDespachoDao ordenDao;
    private MovimientoInventarioDao movimientoDao;
    private RegistroEntregaDao entregaDao;

    private TextInputEditText campoNombreDistribuidor;
    private TextInputEditText campoLitrosDist, campoLitrosOrden, campoObsOrden;
    private Spinner spinnerCombustibleDist, spinnerCombustibleOrden, spinnerEstacionOrden;
    private TextView stockAcpm, stockCorriente, stockExtra;
    private RecyclerView listaOrdenes;
    private OrdenesAdaptador adaptador;
    private View panelAgregarStock, panelCrearOrden;

    private String distribuidorActual = "";
    private String nombreCompletoUsuario = "";
    private UsuarioDao usuarioDao;
    private List<Usuario> listaEstaciones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_inventario_distribuidor);

        BaseDatosEstaciones db = BaseDatosEstaciones.obtenerInstancia(this);
        inventarioDao = db.inventarioDistribuidorDao();
        ordenDao = db.ordenDespachoDao();
        movimientoDao = db.movimientoInventarioDao();
        entregaDao = db.registroEntregaDao();
        usuarioDao = db.usuarioDao();

        // Aislamiento por empresa: cada usuario DISTRIBUIDOR es su propia empresa.
        // El identificador estable en la BD es nombreUsuario (único, inmutable).
        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        nombreCompletoUsuario = getIntent().getStringExtra("nombreCompleto");
        if (nombreUsuario != null && !nombreUsuario.trim().isEmpty()) {
            distribuidorActual = nombreUsuario.trim();
        }

        vincularVistas();
        configurarEventos();
        prefijarNombreDistribuidor();
        actualizarStock();
        cargarOrdenes();
    }

    private void prefijarNombreDistribuidor() {
        if (distribuidorActual.isEmpty()) return;
        String etiqueta = (nombreCompletoUsuario != null && !nombreCompletoUsuario.trim().isEmpty())
                ? nombreCompletoUsuario.trim() + " (" + distribuidorActual + ")"
                : distribuidorActual;
        campoNombreDistribuidor.setText(etiqueta);
        campoNombreDistribuidor.setEnabled(false);
        campoNombreDistribuidor.setFocusable(false);
    }

    private void vincularVistas() {
        campoNombreDistribuidor = findViewById(R.id.campoNombreDistribuidor);
        campoLitrosDist = findViewById(R.id.campoLitrosDist);
        campoLitrosOrden = findViewById(R.id.campoLitrosOrden);
        campoObsOrden = findViewById(R.id.campoObsOrden);
        spinnerCombustibleDist = findViewById(R.id.spinnerCombustibleDist);
        spinnerCombustibleOrden = findViewById(R.id.spinnerCombustibleOrden);
        spinnerEstacionOrden = findViewById(R.id.spinnerEstacionOrden);
        stockAcpm = findViewById(R.id.stockAcpmDist);
        stockCorriente = findViewById(R.id.stockCorrienteDist);
        stockExtra = findViewById(R.id.stockExtraDist);
        listaOrdenes = findViewById(R.id.listaOrdenes);
        panelAgregarStock = findViewById(R.id.panelAgregarStock);
        panelCrearOrden = findViewById(R.id.panelCrearOrden);

        ArrayAdapter<String> adapterComb = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COMBUSTIBLES);
        spinnerCombustibleDist.setAdapter(adapterComb);
        spinnerCombustibleOrden.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COMBUSTIBLES));

        listaOrdenes.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new OrdenesAdaptador(this::confirmarEntrega);
        listaOrdenes.setAdapter(adaptador);

        // Poblar spinner de estaciones desde BD
        poblarSpinnerEstaciones();
    }

    private void poblarSpinnerEstaciones() {
        listaEstaciones = usuarioDao.obtenerPorRol("ESTACION");
        List<String> nombres = new ArrayList<>();
        if (listaEstaciones.isEmpty()) {
            nombres.add("(No hay estaciones registradas)");
        } else {
            for (Usuario u : listaEstaciones) {
                String etiqueta = u.getNombreCompleto() + " (@" + u.getNombreUsuario() + ")";
                nombres.add(etiqueta);
            }
        }
        ArrayAdapter<String> adapterEstaciones = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, nombres);
        spinnerEstacionOrden.setAdapter(adapterEstaciones);
    }

    private void configurarEventos() {
        findViewById(R.id.botonAgregarStock).setOnClickListener(v -> agregarStock());
        findViewById(R.id.botonCrearOrden).setOnClickListener(v -> crearOrden());
        findViewById(R.id.botonVolverDist).setOnClickListener(v -> finish());

        MaterialButton tabStock = findViewById(R.id.tabAgregarStock);
        MaterialButton tabOrden = findViewById(R.id.tabCrearOrden);

        tabStock.setOnClickListener(v -> {
            panelAgregarStock.setVisibility(View.VISIBLE);
            panelCrearOrden.setVisibility(View.GONE);
        });
        tabOrden.setOnClickListener(v -> {
            panelAgregarStock.setVisibility(View.GONE);
            panelCrearOrden.setVisibility(View.VISIBLE);
        });
    }

    private String obtenerNombreDistribuidor() {
        // Si ya está fijado desde el usuario logueado, usar ese identificador directamente
        // (el campo visual puede mostrar "Nombre Completo (usuario)", que no sirve como key de BD).
        if (!distribuidorActual.isEmpty()) {
            return distribuidorActual;
        }
        String nombre = campoNombreDistribuidor.getText() != null
                ? campoNombreDistribuidor.getText().toString().trim() : "";
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del distribuidor", Toast.LENGTH_SHORT).show();
        }
        return nombre;
    }

    private void agregarStock() {
        String nombre = obtenerNombreDistribuidor();
        if (nombre.isEmpty()) return;
        distribuidorActual = nombre;

        String combustible = spinnerCombustibleDist.getSelectedItem().toString();
        String litrosStr = campoLitrosDist.getText() != null ? campoLitrosDist.getText().toString().trim() : "";

        if (litrosStr.isEmpty()) {
            Toast.makeText(this, "Ingrese los litros recibidos", Toast.LENGTH_SHORT).show();
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

        // Verificar si ya existe inventario para este distribuidor + combustible
        InventarioDistribuidor inv = inventarioDao.obtenerPorDistribuidorYTipo(nombre, combustible);
        if (inv == null) {
            inv = new InventarioDistribuidor();
            inv.setNombreDistribuidor(nombre);
            inv.setTipoCombustible(combustible);
            inv.setStockLitros(litros);
            inventarioDao.insertar(inv);
        } else {
            inventarioDao.agregarStock(nombre, combustible, litros, System.currentTimeMillis());
        }

        Toast.makeText(this, String.format(Locale.getDefault(),
                "Stock actualizado: +%.0f L de %s", litros, combustible), Toast.LENGTH_SHORT).show();
        campoLitrosDist.setText("");
        actualizarStock();
    }

    private void crearOrden() {
        String nombre = obtenerNombreDistribuidor();
        if (nombre.isEmpty()) return;
        distribuidorActual = nombre;

        String combustible = spinnerCombustibleOrden.getSelectedItem().toString();

        // Obtener estación seleccionada del spinner
        String estacion;
        if (listaEstaciones.isEmpty()) {
            Toast.makeText(this, "No hay estaciones registradas", Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = spinnerEstacionOrden.getSelectedItemPosition();
        estacion = listaEstaciones.get(pos).getNombreUsuario();

        String litrosStr = campoLitrosOrden.getText() != null ? campoLitrosOrden.getText().toString().trim() : "";
        String obs = campoObsOrden.getText() != null ? campoObsOrden.getText().toString().trim() : "";

        if (litrosStr.isEmpty()) {
            Toast.makeText(this, "Complete los litros a despachar", Toast.LENGTH_SHORT).show();
            return;
        }

        double litros;
        try {
            litros = Double.parseDouble(litrosStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Litros inválidos", Toast.LENGTH_SHORT).show();
            return;
        }

        double stockDisponible = inventarioDao.obtenerStock(nombre, combustible);
        if (litros > stockDisponible) {
            Toast.makeText(this, String.format(Locale.getDefault(),
                    "Stock insuficiente. Disponible: %.0f L de %s", stockDisponible, combustible),
                    Toast.LENGTH_LONG).show();
            return;
        }

        int numOrdenes = ordenDao.contarOrdenesPorDistribuidor(nombre) + 1;
        OrdenDespacho orden = new OrdenDespacho();
        orden.setNumeroOrden(String.format(Locale.getDefault(), "OD-%04d", numOrdenes));
        orden.setDistribuidorNombre(nombre);
        orden.setEstacionDestino(estacion);
        orden.setTipoCombustible(combustible);
        orden.setVolumenLitros(litros);
        orden.setObservaciones(obs.isEmpty() ? "Despacho programado" : obs);
        ordenDao.insertar(orden);

        Toast.makeText(this, "Orden " + orden.getNumeroOrden() + " creada — PENDIENTE de entrega",
                Toast.LENGTH_SHORT).show();

        campoLitrosOrden.setText("");
        campoObsOrden.setText("");
        cargarOrdenes();
    }

    private void confirmarEntrega(OrdenDespacho orden) {
        String dist = orden.getDistribuidorNombre();
        String combustible = orden.getTipoCombustible();
        double litros = orden.getVolumenLitros();

        // 1) Reducir stock del distribuidor
        inventarioDao.reducirStock(dist, combustible, litros, System.currentTimeMillis());

        // 2) Crear ENTRADA en el inventario de la estación destino (aislado por empresa).
        // estacionDestino es el nombreUsuario de la estación (spinner guarda user, no nombre).
        String estacionDestino = orden.getEstacionDestino();
        double saldoActual = estacionDestino != null && !estacionDestino.isEmpty()
                ? movimientoDao.obtenerSaldoPorEmpresa(combustible, estacionDestino)
                : movimientoDao.obtenerSaldoActual(combustible);
        MovimientoInventario entrada = new MovimientoInventario();
        entrada.setTipoMovimiento("ENTRADA");
        entrada.setTipoCombustible(combustible);
        entrada.setVolumenLitros(litros);
        entrada.setSaldoAnterior(saldoActual);
        entrada.setSaldoNuevo(saldoActual + litros);
        entrada.setMotivo("Entrega de " + dist + " — " + orden.getNumeroOrden());
        entrada.setResponsable(dist);
        entrada.setDistribuidorNombre(dist);
        entrada.setEsAuditable(true);
        entrada.setEmpresaUsuario(estacionDestino); // scope: esta entrada suma al inventario de la estación destino
        movimientoDao.insertar(entrada);

        // 3) Registrar en historial de entregas (HU-09)
        RegistroEntrega reg = new RegistroEntrega();
        reg.setEstacionDestino(orden.getEstacionDestino());
        reg.setVehiculoTransporte("Vehículo distribuidor");
        reg.setPlacaVehiculo("—");
        reg.setVolumenLitros(litros);
        reg.setTipoCombustible(combustible);
        reg.setObservaciones("Orden " + orden.getNumeroOrden() + " confirmada");
        reg.setRegistradoPor(dist);
        entregaDao.insertar(reg);

        // 4) Actualizar estado de la orden
        ordenDao.actualizarEstado(orden.getId(), "ENTREGADA", System.currentTimeMillis());

        Toast.makeText(this, "✅ Entrega confirmada — " + orden.getNumeroOrden()
                + " → " + orden.getEstacionDestino(), Toast.LENGTH_LONG).show();

        actualizarStock();
        cargarOrdenes();
    }

    private void actualizarStock() {
        // Si no viene del login (flujo legacy), leer del campo como fallback.
        if (distribuidorActual.isEmpty()
                && campoNombreDistribuidor.isEnabled()
                && campoNombreDistribuidor.getText() != null) {
            distribuidorActual = campoNombreDistribuidor.getText().toString().trim();
        }
        if (distribuidorActual.isEmpty()) {
            // Mostrar 0 explícitamente en vez de dejar valores stale
            stockAcpm.setText("0 L");
            stockCorriente.setText("0 L");
            stockExtra.setText("0 L");
            return;
        }

        stockAcpm.setText(String.format(Locale.getDefault(), "%.0f L",
                inventarioDao.obtenerStock(distribuidorActual, "ACPM")));
        stockCorriente.setText(String.format(Locale.getDefault(), "%.0f L",
                inventarioDao.obtenerStock(distribuidorActual, "Gasolina Corriente")));
        stockExtra.setText(String.format(Locale.getDefault(), "%.0f L",
                inventarioDao.obtenerStock(distribuidorActual, "Gasolina Extra")));
    }

    private void cargarOrdenes() {
        List<OrdenDespacho> ordenes;
        if (!distribuidorActual.isEmpty()) {
            ordenes = ordenDao.obtenerPorDistribuidor(distribuidorActual);
        } else {
            ordenes = ordenDao.obtenerTodas();
        }
        adaptador.actualizarDatos(ordenes);
    }

    // ─── Adaptador ────────────────────────────────────────────────────────────

    private static class OrdenesAdaptador extends RecyclerView.Adapter<OrdenesAdaptador.VH> {

        interface OnConfirmarEntrega { void confirmar(OrdenDespacho orden); }

        private List<OrdenDespacho> items = new ArrayList<>();
        private final OnConfirmarEntrega listener;
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        OrdenesAdaptador(OnConfirmarEntrega listener) {
            this.listener = listener;
        }

        void actualizarDatos(List<OrdenDespacho> nuevas) {
            this.items = nuevas;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_orden_despacho, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            OrdenDespacho o = items.get(position);
            h.numero.setText("📋 " + o.getNumeroOrden());
            h.estacion.setText("📍 " + o.getEstacionDestino());
            h.volumen.setText(String.format(Locale.getDefault(),
                    "⛽ %.0f L — %s", o.getVolumenLitros(), o.getTipoCombustible()));
            h.fecha.setText(fmt.format(new Date(o.getFechaCreacion())));

            boolean pendiente = "PENDIENTE".equals(o.getEstado());
            h.estado.setText(pendiente ? "⏳ PENDIENTE" : "✅ ENTREGADA");
            h.estado.setTextColor(h.itemView.getContext().getColor(
                    pendiente ? R.color.advertencia : R.color.exito));

            if (pendiente) {
                h.botonConfirmar.setVisibility(View.VISIBLE);
                h.botonConfirmar.setOnClickListener(v -> listener.confirmar(o));
            } else {
                h.botonConfirmar.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView numero, estacion, volumen, estado, fecha;
            MaterialButton botonConfirmar;
            VH(View v) {
                super(v);
                numero = v.findViewById(R.id.textoNumeroOrden);
                estacion = v.findViewById(R.id.textoEstacionOrden);
                volumen = v.findViewById(R.id.textoVolumenOrden);
                estado = v.findViewById(R.id.textoEstadoOrden);
                fecha = v.findViewById(R.id.textoFechaOrden);
                botonConfirmar = v.findViewById(R.id.botonConfirmarEntrega);
            }
        }
    }
}
