package com.estaciones.qestaciones.hu14_usuario_particular;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Perfil editable del usuario logueado (cualquier rol).
 * Para USUARIO_PARTICULAR muestra Cédula; para ESTACION/DISTRIBUIDOR muestra NIT.
 *
 * Persiste mediante UsuarioDao.actualizarPerfil, que solo toca las columnas
 * de perfil y no altera rol, contraseña, ni estado de bloqueo.
 */
public class PerfilUsuarioActividad extends AppCompatActivity {

    private UsuarioDao usuarioDao;
    private Usuario usuarioActual;
    private String nombreUsuario = "";
    private String rolUsuario = "";

    private TextView textoUsuario;
    private TextInputEditText campoNombre, campoCorreo, campoTelefono, campoCedula, campoNit, campoDireccion, campoCiudad;
    private TextInputLayout contenedorCedula, contenedorNit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_perfil_usuario);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();

        nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        rolUsuario = getIntent().getStringExtra("rol");
        if (nombreUsuario == null) nombreUsuario = "";
        if (rolUsuario == null) rolUsuario = "";

        vincularVistas();
        cargarUsuario();
        configurarVisibilidadCampos();

        findViewById(R.id.botonGuardarPerfil).setOnClickListener(v -> guardar());
        findViewById(R.id.botonVolverPerfil).setOnClickListener(v -> finish());
    }

    private void vincularVistas() {
        textoUsuario = findViewById(R.id.textoUsuarioPerfil);
        campoNombre = findViewById(R.id.campoNombreCompletoPerfil);
        campoCorreo = findViewById(R.id.campoCorreoPerfil);
        campoTelefono = findViewById(R.id.campoTelefonoPerfil);
        campoCedula = findViewById(R.id.campoCedulaPerfil);
        campoNit = findViewById(R.id.campoNitPerfil);
        campoDireccion = findViewById(R.id.campoDireccionPerfil);
        campoCiudad = findViewById(R.id.campoCiudadPerfil);
        contenedorCedula = findViewById(R.id.contenedorCedulaPerfil);
        contenedorNit = findViewById(R.id.contenedorNitPerfil);
    }

    private void cargarUsuario() {
        if (nombreUsuario.isEmpty()) {
            Toast.makeText(this, "No se pudo identificar el usuario logueado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        usuarioActual = usuarioDao.buscarPorNombreUsuario(nombreUsuario);
        if (usuarioActual == null) {
            Toast.makeText(this, "Usuario no encontrado en la BD", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        textoUsuario.setText("@" + usuarioActual.getNombreUsuario() + " (" + usuarioActual.getRol() + ")");
        setText(campoNombre, usuarioActual.getNombreCompleto());
        setText(campoCorreo, usuarioActual.getCorreo());
        setText(campoTelefono, usuarioActual.getTelefono());
        setText(campoCedula, usuarioActual.getCedula());
        setText(campoNit, usuarioActual.getNit());
        setText(campoDireccion, usuarioActual.getDireccion());
        setText(campoCiudad, usuarioActual.getCiudad());
    }

    private void configurarVisibilidadCampos() {
        // Mostrar NIT para empresas, cédula para personas; por defecto mostramos cédula
        boolean esEmpresa = "ESTACION".equals(rolUsuario) || "DISTRIBUIDOR".equals(rolUsuario);
        contenedorNit.setVisibility(esEmpresa ? View.VISIBLE : View.GONE);
        contenedorCedula.setVisibility(esEmpresa ? View.GONE : View.VISIBLE);
    }

    private void guardar() {
        String nombre = leer(campoNombre);
        String correo = leer(campoCorreo);
        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Ingrese un correo válido", Toast.LENGTH_SHORT).show();
            return;
        }
        // Validar que el correo, si cambió, no esté ya en uso por otro usuario
        if (!correo.equalsIgnoreCase(usuarioActual.getCorreo() == null ? "" : usuarioActual.getCorreo())) {
            Usuario existente = usuarioDao.buscarPorCorreo(correo);
            if (existente != null && existente.getId() != usuarioActual.getId()) {
                Toast.makeText(this, "Ese correo ya está registrado para otro usuario", Toast.LENGTH_LONG).show();
                return;
            }
        }

        usuarioDao.actualizarPerfil(
                usuarioActual.getId(),
                nombre,
                correo,
                nullSiVacio(leer(campoTelefono)),
                nullSiVacio(leer(campoDireccion)),
                nullSiVacio(leer(campoCiudad)),
                nullSiVacio(leer(campoCedula)),
                nullSiVacio(leer(campoNit))
        );

        Toast.makeText(this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String leer(TextInputEditText c) {
        return c.getText() != null ? c.getText().toString().trim() : "";
    }

    private static String nullSiVacio(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static void setText(TextInputEditText c, String valor) {
        c.setText(valor == null ? "" : valor);
    }
}
