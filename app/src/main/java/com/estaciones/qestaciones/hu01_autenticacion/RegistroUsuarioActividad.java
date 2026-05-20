package com.estaciones.qestaciones.hu01_autenticacion;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.Rol;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;
import java.util.Random;

/**
 * Actividad de registro de nuevos usuarios con 2FA (HU-02).
 * Integra envío de correo real vía SMTP (ServidorCorreo).
 * Si el SMTP no está configurado, usa simulación como fallback.
 *
 * Muestra campos adicionales según el rol seleccionado:
 * - ESTACION/DISTRIBUIDOR: NIT, Ciudad (datos de empresa)
 * - USUARIO_PARTICULAR: Cédula, Teléfono, Dirección, Ciudad
 */
public class RegistroUsuarioActividad extends AppCompatActivity {

    private TextInputEditText campoNombreCompleto, campoCorreo, campoUsuario, campoContrasena, campoConfirmarContrasena, campoCodigo;
    private TextInputEditText campoNit, campoCiudadEmpresa;
    private TextInputEditText campoCedula, campoTelefono, campoDireccion, campoCiudadParticular;
    private View panelCamposEmpresa, panelCamposParticular, panelCamposOperador;
    private Spinner selectorRol;
    private Spinner spinnerEstacionOperador;
    private TextView textoAvisoSinEstaciones;
    private java.util.List<Usuario> estacionesDisponibles = new java.util.ArrayList<>();
    private TextView textoError;
    private View layoutFormulario, layoutVerificacion;
    private String codigoGenerado;
    private Usuario usuarioTemporal;

    private GestorAutenticacion gestorAutenticacion;
    private ServidorCorreo servidorCorreo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_registro_usuario);

        gestorAutenticacion = new GestorAutenticacion(this);
        servidorCorreo = new ServidorCorreo();

        inicializarVistas();
        configurarSelectorRol();
        configurarEventos();
    }

    private void inicializarVistas() {
        campoNombreCompleto = findViewById(R.id.campoNombreCompleto);
        campoCorreo = findViewById(R.id.campoCorreo);
        campoUsuario = findViewById(R.id.campoUsuario);
        campoContrasena = findViewById(R.id.campoContrasena);
        campoConfirmarContrasena = findViewById(R.id.campoConfirmarContrasena);
        campoCodigo = findViewById(R.id.campoCodigoVerificacion);
        selectorRol = findViewById(R.id.selectorRol);
        textoError = findViewById(R.id.textoError);
        layoutFormulario = findViewById(R.id.layoutFormulario);
        layoutVerificacion = findViewById(R.id.layoutVerificacion);

        // Campos dinámicos por rol
        panelCamposEmpresa = findViewById(R.id.panelCamposEmpresa);
        panelCamposParticular = findViewById(R.id.panelCamposParticular);
        panelCamposOperador = findViewById(R.id.panelCamposOperador);
        campoNit = findViewById(R.id.campoNit);
        campoCiudadEmpresa = findViewById(R.id.campoCiudadEmpresa);
        campoCedula = findViewById(R.id.campoCedula);
        campoTelefono = findViewById(R.id.campoTelefono);
        campoDireccion = findViewById(R.id.campoDireccion);
        campoCiudadParticular = findViewById(R.id.campoCiudadParticular);
        spinnerEstacionOperador = findViewById(R.id.spinnerEstacionOperador);
        textoAvisoSinEstaciones = findViewById(R.id.textoAvisoSinEstaciones);

        findViewById(R.id.enlaceVolverLogin).setOnClickListener(v -> finish());
    }

    private void poblarSpinnerEstaciones() {
        UsuarioDao dao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();
        estacionesDisponibles = dao.obtenerPorRol("ESTACION");
        java.util.List<String> etiquetas = new java.util.ArrayList<>();
        for (Usuario u : estacionesDisponibles) {
            String nombre = u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getNombreUsuario();
            etiquetas.add(nombre + " (@" + u.getNombreUsuario() + ")");
        }
        if (etiquetas.isEmpty()) {
            etiquetas.add("(sin estaciones)");
            textoAvisoSinEstaciones.setVisibility(View.VISIBLE);
            spinnerEstacionOperador.setEnabled(false);
        } else {
            textoAvisoSinEstaciones.setVisibility(View.GONE);
            spinnerEstacionOperador.setEnabled(true);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, etiquetas);
        spinnerEstacionOperador.setAdapter(adapter);
    }

    private void configurarSelectorRol() {
        // Obtenemos todos los roles definidos en el enum Rol
        Rol[] rolesEnum = Rol.values();
        String[] descripciones = new String[rolesEnum.length];
        for (int i = 0; i < rolesEnum.length; i++) {
            descripciones[i] = rolesEnum[i].getDescripcion();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, descripciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectorRol.setAdapter(adapter);

        // Listener para mostrar/ocultar campos según rol
        selectorRol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Rol rolSeleccionado = Rol.values()[position];
                actualizarCamposPorRol(rolSeleccionado);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                panelCamposEmpresa.setVisibility(View.GONE);
                panelCamposParticular.setVisibility(View.GONE);
            }
        });
    }

    private void actualizarCamposPorRol(Rol rol) {
        switch (rol) {
            case ESTACION:
            case DISTRIBUIDOR:
                panelCamposEmpresa.setVisibility(View.VISIBLE);
                panelCamposParticular.setVisibility(View.GONE);
                panelCamposOperador.setVisibility(View.GONE);
                break;
            case USUARIO_PARTICULAR:
                panelCamposEmpresa.setVisibility(View.GONE);
                panelCamposParticular.setVisibility(View.VISIBLE);
                panelCamposOperador.setVisibility(View.GONE);
                break;
            case OPERADOR:
                panelCamposEmpresa.setVisibility(View.GONE);
                panelCamposParticular.setVisibility(View.GONE);
                panelCamposOperador.setVisibility(View.VISIBLE);
                poblarSpinnerEstaciones();
                break;
            default:
                panelCamposEmpresa.setVisibility(View.GONE);
                panelCamposParticular.setVisibility(View.GONE);
                panelCamposOperador.setVisibility(View.GONE);
                break;
        }
    }

    private void configurarEventos() {
        findViewById(R.id.botonRegistrar).setOnClickListener(v -> iniciarVerificacion());
        findViewById(R.id.botonValidarCodigo).setOnClickListener(v -> completarRegistro());
        findViewById(R.id.botonCancelarVerificacion).setOnClickListener(v -> {
            layoutVerificacion.setVisibility(View.GONE);
            layoutFormulario.setVisibility(View.VISIBLE);
        });
    }

    private void iniciarVerificacion() {
        String nombre = obtenerTexto(campoNombreCompleto);
        String correo = obtenerTexto(campoCorreo);
        String user = obtenerTexto(campoUsuario);
        String pass = obtenerTexto(campoContrasena);
        String confirm = obtenerTexto(campoConfirmarContrasena);

        if (nombre.isEmpty() || correo.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            mostrarError("Todos los campos son obligatorios");
            return;
        }

        if (!pass.equals(confirm)) {
            mostrarError("Las contraseñas no coinciden");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarError("Ingrese un correo electrónico válido");
            return;
        }

        // Generar código de 6 dígitos
        codigoGenerado = String.format(Locale.getDefault(), "%06d", new Random().nextInt(999999));

        // Guardar datos temporales
        usuarioTemporal = new Usuario();
        usuarioTemporal.setNombreCompleto(nombre);
        usuarioTemporal.setCorreo(correo);
        usuarioTemporal.setNombreUsuario(user);
        usuarioTemporal.setContrasena(pass);

        // Asignar rol seleccionado
        Rol rolSeleccionado = Rol.values()[selectorRol.getSelectedItemPosition()];
        usuarioTemporal.setRol(rolSeleccionado.name());

        // Guardar campos extra según rol
        switch (rolSeleccionado) {
            case ESTACION:
            case DISTRIBUIDOR:
                usuarioTemporal.setNit(obtenerTexto(campoNit));
                usuarioTemporal.setCiudad(obtenerTexto(campoCiudadEmpresa));
                break;
            case USUARIO_PARTICULAR:
                usuarioTemporal.setCedula(obtenerTexto(campoCedula));
                usuarioTemporal.setTelefono(obtenerTexto(campoTelefono));
                usuarioTemporal.setDireccion(obtenerTexto(campoDireccion));
                usuarioTemporal.setCiudad(obtenerTexto(campoCiudadParticular));
                break;
            case OPERADOR:
                // El operador debe estar asignado a una estación para poder vender
                if (estacionesDisponibles.isEmpty()) {
                    mostrarError("No hay estaciones registradas. Registra primero una estación antes de crear un operador.");
                    return;
                }
                int pos = spinnerEstacionOperador.getSelectedItemPosition();
                if (pos < 0 || pos >= estacionesDisponibles.size()) {
                    mostrarError("Selecciona la estación en la que trabajas");
                    return;
                }
                usuarioTemporal.setEmpresaAsignada(
                        estacionesDisponibles.get(pos).getNombreUsuario());
                break;
        }

        // Cambiar a vista de verificación
        layoutFormulario.setVisibility(View.GONE);
        layoutVerificacion.setVisibility(View.VISIBLE);
        textoError.setVisibility(View.GONE);

        // ── HU-02: Envío real por SMTP si está configurado ──
        if (ServidorCorreo.estaConfigurado()) {
            Toast.makeText(this, "Enviando código a " + correo + "...", Toast.LENGTH_SHORT).show();
            servidorCorreo.enviarCodigoVerificacion(correo, codigoGenerado, new ServidorCorreo.ResultadoEnvio() {
                @Override
                public void onExito() {
                    Toast.makeText(RegistroUsuarioActividad.this,
                            "✅ Código enviado a " + correo, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String mensajeError) {
                    // Si falla SMTP, mostrar simulación como fallback
                    Toast.makeText(RegistroUsuarioActividad.this,
                            "⚠ No se pudo enviar el correo. Usando simulación.", Toast.LENGTH_LONG).show();
                    mostrarSimulacionCorreo(correo);
                }
            });
        } else {
            // SMTP no configurado: usar simulación
            mostrarSimulacionCorreo(correo);
        }
    }

    /**
     * Muestra un diálogo simulando la bandeja de entrada (fallback cuando SMTP no está configurado).
     */
    private void mostrarSimulacionCorreo(String correo) {
        new AlertDialog.Builder(this)
            .setTitle("📩 Bandeja de Entrada (Simulada)")
            .setMessage("De: seguridad@qestaciones.com\nPara: " + correo
                    + "\n\nAsunto: Código de Verificación\n\nTu código de seguridad para registrarte es: " + codigoGenerado
                    + "\n\n⚠ SMTP no configurado. Para envío real, configure ServidorCorreo.java con credenciales Gmail.")
            .setPositiveButton("Usar Código", (dialog, which) -> {
                campoCodigo.setText(codigoGenerado);
                Toast.makeText(this, "Código autocompletado", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cerrar", null)
            .show();
    }

    private void completarRegistro() {
        String codigoIngresado = obtenerTexto(campoCodigo);

        if (codigoIngresado.equals(codigoGenerado)) {
            GestorAutenticacion.ResultadoAutenticacion resultado = gestorAutenticacion.registrarUsuario(
                    usuarioTemporal.getNombreCompleto(),
                    usuarioTemporal.getNombreUsuario(),
                    usuarioTemporal.getContrasena(),
                    usuarioTemporal.getRol(),
                    usuarioTemporal.getCorreo(),
                    "AUTOREGISTRO_2FA"
            );

            if (resultado.isExitoso()) {
                // Guardar campos extra del perfil (NIT, cédula, teléfono, etc.)
                guardarCamposExtraPerfil();
                Toast.makeText(this, "¡Cuenta verificada y creada!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                mostrarError(resultado.getMensaje());
            }
        } else {
            mostrarError("El código de verificación no es válido");
        }
    }

    /**
     * Después del registro exitoso, actualiza los campos extra del perfil
     * (NIT, cédula, teléfono, dirección, ciudad) en la base de datos.
     */
    private void guardarCamposExtraPerfil() {
        UsuarioDao dao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();
        Usuario creado = dao.buscarPorNombreUsuario(usuarioTemporal.getNombreUsuario());
        if (creado != null) {
            // Usar usuarioTemporal para todos los valores: es la fuente de verdad del formulario
            // (la BD podría tener null en campos que el usuario sí rellenó)
            dao.actualizarPerfil(
                    creado.getId(),
                    usuarioTemporal.getNombreCompleto(),
                    usuarioTemporal.getCorreo(),
                    usuarioTemporal.getTelefono(),
                    usuarioTemporal.getDireccion(),
                    usuarioTemporal.getCiudad(),
                    usuarioTemporal.getCedula(),
                    usuarioTemporal.getNit()
            );
            if (usuarioTemporal.getEmpresaAsignada() != null
                    && !usuarioTemporal.getEmpresaAsignada().isEmpty()) {
                dao.asignarEmpresa(creado.getId(), usuarioTemporal.getEmpresaAsignada());
            }
        }
    }

    private String obtenerTexto(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }

    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }
}
