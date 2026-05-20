package com.estaciones.qestaciones.hu01_autenticacion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.PrincipalActividad;
import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.hu04_consulta.ConsultaPreciosActividad;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Actividad de inicio de sesión (HU-01).
 */
public class InicioSesionActividad extends AppCompatActivity {

    private TextInputEditText campoUsuario;
    private TextInputEditText campoContrasena;
    private TextView textoError;
    private MaterialButton botonIngresar;
    private MaterialButton enlaceRegistro;
    private MaterialButton botonConsultaPublica;

    private GestorAutenticacion gestorAutenticacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_inicio_sesion);

        gestorAutenticacion = new GestorAutenticacion(this);

        inicializarVistas();
        configurarEventos();
    }

    private void inicializarVistas() {
        campoUsuario = findViewById(R.id.campoUsuario);
        campoContrasena = findViewById(R.id.campoContrasena);
        textoError = findViewById(R.id.textoError);
        botonIngresar = findViewById(R.id.botonIngresar);
        enlaceRegistro = findViewById(R.id.enlaceRegistrarse);
        botonConsultaPublica = findViewById(R.id.botonConsultaPublica);
    }

    private void configurarEventos() {
        botonIngresar.setOnClickListener(v -> intentarInicioSesion());

        enlaceRegistro.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroUsuarioActividad.class));
        });

        // HU-04: Consulta pública sin necesidad de Login
        botonConsultaPublica.setOnClickListener(v -> {
            startActivity(new Intent(this, ConsultaPreciosActividad.class));
        });
    }

    private void intentarInicioSesion() {
        String usuario = campoUsuario.getText() != null ? campoUsuario.getText().toString().trim() : "";
        String contrasena = campoContrasena.getText() != null ? campoContrasena.getText().toString().trim() : "";

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError(getString(R.string.error_campos_vacios));
            return;
        }

        GestorAutenticacion.ResultadoAutenticacion resultado = gestorAutenticacion.iniciarSesion(usuario, contrasena);

        if (resultado.isExitoso()) {
            Intent intento = new Intent(this, PrincipalActividad.class);
            intento.putExtra("nombreUsuario", resultado.getUsuario().getNombreUsuario());
            intento.putExtra("nombreCompleto", resultado.getUsuario().getNombreCompleto());
            intento.putExtra("rol", resultado.getUsuario().getRol());
            startActivity(intento);
            finish();
        } else {
            mostrarError(resultado.getMensaje());
        }
    }

    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }
}
