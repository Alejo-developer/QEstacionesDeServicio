package com.estaciones.qestaciones.hu11_reportes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.estaciones.qestaciones.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

/**
 * Actividad para generacion de reportes oficiales (HU-11 y HU-12).
 * - HU-11: PDF y CSV completos
 * - HU-12: CSV filtrado por decreto especifico
 */
public class ReportesActividad extends AppCompatActivity {

    private GestorReportes gestorReportes;
    private TextView textoEstado;
    private TextInputEditText campoDecreto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_reportes);

        gestorReportes = new GestorReportes(this);
        textoEstado = findViewById(R.id.textoEstadoReporte);
        campoDecreto = findViewById(R.id.campoFiltroDecreto);

        findViewById(R.id.botonGenerarPDF).setOnClickListener(v -> generarPDF());
        findViewById(R.id.botonGenerarCSV).setOnClickListener(v -> generarCSV());
        findViewById(R.id.botonReportePorDecreto).setOnClickListener(v -> generarPorDecreto());
        findViewById(R.id.botonVolverReportes).setOnClickListener(v -> finish());
    }

    private void generarPDF() {
        textoEstado.setText("Generando PDF...");
        textoEstado.setVisibility(View.VISIBLE);
        GestorReportes.ResultadoReporte r = gestorReportes.generarPDF();
        mostrarResultado(r, "application/pdf");
    }

    private void generarCSV() {
        textoEstado.setText("Generando CSV...");
        textoEstado.setVisibility(View.VISIBLE);
        GestorReportes.ResultadoReporte r = gestorReportes.generarCSV();
        mostrarResultado(r, "text/csv");
    }

    private void generarPorDecreto() {
        String decreto = campoDecreto.getText() != null ?
                campoDecreto.getText().toString().trim() : "";
        textoEstado.setText("Generando reporte por decreto...");
        textoEstado.setVisibility(View.VISIBLE);
        GestorReportes.ResultadoReporte r = gestorReportes.generarReportePorDecreto(decreto);
        mostrarResultado(r, "text/csv");
    }

    private void mostrarResultado(GestorReportes.ResultadoReporte resultado, String mimeType) {
        if (resultado.exitoso) {
            textoEstado.setText("Listo: " + (resultado.archivo != null ? resultado.archivo.getName() : ""));
            Toast.makeText(this, resultado.mensaje, Toast.LENGTH_SHORT).show();
            if (resultado.archivo != null) abrirArchivo(resultado.archivo, mimeType);
        } else {
            textoEstado.setText("Error: " + resultado.mensaje);
            Toast.makeText(this, resultado.mensaje, Toast.LENGTH_LONG).show();
        }
    }

    private void abrirArchivo(File archivo, String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Intent compartir = new Intent(Intent.ACTION_SEND);
                compartir.setType(mimeType);
                compartir.putExtra(Intent.EXTRA_STREAM, uri);
                compartir.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(compartir, "Compartir reporte"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Guardado en:\n" + archivo.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
