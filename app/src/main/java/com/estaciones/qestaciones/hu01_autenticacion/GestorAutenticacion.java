package com.estaciones.qestaciones.hu01_autenticacion;

import android.content.Context;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.RegistroAcceso;
import com.estaciones.qestaciones.modelo.Usuario;
import java.util.UUID;

/**
 * Gestor de autenticación con simulación OAuth2 y Auditoría (HU-01, HU-02).
 */
public class GestorAutenticacion {

    private static final int MAXIMO_INTENTOS = 3;
    private final BaseDatosEstaciones baseDatos;
    private final AuditoriaGestor auditoriaGestor;

    public GestorAutenticacion(Context contexto) {
        this.baseDatos = BaseDatosEstaciones.obtenerInstancia(contexto);
        this.auditoriaGestor = new AuditoriaGestor(contexto);
    }

    public static class ResultadoAutenticacion {
        private final boolean exitoso;
        private final String mensaje;
        private final Usuario usuario;
        private final int intentosRestantes;
        private final String token; // Simulación OAuth2

        public ResultadoAutenticacion(boolean exitoso, String mensaje, Usuario usuario, int intentosRestantes, String token) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.usuario = usuario;
            this.intentosRestantes = intentosRestantes;
            this.token = token;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public Usuario getUsuario() { return usuario; }
        public int getIntentosRestantes() { return intentosRestantes; }
        public String getToken() { return token; }
    }

    public ResultadoAutenticacion iniciarSesion(String nombreUsuario, String contrasena) {
        Usuario usuario = baseDatos.usuarioDao().buscarPorNombreUsuario(nombreUsuario);

        if (usuario == null) {
            auditoriaGestor.registrarAccion("SISTEMA", "LOGIN_FALLIDO", "AUTENTICACION", "Usuario no existe: " + nombreUsuario);
            return new ResultadoAutenticacion(false, "Credenciales incorrectas", null, -1, null);
        }

        if (usuario.isBloqueado()) {
            // Verificar si el ban temporal ya expiró
            if (usuario.getFechaBanHasta() > 0 && System.currentTimeMillis() > usuario.getFechaBanHasta()) {
                baseDatos.usuarioDao().desbloquear(usuario.getId());
                usuario = baseDatos.usuarioDao().buscarPorNombreUsuario(nombreUsuario);
            } else {
                String detalle = usuario.getFechaBanHasta() > 0
                        ? "Cuenta suspendida temporalmente"
                        : "Cuenta bloqueada por intentos fallidos";
                auditoriaGestor.registrarAccion(nombreUsuario, "LOGIN_BLOQUEADO", "AUTENTICACION", "Intento en cuenta bloqueada");
                return new ResultadoAutenticacion(false, detalle, null, 0, null);
            }
        }

        if (!usuario.getContrasena().equals(contrasena)) {
            int nuevosIntentos = usuario.getIntentosFallidos() + 1;
            baseDatos.usuarioDao().actualizarIntentos(usuario.getId(), nuevosIntentos);

            if (nuevosIntentos >= MAXIMO_INTENTOS) {
                baseDatos.usuarioDao().bloquearUsuario(usuario.getId());
                auditoriaGestor.registrarAccion(nombreUsuario, "CUENTA_BLOQUEADA", "SEGURIDAD", "Bloqueo por superar intentos");
                return new ResultadoAutenticacion(false, "Cuenta bloqueada por seguridad", null, 0, null);
            }

            auditoriaGestor.registrarAccion(nombreUsuario, "LOGIN_FALLIDO", "AUTENTICACION", "Contraseña incorrecta. Intento: " + nuevosIntentos);
            return new ResultadoAutenticacion(false, "Credenciales incorrectas", null, MAXIMO_INTENTOS - nuevosIntentos, null);
        }

        // Simulación OAuth2: Generar Token
        String token = UUID.randomUUID().toString();
        baseDatos.usuarioDao().actualizarIntentos(usuario.getId(), 0);
        auditoriaGestor.registrarAccion(nombreUsuario, "LOGIN_EXITOSO", "AUTENTICACION", "Sesión iniciada con Token OAuth2: " + token.substring(0, 8) + "...");
        
        return new ResultadoAutenticacion(true, "Bienvenido", usuario, MAXIMO_INTENTOS, token);
    }

    public ResultadoAutenticacion registrarUsuario(String nombreCompleto, String nombreUsuario, String contrasena, String rol, String adminActual) {
        return registrarUsuario(nombreCompleto, nombreUsuario, contrasena, rol, null, adminActual);
    }

    /**
     * Registro con correo. Se mantiene la sobrecarga sin correo para no romper
     * llamadas existentes.
     */
    public ResultadoAutenticacion registrarUsuario(String nombreCompleto, String nombreUsuario,
                                                    String contrasena, String rol, String correo,
                                                    String adminActual) {
        if (baseDatos.usuarioDao().contarPorNombreUsuario(nombreUsuario) > 0) {
            return new ResultadoAutenticacion(false, "El usuario ya existe", null, -1, null);
        }
        if (correo != null && !correo.isEmpty()
                && baseDatos.usuarioDao().contarPorCorreo(correo) > 0) {
            return new ResultadoAutenticacion(false, "Ese correo ya está registrado", null, -1, null);
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombreCompleto(nombreCompleto);
        nuevoUsuario.setNombreUsuario(nombreUsuario);
        nuevoUsuario.setContrasena(contrasena);
        nuevoUsuario.setRol(rol);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setBloqueado(false);
        nuevoUsuario.setIntentosFallidos(0);

        baseDatos.usuarioDao().insertar(nuevoUsuario);
        auditoriaGestor.registrarAccion(adminActual, "CREAR_USUARIO", "ADMINISTRACION", "Nuevo usuario: " + nombreUsuario + " con rol: " + rol);

        return new ResultadoAutenticacion(true, "Usuario registrado exitosamente", null, -1, null);
    }
}
