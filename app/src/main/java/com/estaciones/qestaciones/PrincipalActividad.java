package com.estaciones.qestaciones;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.estaciones.qestaciones.hu01_autenticacion.AuditoriaActividad;
import com.estaciones.qestaciones.hu01_autenticacion.InicioSesionActividad;
import com.estaciones.qestaciones.hu01_autenticacion.ListaUsuariosActividad;
import com.estaciones.qestaciones.hu03_precios.ConfiguracionPreciosActividad;
import com.estaciones.qestaciones.hu03_precios.HistorialNormativoActividad;
import com.estaciones.qestaciones.hu04_consulta.ConsultaPreciosActividad;
import com.estaciones.qestaciones.hu05_subsidios.ValidacionSubsidioActividad;
import com.estaciones.qestaciones.hu06_zonas.ConfiguracionZonasActividad;
import com.estaciones.qestaciones.hu03_precios.MargenEstacionActividad;
import com.estaciones.qestaciones.hu03_precios.PrecioReferenciaActividad;
import com.estaciones.qestaciones.hu07_alertas.AlertasNormativasActividad;
import com.estaciones.qestaciones.hu08_inventario.InventarioTiempoRealActividad;
import com.estaciones.qestaciones.hu08_inventario.VentaActividad;
import com.estaciones.qestaciones.hu09_entregas.InventarioDistribuidorActividad;
import com.estaciones.qestaciones.hu09_entregas.RegistroEntregaActividad;
import com.estaciones.qestaciones.hu10_trazabilidad.TrazabilidadActividad;
import com.estaciones.qestaciones.hu11_reportes.ReportesActividad;
import com.estaciones.qestaciones.hu13_inventarios.ControlInventarioActividad;
import com.estaciones.qestaciones.hu14_usuario_particular.CompraActividad;
import com.estaciones.qestaciones.hu14_usuario_particular.HistorialComprasActividad;
import com.estaciones.qestaciones.hu14_usuario_particular.PerfilUsuarioActividad;
import com.estaciones.qestaciones.hu14_usuario_particular.SeleccionarEmpresaActividad;
import com.estaciones.qestaciones.hu15_saldo.MiSaldoActividad;
import com.estaciones.qestaciones.hu15_saldo.PagarEmpleadoActividad;
import com.estaciones.qestaciones.hu15_saldo.SaldosSistemaActividad;
import com.estaciones.qestaciones.hu16_mapas.EstacionesCercanasActividad;
import com.estaciones.qestaciones.hu16_mapas.RutaDistribuidorActividad;
import com.estaciones.qestaciones.hu16_mapas.UbicacionEstacionActividad;
import com.estaciones.qestaciones.hu17_red.BoletinesNormativosActividad;
import com.google.android.material.button.MaterialButton;

/**
 * Actividad principal (menú) que se muestra tras el inicio de sesión.
 */
public class PrincipalActividad extends AppCompatActivity {

    private String nombreUsuario;
    private String nombreCompleto;
    private String rol;
    /**
     * Scope efectivo de datos por empresa. Para ESTACION/DISTRIBUIDOR es el propio
     * nombreUsuario; para OPERADOR es la estación a la que fue asignado; para
     * el resto queda igual al nombreUsuario (no se usa para scope).
     */
    private String empresaScope;
    private String empresaScopeNombre; // nombreCompleto de la empresa, si la hay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);

        nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        nombreCompleto = getIntent().getStringExtra("nombreCompleto");
        rol = getIntent().getStringExtra("rol");

        resolverEmpresaScope();

        configurarInterfaz();
        configurarEventos();
    }

    private void resolverEmpresaScope() {
        // Default: la empresa scope es el propio usuario logueado.
        empresaScope = nombreUsuario;

        if ("OPERADOR".equals(rol) && nombreUsuario != null) {
            com.estaciones.qestaciones.datos.UsuarioDao dao =
                    com.estaciones.qestaciones.datos.BaseDatosEstaciones
                            .obtenerInstancia(this).usuarioDao();
            com.estaciones.qestaciones.modelo.Usuario yo =
                    dao.buscarPorNombreUsuario(nombreUsuario);
            if (yo != null && yo.getEmpresaAsignada() != null
                    && !yo.getEmpresaAsignada().isEmpty()) {
                empresaScope = yo.getEmpresaAsignada();
                com.estaciones.qestaciones.modelo.Usuario emp =
                        dao.buscarPorNombreUsuario(empresaScope);
                if (emp != null) empresaScopeNombre = emp.getNombreCompleto();
            } else {
                // Operador sin estación asignada: sin scope válido para vender.
                empresaScope = null;
            }
        }
    }

    private void configurarInterfaz() {
        TextView textoBienvenida = findViewById(R.id.textoBienvenida);
        TextView textoRol = findViewById(R.id.textoRol);

        textoBienvenida.setText(getString(R.string.bienvenida, nombreCompleto));
        textoRol.setText(getString(R.string.rol_actual, rol));

        MaterialButton botonConfigPrecios = findViewById(R.id.botonConfigPrecios);
        MaterialButton botonHistorial = findViewById(R.id.botonHistorial);
        MaterialButton botonConfigZonas = findViewById(R.id.botonConfigZonas);
        MaterialButton botonValidarSubsidio = findViewById(R.id.botonValidarSubsidio);
        MaterialButton botonRegistrarEntregas = findViewById(R.id.botonRegistrarEntregas);
        MaterialButton botonControlInventario = findViewById(R.id.botonControlInventario);

        android.widget.LinearLayout layoutContenedor = (android.widget.LinearLayout)botonConfigPrecios.getParent();

        // Botón: Gestionar Usuarios
        MaterialButton botonVerUsuarios = new MaterialButton(this);
        botonVerUsuarios.setText("👥  Gestionar Usuarios");
        botonVerUsuarios.setAllCaps(false);
        layoutContenedor.addView(botonVerUsuarios, 0);
        botonVerUsuarios.setVisibility(View.GONE);

        // Botón: Auditoría de Seguridad (HU-02)
        MaterialButton botonAuditoria = new MaterialButton(this);
        botonAuditoria.setText("🛡  Auditoría de Seguridad");
        botonAuditoria.setAllCaps(false);
        layoutContenedor.addView(botonAuditoria, 1);
        botonAuditoria.setVisibility(View.GONE);

        // Botón: Consulta de Precios (Para Usuario Particular - HU-04)
        MaterialButton botonConsultaPrecios = new MaterialButton(this);
        botonConsultaPrecios.setText("🔍  Consultar Precios");
        botonConsultaPrecios.setAllCaps(false);
        layoutContenedor.addView(botonConsultaPrecios, 2);
        botonConsultaPrecios.setVisibility(View.GONE);

        // Botón: Reportes Regulatorios (HU-11/HU-12)
        MaterialButton botonReportes = new MaterialButton(this);
        botonReportes.setText("📊  Reportes Regulatorios");
        botonReportes.setAllCaps(false);
        layoutContenedor.addView(botonReportes, 3);
        botonReportes.setVisibility(View.GONE);

        // Botón: Alertas Normativas (HU-07)
        MaterialButton botonAlertas = new MaterialButton(this);
        botonAlertas.setText("🔔  Alertas Normativas");
        botonAlertas.setAllCaps(false);
        layoutContenedor.addView(botonAlertas, 4);
        botonAlertas.setVisibility(View.GONE);

        // Botón: Inventario en Tiempo Real (HU-08 — ESTACION)
        MaterialButton botonInventarioTiempoReal = new MaterialButton(this);
        botonInventarioTiempoReal.setText("📊  Inventario en Tiempo Real");
        botonInventarioTiempoReal.setAllCaps(false);
        layoutContenedor.addView(botonInventarioTiempoReal, 5);
        botonInventarioTiempoReal.setVisibility(View.GONE);

        // Botón: Trazabilidad de Operaciones (HU-10 — REGULADOR)
        MaterialButton botonTrazabilidad = new MaterialButton(this);
        botonTrazabilidad.setText("🔎  Trazabilidad de Operaciones");
        botonTrazabilidad.setAllCaps(false);
        layoutContenedor.addView(botonTrazabilidad, 6);
        botonTrazabilidad.setVisibility(View.GONE);

        // Botón: Precio de Referencia (REGULADOR)
        MaterialButton botonPrecioRef = new MaterialButton(this);
        botonPrecioRef.setText("💲  Precio de Referencia");
        botonPrecioRef.setAllCaps(false);
        layoutContenedor.addView(botonPrecioRef, 7);
        botonPrecioRef.setVisibility(View.GONE);

        // Botón: Mi Precio de Venta / Margen (ESTACION)
        MaterialButton botonMargenEstacion = new MaterialButton(this);
        botonMargenEstacion.setText("💲  Configurar Precio de Venta");
        botonMargenEstacion.setAllCaps(false);
        layoutContenedor.addView(botonMargenEstacion, 8);
        botonMargenEstacion.setVisibility(View.GONE);

        // Botón: Registrar Venta (ESTACION/OPERADOR)
        MaterialButton botonVenta = new MaterialButton(this);
        botonVenta.setText("🛒  Registrar Venta");
        botonVenta.setAllCaps(false);
        layoutContenedor.addView(botonVenta, 9);
        botonVenta.setVisibility(View.GONE);

        // Botón: Inventario Distribuidor (DISTRIBUIDOR)
        MaterialButton botonInventarioDist = new MaterialButton(this);
        botonInventarioDist.setText("🚛  Mi Inventario y Despachos");
        botonInventarioDist.setAllCaps(false);
        layoutContenedor.addView(botonInventarioDist, 10);
        botonInventarioDist.setVisibility(View.GONE);

        // Botón: Comprar Combustible (USUARIO_PARTICULAR)
        MaterialButton botonComprar = new MaterialButton(this);
        botonComprar.setText("🛒  Comprar Combustible");
        botonComprar.setAllCaps(false);
        layoutContenedor.addView(botonComprar, 11);
        botonComprar.setVisibility(View.GONE);

        // Botón: Mi Historial de Compras (USUARIO_PARTICULAR)
        MaterialButton botonHistorialCompras = new MaterialButton(this);
        botonHistorialCompras.setText("🧾  Mi Historial de Compras");
        botonHistorialCompras.setAllCaps(false);
        layoutContenedor.addView(botonHistorialCompras, 12);
        botonHistorialCompras.setVisibility(View.GONE);

        // Botón: Mi Perfil (USUARIO_PARTICULAR y cualquier empresa)
        MaterialButton botonPerfil = new MaterialButton(this);
        botonPerfil.setText("👤  Mi Perfil");
        botonPerfil.setAllCaps(false);
        layoutContenedor.addView(botonPerfil, 13);
        botonPerfil.setVisibility(View.GONE);

        // Botón: Mi Saldo (cliente, estación, distribuidor, operador)
        MaterialButton botonMiSaldo = new MaterialButton(this);
        botonMiSaldo.setText("💰  Mi Saldo");
        botonMiSaldo.setAllCaps(false);
        layoutContenedor.addView(botonMiSaldo, 14);
        botonMiSaldo.setVisibility(View.GONE);

        // Botón: Pagar a empleado (solo ESTACION)
        MaterialButton botonPagarEmpleado = new MaterialButton(this);
        botonPagarEmpleado.setText("💼  Pagar a empleado");
        botonPagarEmpleado.setAllCaps(false);
        layoutContenedor.addView(botonPagarEmpleado, 15);
        botonPagarEmpleado.setVisibility(View.GONE);

        // Botón: Saldos del sistema (solo ADMIN)
        MaterialButton botonSaldosSistema = new MaterialButton(this);
        botonSaldosSistema.setText("💼  Saldos del Sistema");
        botonSaldosSistema.setAllCaps(false);
        layoutContenedor.addView(botonSaldosSistema, 16);
        botonSaldosSistema.setVisibility(View.GONE);

        // Botón: Mi ubicación (Lab Maps Intent — ESTACION, DISTRIBUIDOR, USUARIO_PARTICULAR)
        MaterialButton botonMiUbicacion = new MaterialButton(this);
        botonMiUbicacion.setText("📍  Mi ubicación");
        botonMiUbicacion.setAllCaps(false);
        layoutContenedor.addView(botonMiUbicacion, 17);
        botonMiUbicacion.setVisibility(View.GONE);

        // Botón: Ruta del día (Lab Maps Intent — solo DISTRIBUIDOR)
        MaterialButton botonRutaDia = new MaterialButton(this);
        botonRutaDia.setText("🚚  Ruta del día");
        botonRutaDia.setAllCaps(false);
        layoutContenedor.addView(botonRutaDia, 18);
        botonRutaDia.setVisibility(View.GONE);

        // Botón: Estaciones cercanas (Lab Maps Intent — solo USUARIO_PARTICULAR)
        MaterialButton botonEstacionesCercanas = new MaterialButton(this);
        botonEstacionesCercanas.setText("🗺️  Estaciones cercanas");
        botonEstacionesCercanas.setAllCaps(false);
        layoutContenedor.addView(botonEstacionesCercanas, 19);
        botonEstacionesCercanas.setVisibility(View.GONE);

        // Botón: Boletines Normativos (Lab Retrofit — REGULADOR y ADMINISTRADOR)
        MaterialButton botonBoletines = new MaterialButton(this);
        botonBoletines.setText("🌐  Boletines Normativos");
        botonBoletines.setAllCaps(false);
        layoutContenedor.addView(botonBoletines, 20);
        botonBoletines.setVisibility(View.GONE);

        // Mostrar según rol
        switch (rol != null ? rol : "") {
            case "ADMINISTRADOR":
                botonConfigPrecios.setVisibility(View.VISIBLE);
                botonHistorial.setVisibility(View.VISIBLE);
                botonConfigZonas.setVisibility(View.VISIBLE);
                botonValidarSubsidio.setVisibility(View.VISIBLE);
                botonVerUsuarios.setVisibility(View.VISIBLE);
                botonAuditoria.setVisibility(View.VISIBLE);
                botonReportes.setVisibility(View.VISIBLE);
                botonSaldosSistema.setVisibility(View.VISIBLE);
                botonVerUsuarios.setOnClickListener(v -> {
                    Intent i = new Intent(this, ListaUsuariosActividad.class);
                    i.putExtra("rol", rol);
                    startActivity(i);
                });
                botonAuditoria.setOnClickListener(v -> startActivity(new Intent(this, AuditoriaActividad.class)));
                botonReportes.setOnClickListener(v -> startActivity(new Intent(this, ReportesActividad.class)));
                botonSaldosSistema.setOnClickListener(v -> startActivity(new Intent(this, SaldosSistemaActividad.class)));
                break;
            case "OPERADOR":
                botonValidarSubsidio.setVisibility(View.VISIBLE);
                botonVenta.setVisibility(View.VISIBLE);
                botonReportes.setVisibility(View.VISIBLE);
                botonReportes.setOnClickListener(v -> startActivity(new Intent(this, ReportesActividad.class)));
                // Acceso al inventario de la estación en la que trabaja
                if (empresaScope != null) {
                    botonVenta.setOnClickListener(v -> startActivity(conScope(new Intent(this, VentaActividad.class))));
                    botonInventarioTiempoReal.setVisibility(View.VISIBLE);
                    botonInventarioTiempoReal.setOnClickListener(v -> startActivity(
                            conScope(new Intent(this, InventarioTiempoRealActividad.class))));
                    String etiquetaEmpresa = empresaScopeNombre != null && !empresaScopeNombre.isEmpty()
                            ? empresaScopeNombre + " (@" + empresaScope + ")"
                            : "@" + empresaScope;
                    textoRol.setText(getString(R.string.rol_actual, rol) + " — Trabaja en: " + etiquetaEmpresa);
                } else {
                    // Operador sin estación asignada: no puede vender ni ver inventario.
                    botonVenta.setEnabled(false);
                    botonVenta.setAlpha(0.5f);
                    botonVenta.setOnClickListener(v -> android.widget.Toast.makeText(this,
                            "Pide al admin que te asigne a una estación antes de vender.",
                            android.widget.Toast.LENGTH_LONG).show());
                    textoRol.setText(getString(R.string.rol_actual, rol) + " — ⚠ Sin estación asignada");
                }
                break;
            case "DISTRIBUIDOR":
                botonRegistrarEntregas.setVisibility(View.VISIBLE);
                botonInventarioDist.setVisibility(View.VISIBLE);
                botonInventarioDist.setOnClickListener(v -> startActivity(
                        conScope(new Intent(this, InventarioDistribuidorActividad.class))));
                break;
            case "ESTACION":
                botonInventarioTiempoReal.setVisibility(View.VISIBLE);
                botonInventarioTiempoReal.setOnClickListener(v -> startActivity(conScope(new Intent(this, InventarioTiempoRealActividad.class))));
                botonControlInventario.setVisibility(View.VISIBLE);
                botonAlertas.setVisibility(View.VISIBLE);
                botonAlertas.setOnClickListener(v -> startActivity(new Intent(this, AlertasNormativasActividad.class)));
                botonMargenEstacion.setVisibility(View.VISIBLE);
                botonMargenEstacion.setOnClickListener(v -> startActivity(conScope(new Intent(this, MargenEstacionActividad.class))));
                botonVenta.setVisibility(View.VISIBLE);
                botonVenta.setOnClickListener(v -> startActivity(conScope(new Intent(this, VentaActividad.class))));
                break;
            case "REGULADOR":
                botonConfigPrecios.setVisibility(View.VISIBLE);
                botonHistorial.setVisibility(View.VISIBLE);
                botonConfigZonas.setVisibility(View.VISIBLE);
                botonValidarSubsidio.setVisibility(View.VISIBLE);
                botonVerUsuarios.setVisibility(View.VISIBLE);
                botonAuditoria.setVisibility(View.VISIBLE);
                botonReportes.setVisibility(View.VISIBLE);
                botonTrazabilidad.setVisibility(View.VISIBLE);
                botonPrecioRef.setVisibility(View.VISIBLE);
                botonVerUsuarios.setOnClickListener(v -> {
                    Intent i = new Intent(this, ListaUsuariosActividad.class);
                    i.putExtra("rol", rol);
                    startActivity(i);
                });
                botonAuditoria.setOnClickListener(v -> startActivity(new Intent(this, AuditoriaActividad.class)));
                botonReportes.setOnClickListener(v -> startActivity(new Intent(this, ReportesActividad.class)));
                botonTrazabilidad.setOnClickListener(v -> startActivity(new Intent(this, TrazabilidadActividad.class)));
                botonPrecioRef.setOnClickListener(v -> startActivity(new Intent(this, PrecioReferenciaActividad.class)));
                break;
            case "USUARIO_PARTICULAR":
                botonConsultaPrecios.setVisibility(View.VISIBLE);
                botonConsultaPrecios.setOnClickListener(v -> startActivity(new Intent(this, ConsultaPreciosActividad.class)));
                botonComprar.setVisibility(View.VISIBLE);
                botonComprar.setOnClickListener(v -> startActivity(conScope(new Intent(this, SeleccionarEmpresaActividad.class))));
                botonHistorialCompras.setVisibility(View.VISIBLE);
                botonHistorialCompras.setOnClickListener(v -> startActivity(conScope(new Intent(this, HistorialComprasActividad.class))));
                botonPerfil.setVisibility(View.VISIBLE);
                botonPerfil.setOnClickListener(v -> startActivity(conScope(new Intent(this, PerfilUsuarioActividad.class))));
                break;
        }

        // Mi Perfil también disponible para empresas (estación, distribuidor) y operadores
        if ("ESTACION".equals(rol) || "DISTRIBUIDOR".equals(rol) || "OPERADOR".equals(rol)) {
            botonPerfil.setVisibility(View.VISIBLE);
            botonPerfil.setOnClickListener(v -> startActivity(conScope(new Intent(this, PerfilUsuarioActividad.class))));
        }

        // Mi Saldo: para todos los roles que mueven dinero (no admin)
        if ("USUARIO_PARTICULAR".equals(rol) || "ESTACION".equals(rol)
                || "DISTRIBUIDOR".equals(rol) || "OPERADOR".equals(rol)) {
            botonMiSaldo.setVisibility(View.VISIBLE);
            botonMiSaldo.setOnClickListener(v -> startActivity(conScope(new Intent(this, MiSaldoActividad.class))));
        }

        // Pagar a empleado: solo ESTACION (paga a sus operadores asignados)
        if ("ESTACION".equals(rol)) {
            botonPagarEmpleado.setVisibility(View.VISIBLE);
            botonPagarEmpleado.setOnClickListener(v -> startActivity(conScope(new Intent(this, PagarEmpleadoActividad.class))));
        }

        // Lab Maps Intent — Mi ubicación: ESTACION, DISTRIBUIDOR y USUARIO_PARTICULAR
        // pueden registrar sus coords. El OPERADOR queda fuera porque trabaja para
        // una estación y no tiene ubicación propia.
        if ("ESTACION".equals(rol) || "DISTRIBUIDOR".equals(rol) || "USUARIO_PARTICULAR".equals(rol)) {
            botonMiUbicacion.setVisibility(View.VISIBLE);
            botonMiUbicacion.setOnClickListener(v -> {
                // Aquí pasamos el nombreUsuario REAL (no el scope), porque editamos al
                // usuario logueado — no al de la empresa donde trabaja.
                Intent i = new Intent(this, UbicacionEstacionActividad.class);
                i.putExtra("nombreUsuario", nombreUsuario);
                i.putExtra("nombreCompleto", nombreCompleto);
                i.putExtra("rol", rol);
                startActivity(i);
            });
        }

        // Lab Maps Intent — Ruta del día: solo DISTRIBUIDOR
        if ("DISTRIBUIDOR".equals(rol)) {
            botonRutaDia.setVisibility(View.VISIBLE);
            botonRutaDia.setOnClickListener(v -> startActivity(conScope(new Intent(this, RutaDistribuidorActividad.class))));
        }

        // Lab Maps Intent — Estaciones cercanas: solo USUARIO_PARTICULAR
        if ("USUARIO_PARTICULAR".equals(rol)) {
            botonEstacionesCercanas.setVisibility(View.VISIBLE);
            botonEstacionesCercanas.setOnClickListener(v -> {
                Intent i = new Intent(this, EstacionesCercanasActividad.class);
                i.putExtra("nombreUsuario", nombreUsuario);
                i.putExtra("nombreCompleto", nombreCompleto);
                i.putExtra("rol", rol);
                startActivity(i);
            });
        }

        // Lab Retrofit — Boletines Normativos: REGULADOR y ADMINISTRADOR
        if ("REGULADOR".equals(rol) || "ADMINISTRADOR".equals(rol)) {
            botonBoletines.setVisibility(View.VISIBLE);
            botonBoletines.setOnClickListener(v -> startActivity(
                    new Intent(this, BoletinesNormativosActividad.class)));
        }
    }

    /**
     * Añade al Intent los extras de identidad (nombreUsuario, nombreCompleto, rol)
     * para que la actividad destino pueda aislar datos por empresa/usuario.
     *
     * Si el usuario es OPERADOR con estación asignada, el "nombreUsuario" que se
     * pasa es el de la estación (empresa) — así la venta descuenta del inventario
     * de la estación, no del propio operador. El nombre real del operador se
     * conserva en el extra separado "nombreUsuarioOperador" para auditoría.
     */
    private Intent conScope(Intent i) {
        String scope = empresaScope != null ? empresaScope : nombreUsuario;
        i.putExtra("nombreUsuario", scope);
        i.putExtra("nombreCompleto", nombreCompleto);
        i.putExtra("rol", rol);
        i.putExtra("nombreUsuarioOperador", nombreUsuario);
        return i;
    }

    private void configurarEventos() {
        findViewById(R.id.botonConfigPrecios).setOnClickListener(v -> startActivity(new Intent(this, ConfiguracionPreciosActividad.class)));
        findViewById(R.id.botonHistorial).setOnClickListener(v -> startActivity(new Intent(this, HistorialNormativoActividad.class)));
        findViewById(R.id.botonConfigZonas).setOnClickListener(v -> startActivity(new Intent(this, ConfiguracionZonasActividad.class)));
        findViewById(R.id.botonValidarSubsidio).setOnClickListener(v -> {
            Intent i = new Intent(this, ValidacionSubsidioActividad.class);
            i.putExtra("rol", rol);
            startActivity(i);
        });
        findViewById(R.id.botonRegistrarEntregas).setOnClickListener(v -> startActivity(conScope(new Intent(this, RegistroEntregaActividad.class))));
        findViewById(R.id.botonControlInventario).setOnClickListener(v -> startActivity(conScope(new Intent(this, ControlInventarioActividad.class))));

        findViewById(R.id.botonCerrarSesion).setOnClickListener(v -> {
            Intent intento = new Intent(this, InicioSesionActividad.class);
            intento.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intento);
            finish();
        });
    }
}
