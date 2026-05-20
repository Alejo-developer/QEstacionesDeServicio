package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.estaciones.qestaciones.modelo.Usuario;

import java.util.List;

/**
 * DAO para operaciones de la tabla usuarios.
 * HU-01: Autenticación y control de acceso.
 * HU-02: 2FA con Correo.
 */
@Dao
public interface UsuarioDao {

    @Insert
    void insertar(Usuario usuario);

    @Update
    void actualizar(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario LIMIT 1")
    Usuario buscarPorNombreUsuario(String nombreUsuario);

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    Usuario buscarPorCorreo(String correo);

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario AND contrasena = :contrasena LIMIT 1")
    Usuario buscarPorCredenciales(String nombreUsuario, String contrasena);

    @Query("UPDATE usuarios SET intentosFallidos = :intentos WHERE id = :idUsuario")
    void actualizarIntentos(int idUsuario, int intentos);

    @Query("UPDATE usuarios SET bloqueado = 1 WHERE id = :idUsuario")
    void bloquearUsuario(int idUsuario);

    @Query("SELECT * FROM usuarios")
    List<Usuario> obtenerTodos();

    @Query("SELECT * FROM usuarios WHERE rol = :rol ORDER BY fechaCreacion DESC")
    List<Usuario> obtenerPorRol(String rol);

    @Query("UPDATE usuarios SET nombreCompleto = :nombreCompleto, correo = :correo, " +
           "telefono = :telefono, direccion = :direccion, ciudad = :ciudad, " +
           "cedula = :cedula, nit = :nit WHERE id = :id")
    void actualizarPerfil(int id, String nombreCompleto, String correo, String telefono,
                           String direccion, String ciudad, String cedula, String nit);

    /** Asigna (o reasigna) la empresa a la que pertenece un OPERADOR. */
    @Query("UPDATE usuarios SET empresaAsignada = :empresa WHERE id = :id")
    void asignarEmpresa(int id, String empresa);

    /** Lista los operadores asignados a una empresa específica (estación). */
    @Query("SELECT * FROM usuarios WHERE rol = 'OPERADOR' AND empresaAsignada = :empresa ORDER BY nombreCompleto")
    List<Usuario> obtenerOperadoresPorEmpresa(String empresa);

    /** Saldo actual del usuario (0 si no existe). */
    @Query("SELECT COALESCE(saldoCop, 0) FROM usuarios WHERE nombreUsuario = :usuario LIMIT 1")
    double obtenerSaldo(String usuario);

    /**
     * Suma `monto` al saldo de un usuario de forma atómica (sin leer-modificar-escribir
     * desde Java). Úsalo dentro de un runInTransaction junto con el otro lado de la
     * transferencia para garantizar que ambas se aplican o ninguna.
     */
    @Query("UPDATE usuarios SET saldoCop = saldoCop + :monto WHERE nombreUsuario = :usuario")
    void incrementarSaldo(String usuario, double monto);

    /**
     * Resta `monto` del saldo. SOLO se aplica si hay fondos suficientes — devuelve la
     * cantidad de filas afectadas (1 = ok, 0 = saldo insuficiente o usuario inexistente).
     * El llamador debe verificar el resultado y abortar la transacción si fue 0.
     */
    @Query("UPDATE usuarios SET saldoCop = saldoCop - :monto WHERE nombreUsuario = :usuario AND saldoCop >= :monto")
    int decrementarSaldoSiAlcanza(String usuario, double monto);

    /** Lab Maps Intent: actualiza la ubicación geográfica del usuario. */
    @Query("UPDATE usuarios SET latitud = :lat, longitud = :lng WHERE id = :id")
    void actualizarUbicacion(int id, Double lat, Double lng);

    /**
     * Lab Maps Intent: estaciones con coordenadas registradas. El distribuidor
     * arma su ruta del día seleccionando de esta lista; las que aún no tienen
     * coords quedan fuera porque no se pueden colocar en el mapa.
     */
    @Query("SELECT * FROM usuarios WHERE rol = 'ESTACION' AND latitud IS NOT NULL "
            + "AND longitud IS NOT NULL ORDER BY nombreCompleto")
    List<Usuario> obtenerEstacionesConCoordenadas();

    @Query("SELECT COUNT(*) FROM usuarios WHERE nombreUsuario = :nombreUsuario")
    int contarPorNombreUsuario(String nombreUsuario);

    @Query("SELECT COUNT(*) FROM usuarios WHERE correo = :correo")
    int contarPorCorreo(String correo);

    @Delete
    void eliminar(Usuario usuario);

    @Query("UPDATE usuarios SET bloqueado = 0, intentosFallidos = 0, fechaBanHasta = 0 WHERE id = :id")
    void desbloquear(int id);

    @Query("UPDATE usuarios SET bloqueado = 1, fechaBanHasta = :hasta WHERE id = :id")
    void banearHasta(int id, long hasta);
}
