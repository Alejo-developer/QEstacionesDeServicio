package com.estaciones.qestaciones.datos;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.estaciones.qestaciones.modelo.Auditoria;
import com.estaciones.qestaciones.modelo.InventarioDistribuidor;
import com.estaciones.qestaciones.modelo.MargenEstacion;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.estaciones.qestaciones.modelo.MovimientoSaldo;
import com.estaciones.qestaciones.modelo.OrdenDespacho;
import com.estaciones.qestaciones.modelo.PrecioReferencia;
import com.estaciones.qestaciones.modelo.RegistroAcceso;
import com.estaciones.qestaciones.modelo.RegistroEntrega;
import com.estaciones.qestaciones.modelo.ReglaPrecios;
import com.estaciones.qestaciones.modelo.Transaccion;
import com.estaciones.qestaciones.modelo.Usuario;
import com.estaciones.qestaciones.modelo.ValidacionSubsidio;
import com.estaciones.qestaciones.modelo.ZonaRegulada;

/**
 * Base de datos principal de la aplicación usando Room.
 *
 * Historial de versiones:
 *  - v8: fechaBanHasta en Usuario (ban temporal administrador)
 *  - v9: Perfil de usuario/empresa (nit, cedula, telefono, direccion, ciudad) en Usuario.
 *        Scope por empresa en MovimientoInventario (empresaUsuario).
 *        Datos de vendedor/comprador en Transaccion (empresaVendedoraUsuario,
 *        tipoVendedor, compradorUsuario, correoCliente).
 *  - v10: empresaAsignada en Usuario para vincular al OPERADOR con una estación.
 *  - v11: saldoCop en Usuario + tabla movimientos_saldo (historial de pagos/recargas).
 *  - v12: latitud/longitud en Usuario (Lab Maps Intent — ubicación de estaciones
 *         para que el distribuidor arme la ruta diaria con waypoints en Google Maps).
 *
 * Política: NUNCA usar fallbackToDestructiveMigration. Cada cambio de esquema
 * debe venir con una Migration explícita que preserve los datos existentes.
 */
@Database(
    entities = {
        Usuario.class,
        RegistroAcceso.class,
        ReglaPrecios.class,
        ValidacionSubsidio.class,
        ZonaRegulada.class,
        RegistroEntrega.class,
        MovimientoInventario.class,
        Auditoria.class,
        PrecioReferencia.class,
        MargenEstacion.class,
        Transaccion.class,
        InventarioDistribuidor.class,
        OrdenDespacho.class,
        MovimientoSaldo.class
    },
    version = 12,
    exportSchema = false
)
public abstract class BaseDatosEstaciones extends RoomDatabase {

    private static volatile BaseDatosEstaciones instancia;

    public abstract UsuarioDao usuarioDao();
    public abstract RegistroAccesoDao registroAccesoDao();
    public abstract ReglaPreciosDao reglaPreciosDao();
    public abstract ValidacionSubsidioDao validacionSubsidioDao();
    public abstract ZonaReguladaDao zonaReguladaDao();
    public abstract RegistroEntregaDao registroEntregaDao();
    public abstract MovimientoInventarioDao movimientoInventarioDao();
    public abstract AuditoriaDao auditoriaDao();
    public abstract PrecioReferenciaDao precioReferenciaDao();
    public abstract MargenEstacionDao margenEstacionDao();
    public abstract TransaccionDao transaccionDao();
    public abstract InventarioDistribuidorDao inventarioDistribuidorDao();
    public abstract OrdenDespachoDao ordenDespachoDao();
    public abstract MovimientoSaldoDao movimientoSaldoDao();

    /**
     * Migración 8 → 9: añade columnas nuevas sin tocar los datos existentes.
     * Todas las columnas son TEXT nullable, por lo que las filas previas
     * quedan con NULL automáticamente (usuarios, movimientos, transacciones).
     */
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Perfil/empresa en Usuario
            db.execSQL("ALTER TABLE usuarios ADD COLUMN nit TEXT");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN cedula TEXT");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN telefono TEXT");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN direccion TEXT");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN ciudad TEXT");

            // Scope por empresa para aislar el inventario de cada estación
            db.execSQL("ALTER TABLE movimientos_inventario ADD COLUMN empresaUsuario TEXT");

            // Datos de vendedor/comprador para ventas a USUARIO_PARTICULAR
            db.execSQL("ALTER TABLE transacciones ADD COLUMN empresaVendedoraUsuario TEXT");
            db.execSQL("ALTER TABLE transacciones ADD COLUMN tipoVendedor TEXT");
            db.execSQL("ALTER TABLE transacciones ADD COLUMN compradorUsuario TEXT");
            db.execSQL("ALTER TABLE transacciones ADD COLUMN correoCliente TEXT");
        }
    };

    /**
     * Migración 9 → 10: OPERADOR puede estar asignado a una estación.
     * empresaAsignada guarda el nombreUsuario de la estación para la que trabaja.
     * Nullable: usuarios existentes quedan sin asignación (el admin la puede poner después).
     */
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN empresaAsignada TEXT");
        }
    };

    /**
     * Migración 10 → 11: saldo digital por usuario + tabla de historial de movimientos.
     * saldoCop arranca en 0 para todos los usuarios existentes.
     * movimientos_saldo registra recargas, pagos y cobros (cliente↔empresa, estación→operador).
     */
    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN saldoCop REAL NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS movimientos_saldo ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "usuarioNombre TEXT, "
                    + "tipo TEXT, "
                    + "monto REAL NOT NULL, "
                    + "saldoAnterior REAL NOT NULL, "
                    + "saldoNuevo REAL NOT NULL, "
                    + "concepto TEXT, "
                    + "contraparte TEXT, "
                    + "metodoPago TEXT, "
                    + "tarjetaUltimos4 TEXT, "
                    + "fecha INTEGER NOT NULL"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_movimientos_saldo_usuario "
                    + "ON movimientos_saldo(usuarioNombre)");
        }
    };

    /**
     * Migración 11 → 12: ubicación geográfica del usuario (Lab Maps Intent).
     * latitud y longitud son REAL nullables. Las filas previas quedan en NULL —
     * cada usuario tendrá que registrar su ubicación (la estación desde su perfil,
     * con el botón "Usar mi ubicación actual" del GPS).
     *
     * Para no romper pruebas, este UPDATE deja sembradas las coords reales de
     * Bogotá para los usuarios de prueba si ya existían. Si no existen aún, el
     * seed posterior los crea con coords ya puestas.
     */
    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN latitud REAL");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN longitud REAL");

            // Coords reales de Bogotá para los usuarios de prueba ya existentes.
            db.execSQL("UPDATE usuarios SET latitud = 4.6447, longitud = -74.0742 "
                    + "WHERE nombreUsuario = 'estacion1'");
            db.execSQL("UPDATE usuarios SET latitud = 4.6097, longitud = -74.0817 "
                    + "WHERE nombreUsuario = 'distribuidor1'");
            db.execSQL("UPDATE usuarios SET latitud = 4.6533, longitud = -74.0836 "
                    + "WHERE nombreUsuario = 'cliente1'");
        }
    };

    public static BaseDatosEstaciones obtenerInstancia(Context contexto) {
        if (instancia == null) {
            synchronized (BaseDatosEstaciones.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                            contexto.getApplicationContext(),
                            BaseDatosEstaciones.class,
                            "estaciones_servicio_db"
                    )
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build();

                    crearUsuarioAdminPorDefecto(instancia);
                    crearUsuariosDePruebaSiNoExisten(instancia);
                }
            }
        }
        return instancia;
    }

    private static void crearUsuarioAdminPorDefecto(BaseDatosEstaciones db) {
        if (db.usuarioDao().contarPorNombreUsuario("admin") == 0) {
            Usuario admin = new Usuario();
            admin.setNombreUsuario("admin");
            admin.setContrasena("admin123");
            admin.setNombreCompleto("Administrador del Sistema");
            admin.setCorreo("admin@estaciones.com");
            admin.setRol("ADMINISTRADOR");
            admin.setBloqueado(false);
            admin.setIntentosFallidos(0);
            db.usuarioDao().insertar(admin);
        }
    }

    /**
     * Seed de usuarios fijos para pruebas (uno por rol). Idempotente:
     * si el username o correo ya existe, ese usuario NO se toca (respeta lo que
     * el usuario haya creado manualmente). Todos comparten contraseña "admin123".
     *
     * Orden importante: ESTACION antes que OPERADOR (operador1 queda asignado a estacion1).
     */
    private static void crearUsuariosDePruebaSiNoExisten(BaseDatosEstaciones db) {
        UsuarioDao dao = db.usuarioDao();

        // ESTACION — empresa con su propio inventario (centro de Bogotá)
        crearSiFalta(dao, "estacion1", "estacion1@estaciones.com", u -> {
            u.setNombreCompleto("Estación Central");
            u.setRol("ESTACION");
            u.setNit("900111222-3");
            u.setCiudad("Bogotá");
            u.setDireccion("Av. Caracas #50-10");
            u.setTelefono("3001110000");
            u.setLatitud(4.6447);
            u.setLongitud(-74.0742);
        });

        // ESTACION extra — norte de Bogotá (para que la ruta del distribuidor tenga waypoints)
        crearSiFalta(dao, "estacion_norte", "estacion_norte@estaciones.com", u -> {
            u.setNombreCompleto("Estación Norte");
            u.setRol("ESTACION");
            u.setNit("900111223-3");
            u.setCiudad("Bogotá");
            u.setDireccion("Calle 100 #15-20");
            u.setTelefono("3001110001");
            u.setLatitud(4.6850);
            u.setLongitud(-74.0500);
        });

        // ESTACION extra — sur de Bogotá
        crearSiFalta(dao, "estacion_sur", "estacion_sur@estaciones.com", u -> {
            u.setNombreCompleto("Estación Sur");
            u.setRol("ESTACION");
            u.setNit("900111224-3");
            u.setCiudad("Bogotá");
            u.setDireccion("Av. Caracas #30 Sur");
            u.setTelefono("3001110002");
            u.setLatitud(4.5800);
            u.setLongitud(-74.1000);
        });

        // DISTRIBUIDOR — empresa con su propio stock al por mayor (Bogotá centro-occidente)
        crearSiFalta(dao, "distribuidor1", "distribuidor1@estaciones.com", u -> {
            u.setNombreCompleto("Distribuidora Andes");
            u.setRol("DISTRIBUIDOR");
            u.setNit("900333444-5");
            u.setCiudad("Bogotá");
            u.setDireccion("Calle 50 #20-30");
            u.setTelefono("3002220000");
            u.setLatitud(4.6097);
            u.setLongitud(-74.0817);
        });

        // REGULADOR — autoridad que valida placas y configura precios de referencia
        crearSiFalta(dao, "regulador1", "regulador1@estaciones.com", u -> {
            u.setNombreCompleto("Autoridad Reguladora Nacional");
            u.setRol("REGULADOR");
            u.setTelefono("3005550000");
            u.setCiudad("Bogotá");
        });

        // OPERADOR — trabaja para estacion1; vende usando su inventario
        crearSiFalta(dao, "operador1", "operador1@estaciones.com", u -> {
            u.setNombreCompleto("Juan Operador");
            u.setRol("OPERADOR");
            u.setTelefono("3003330000");
            u.setEmpresaAsignada("estacion1");
        });

        // USUARIO_PARTICULAR — cliente que compra a estaciones y distribuidores
        crearSiFalta(dao, "cliente1", "cliente1@estaciones.com", u -> {
            u.setNombreCompleto("María Cliente");
            u.setRol("USUARIO_PARTICULAR");
            u.setCedula("1023456789");
            u.setTelefono("3004440000");
            u.setDireccion("Carrera 7 #80-20");
            u.setCiudad("Bogotá");
            u.setLatitud(4.6533);
            u.setLongitud(-74.0836);
        });
    }

    /**
     * Excepción interna para abortar la transacción de transferirSaldo cuando
     * el comprador no tiene fondos. No se propaga al llamador: se traduce a `false`.
     */
    private static class SaldoInsuficienteException extends RuntimeException {}

    /**
     * Recarga simulada de saldo. Suma `monto` al usuario y registra un MovimientoSaldo
     * de tipo RECARGA. Atómico: si una operación falla, ninguna se aplica.
     *
     * NO recibe el número completo de tarjeta — solo método y últimos 4 dígitos
     * para no persistir datos sensibles.
     */
    public void recargarSaldo(String usuario, double monto, String metodoPago, String ultimos4) {
        runInTransaction(() -> {
            double antes = usuarioDao().obtenerSaldo(usuario);
            usuarioDao().incrementarSaldo(usuario, monto);

            MovimientoSaldo m = new MovimientoSaldo();
            m.setUsuarioNombre(usuario);
            m.setTipo("RECARGA");
            m.setMonto(monto);
            m.setSaldoAnterior(antes);
            m.setSaldoNuevo(antes + monto);
            m.setConcepto("Recarga de saldo");
            m.setMetodoPago(metodoPago);
            m.setTarjetaUltimos4(ultimos4);
            movimientoSaldoDao().insertar(m);
        });
    }

    /**
     * Transfiere `monto` del comprador al vendedor en una sola transacción.
     * - Resta del comprador SOLO si hay fondos (decrementarSaldoSiAlcanza).
     * - Suma al vendedor.
     * - Registra dos MovimientoSaldo (PAGO + INGRESO) con la misma contraparte.
     *
     * @return true si la transferencia se completó; false si el comprador no tenía
     *         saldo suficiente (en ese caso ningún cambio quedó persistido).
     */
    public boolean transferirSaldo(String comprador, String vendedor, double monto,
                                    String concepto, String metodoPago) {
        try {
            runInTransaction(() -> {
                double saldoCompradorAntes = usuarioDao().obtenerSaldo(comprador);
                double saldoVendedorAntes = usuarioDao().obtenerSaldo(vendedor);

                int filas = usuarioDao().decrementarSaldoSiAlcanza(comprador, monto);
                if (filas == 0) {
                    throw new SaldoInsuficienteException();
                }
                usuarioDao().incrementarSaldo(vendedor, monto);

                MovimientoSaldo pago = new MovimientoSaldo();
                pago.setUsuarioNombre(comprador);
                pago.setTipo("PAGO");
                pago.setMonto(monto);
                pago.setSaldoAnterior(saldoCompradorAntes);
                pago.setSaldoNuevo(saldoCompradorAntes - monto);
                pago.setConcepto(concepto);
                pago.setContraparte(vendedor);
                pago.setMetodoPago(metodoPago);
                movimientoSaldoDao().insertar(pago);

                MovimientoSaldo ingreso = new MovimientoSaldo();
                ingreso.setUsuarioNombre(vendedor);
                ingreso.setTipo("INGRESO");
                ingreso.setMonto(monto);
                ingreso.setSaldoAnterior(saldoVendedorAntes);
                ingreso.setSaldoNuevo(saldoVendedorAntes + monto);
                ingreso.setConcepto(concepto);
                ingreso.setContraparte(comprador);
                ingreso.setMetodoPago(metodoPago);
                movimientoSaldoDao().insertar(ingreso);
            });
            return true;
        } catch (SaldoInsuficienteException e) {
            return false;
        }
    }

    private interface RellenarUsuario { void aplicar(Usuario u); }

    private static void crearSiFalta(UsuarioDao dao, String username, String correo,
                                      RellenarUsuario rellenar) {
        if (dao.contarPorNombreUsuario(username) > 0) return;
        if (correo != null && dao.contarPorCorreo(correo) > 0) return;
        Usuario u = new Usuario();
        u.setNombreUsuario(username);
        u.setContrasena("admin123");
        u.setCorreo(correo);
        u.setBloqueado(false);
        u.setIntentosFallidos(0);
        rellenar.aplicar(u);
        dao.insertar(u);
    }
}
