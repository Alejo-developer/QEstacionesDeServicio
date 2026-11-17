# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and run

Android app, Gradle wrapper is committed. From the repo root:

- `./gradlew assembleDebug` — compile the debug APK (output in `app/build/outputs/apk/debug/`).
- `./gradlew installDebug` — install on the connected device/emulator.
- `./gradlew clean` — wipe `build/` (does not touch emulator data).
- No unit tests or instrumented tests are configured. There's no `lint` task beyond Gradle's default; don't invent test commands.

`local.properties` holds `sdk.dir` and is git-ignored — don't commit it. JDK 17 required (`sourceCompatibility`/`targetCompatibility` in `app/build.gradle`).

## Architecture essentials

Single-module Android app (namespace `com.estaciones.qestaciones`), Java only (no Kotlin), Room 2.6.1, Material Components, `com.sun.mail:android-mail` for real SMTP. minSdk 24, compileSdk 34, AGP 8.13.2.

### Layout by feature (HU = Historia de Usuario)

Source is organized by user story, **not** by layer. Each HU folder holds its activities, gestores (managers), and helpers:

```
app/src/main/java/com/estaciones/qestaciones/
├── PrincipalActividad.java      ← role-based menu hub
├── hu01_autenticacion/          ← login, registro, 2FA, ServidorCorreo, auditoría
├── hu03_precios/                ← config precios, margen estación, precio referencia
├── hu04_consulta/               ← consulta pública
├── hu05_subsidios/              ← ApiTransitoServicio (RUNT mock), GestorSubsidios
├── hu06_zonas/, hu07_alertas/, hu08_inventario/ (Venta + InventarioTiempoReal)
├── hu09_entregas/, hu10_trazabilidad/, hu11_reportes/, hu13_inventarios/
├── hu14_usuario_particular/     ← cliente: seleccionar empresa, compra, perfil, historial
├── modelo/   ← entidades Room
└── datos/    ← DAOs + BaseDatosEstaciones (singleton, allowMainThreadQueries)
```

Layouts follow the same Spanish naming: `actividad_*.xml`, `item_*.xml`. Classes use `Actividad` not `Activity`, `Gestor` not `Manager`, `Dao` suffix on all DAOs.

### Six roles, one menu

`PrincipalActividad` receives `nombreUsuario` + `nombreCompleto` + `rol` via Intent extras and *dynamically builds* MaterialButtons that it shows/hides per role (`ADMINISTRADOR`, `OPERADOR`, `DISTRIBUIDOR`, `ESTACION`, `REGULADOR`, `USUARIO_PARTICULAR`). There are no per-role layouts — all buttons live in `actividad_principal.xml` plus ones added programmatically.

**When adding a new activity that needs the logged-in user,** wire navigation through the `conScope(Intent)` helper in `PrincipalActividad` — it propagates `nombreUsuario`/`nombreCompleto`/`rol` so the target activity can do scoping. Read these back with `getIntent().getStringExtra(...)`.

### Per-empresa data scoping

A user with role `ESTACION` or `DISTRIBUIDOR` *is* an empresa — their `nombreUsuario` is the stable key used to isolate inventory, prices and sales. If you add a new table that belongs to an empresa, add a nullable `empresaUsuario` column and filter queries by it. Existing scoped columns/queries:

- `MovimientoInventario.empresaUsuario` + `MovimientoInventarioDao.obtenerSaldoPorEmpresa(combustible, empresa)`
- `InventarioDistribuidor.nombreDistribuidor` (already the empresa key)
- `MargenEstacion.nombreEstacion` (already the empresa key)
- `Transaccion.empresaVendedoraUsuario` + `Transaccion.tipoVendedor` (`ESTACION`/`DISTRIBUIDOR`) + `Transaccion.compradorUsuario` (for `USUARIO_PARTICULAR` sales)

When a `USUARIO_PARTICULAR` buys: `CompraActividad` writes `compradorUsuario` (client) and `empresaVendedoraUsuario` (seller). `HistorialComprasActividad` reads by `compradorUsuario`.

### Subsidio flow — role-critical

Only `OPERADOR`, `REGULADOR`, `ADMINISTRADOR` may call `ApiTransitoServicio` (the RUNT mock) via `GestorSubsidios.validarSubsidio`. An `ESTACION` selling fuel **must not** hit the API; it only consults prior approvals via `ValidacionSubsidioDao.obtenerUltimaAprobadaPorPlaca(placa)`. In `VentaActividad`, if the role is `ESTACION` and a plate was entered but no `APROBADO` record exists, the flag `ventaPermitida = false` and the sale is blocked. Changing the plate text invalidates any previous approval (TextWatcher). Do not bypass this without explicit reason.

### Room database — policy

`BaseDatosEstaciones` is a `volatile` singleton with `allowMainThreadQueries()` on. All DAOs run on the main thread by design — this project doesn't use coroutines/Executor wrappers for DAO calls. Keep that convention unless refactoring the whole data layer.

**Migration policy (hard rule):** `fallbackToDestructiveMigration()` was removed intentionally. Never re-add it. When you change a schema:

1. Add a `Migration(from, to)` to `BaseDatosEstaciones` (see `MIGRATION_8_9` as the pattern — uses `ALTER TABLE ADD COLUMN` with nullable types to preserve rows).
2. Bump `@Database(version = N)`.
3. Register it with `.addMigrations(...)` in `obtenerInstancia`.

A missing migration *should* crash loudly rather than silently wipe `estaciones_servicio_db`.

The DB seeds a default admin (`admin` / `admin123`) on first run via `crearUsuarioAdminPorDefecto` — do not delete that user.

### External integrations

- **SMTP (real)** — `ServidorCorreo` reads the Gmail sender and 16-char app password from `local.properties` (`smtp.correo`, `smtp.contrasena`) via `BuildConfig.SMTP_CORREO` / `BuildConfig.SMTP_CONTRASENA`. Never hardcode credentials in source. Both `enviarCodigoVerificacion` (2FA) and `enviarFactura` (sales + purchases) go through this. When adding new outbound mail, reuse `enviarFactura`'s executor pattern — never block the UI thread on SMTP.
- **RUNT mock** — `ApiTransitoServicio` is a local mock returning fake vehicles, not a real HTTP call. No Retrofit or OkHttp in this project.
- **File sharing (PDF/CSV reports)** — Uses a `FileProvider` with authority `${applicationId}.fileprovider`; paths defined in `res/xml/file_paths.xml`. Required permission `POST_NOTIFICATIONS` is already in the manifest (Android 13+).

### Language conventions

- All identifiers, comments and UI strings are in **Spanish**. Don't translate existing names to English or switch to English for new code in this project. Follow the prefixes already in use (`Actividad`, `Gestor`, `Dao`, `campoX`, `botonX`, `actividad_*.xml`, `item_*.xml`).
- HU folders use `huNN_descripcion` naming — match that when creating new feature folders.
