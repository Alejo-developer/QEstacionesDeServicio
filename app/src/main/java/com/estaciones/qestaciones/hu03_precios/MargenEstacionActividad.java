package com.estaciones.qestaciones.hu03_precios;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.MargenEstacionDao;
import com.estaciones.qestaciones.datos.PrecioReferenciaDao;
import com.estaciones.qestaciones.modelo.MargenEstacion;
import com.estaciones.qestaciones.modelo.PrecioReferencia;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * La ESTACIÓN configura su margen de ganancia sobre el precio de referencia del regulador.
 * precioVenta = precioReferencia + margen (margen puede ser negativo).
 */
public class MargenEstacionActividad extends AppCompatActivity {

    private static final String[] COMBUSTIBLES = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private PrecioReferenciaDao precioDao;
    private MargenEstacionDao margenDao;

    private TextInputEditText campoNombreEstacion;
    private TextInputEditText campoMargenAcpm, campoMargenCorriente, campoMargenExtra;
    private TextView textoRefAcpm, textoRefCorriente, textoRefExtra;
    private TextView textoPrecioVentaAcpm, textoPrecioVentaCorriente, textoPrecioVentaExtra;

    private double refAcpm = 0, refCorriente = 0, refExtra = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_margen_estacion);

        precioDao = BaseDatosEstaciones.obtenerInstancia(this).precioReferenciaDao();
        margenDao = BaseDatosEstaciones.obtenerInstancia(this).margenEstacionDao();

        vincularVistas();
        cargarPreciosReferencia();
        configurarWatchers();

        findViewById(R.id.botonGuardarMargenes).setOnClickListener(v -> guardarMargenes());
        findViewById(R.id.botonVolverMargen).setOnClickListener(v -> finish());
    }

    private void vincularVistas() {
        campoNombreEstacion = findViewById(R.id.campoNombreEstacion);
        campoMargenAcpm = findViewById(R.id.campoMargenAcpm);
        campoMargenCorriente = findViewById(R.id.campoMargenCorriente);
        campoMargenExtra = findViewById(R.id.campoMargenExtra);
        textoRefAcpm = findViewById(R.id.textoRefAcpm);
        textoRefCorriente = findViewById(R.id.textoRefCorriente);
        textoRefExtra = findViewById(R.id.textoRefExtra);
        textoPrecioVentaAcpm = findViewById(R.id.textoPrecioVentaAcpm);
        textoPrecioVentaCorriente = findViewById(R.id.textoPrecioVentaCorriente);
        textoPrecioVentaExtra = findViewById(R.id.textoPrecioVentaExtra);
    }

    private void cargarPreciosReferencia() {
        PrecioReferencia pAcpm = precioDao.obtenerUltimoPorTipo("ACPM");
        PrecioReferencia pCorriente = precioDao.obtenerUltimoPorTipo("Gasolina Corriente");
        PrecioReferencia pExtra = precioDao.obtenerUltimoPorTipo("Gasolina Extra");

        refAcpm = pAcpm != null ? pAcpm.getPrecioPorLitro() : 0;
        refCorriente = pCorriente != null ? pCorriente.getPrecioPorLitro() : 0;
        refExtra = pExtra != null ? pExtra.getPrecioPorLitro() : 0;

        textoRefAcpm.setText(refAcpm > 0 ? "$" + FMT.format(refAcpm) + "/L" : "No fijado");
        textoRefCorriente.setText(refCorriente > 0 ? "$" + FMT.format(refCorriente) + "/L" : "No fijado");
        textoRefExtra.setText(refExtra > 0 ? "$" + FMT.format(refExtra) + "/L" : "No fijado");

        actualizarPrecioVenta(textoPrecioVentaAcpm, refAcpm, 0);
        actualizarPrecioVenta(textoPrecioVentaCorriente, refCorriente, 0);
        actualizarPrecioVenta(textoPrecioVentaExtra, refExtra, 0);
    }

    private void configurarWatchers() {
        campoMargenAcpm.addTextChangedListener(crearWatcher(textoPrecioVentaAcpm, () -> refAcpm));
        campoMargenCorriente.addTextChangedListener(crearWatcher(textoPrecioVentaCorriente, () -> refCorriente));
        campoMargenExtra.addTextChangedListener(crearWatcher(textoPrecioVentaExtra, () -> refExtra));
    }

    private TextWatcher crearWatcher(TextView textoPrecioVenta, RefProvider refProvider) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                double margen = parseMargen(s.toString());
                actualizarPrecioVenta(textoPrecioVenta, refProvider.get(), margen);
            }
        };
    }

    private interface RefProvider { double get(); }

    private void actualizarPrecioVenta(TextView vista, double ref, double margen) {
        if (ref <= 0) {
            vista.setText("$—");
            return;
        }
        double venta = ref + margen;
        vista.setText("$" + FMT.format(venta) + "/L");
        vista.setTextColor(getColor(venta >= ref ? R.color.exito : R.color.advertencia));
    }

    private double parseMargen(String texto) {
        if (texto == null || texto.trim().isEmpty() || texto.trim().equals("-")) return 0;
        try { return Double.parseDouble(texto.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private void guardarMargenes() {
        String estacion = campoNombreEstacion.getText() != null
                ? campoNombreEstacion.getText().toString().trim() : "";
        if (estacion.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre de la estación", Toast.LENGTH_SHORT).show();
            return;
        }
        if (refAcpm <= 0 && refCorriente <= 0 && refExtra <= 0) {
            Toast.makeText(this, "El regulador aún no ha fijado precios de referencia", Toast.LENGTH_LONG).show();
            return;
        }

        guardarMargenIndividual(estacion, "ACPM", refAcpm, parseMargen(
                campoMargenAcpm.getText() != null ? campoMargenAcpm.getText().toString() : "0"));
        guardarMargenIndividual(estacion, "Gasolina Corriente", refCorriente, parseMargen(
                campoMargenCorriente.getText() != null ? campoMargenCorriente.getText().toString() : "0"));
        guardarMargenIndividual(estacion, "Gasolina Extra", refExtra, parseMargen(
                campoMargenExtra.getText() != null ? campoMargenExtra.getText().toString() : "0"));

        Toast.makeText(this, "Precios de venta guardados para " + estacion, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void guardarMargenIndividual(String estacion, String tipo, double ref, double margen) {
        if (ref <= 0) return;
        MargenEstacion m = new MargenEstacion();
        m.setNombreEstacion(estacion);
        m.setTipoCombustible(tipo);
        m.setMargenPesos(margen);
        m.setPrecioReferencia(ref);
        m.setPrecioVenta(ref + margen);
        m.setActualizadoPor("estacion");
        margenDao.insertar(m);
    }
}
