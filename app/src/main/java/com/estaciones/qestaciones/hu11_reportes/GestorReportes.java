package com.estaciones.qestaciones.hu11_reportes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.estaciones.qestaciones.modelo.ReglaPrecios;
import com.estaciones.qestaciones.modelo.ValidacionSubsidio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generador de reportes oficiales en PDF y CSV (HU-11).
 * Exporta datos de inventario, precios y validaciones de subsidios.
 *
 * HU-11 DOING completado:
 * - Exportacion en PDF usando android.graphics.pdf.PdfDocument (sin librerias externas)
 * - Exportacion en CSV compatible con Excel
 * - Archivos guardados en almacenamiento externo de la app (no requiere permiso)
 */
public class GestorReportes {

    private final Context contexto;
    private final BaseDatosEstaciones db;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat fmtArchivo = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    // Medidas pagina A4 en puntos (72 dpi)
    private static final int ANCHO_PAGINA = 595;
    private static final int ALTO_PAGINA = 842;
    private static final int MARGEN = 30;
    private static final int LIMITE_Y = 820;

    public static class ResultadoReporte {
        public final boolean exitoso;
        public final String mensaje;
        public final File archivo;

        ResultadoReporte(boolean exitoso, String mensaje, File archivo) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.archivo = archivo;
        }
    }

    public GestorReportes(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.db = BaseDatosEstaciones.obtenerInstancia(contexto);
    }

    // ─────────────────────────────────────────────
    // PDF
    // ─────────────────────────────────────────────

    /**
     * Genera un reporte PDF con movimientos de inventario y reglas de precios.
     */
    public ResultadoReporte generarPDF() {
        PdfDocument doc = new PdfDocument();

        // Pinturas
        Paint pTitulo = crearPintura(Color.parseColor("#1565C0"), 18f, true);
        Paint pSeccion = crearPintura(Color.parseColor("#1565C0"), 12f, true);
        Paint pCabecera = crearPintura(Color.parseColor("#1565C0"), 9f, true);
        Paint pNormal = crearPintura(Color.BLACK, 9f, false);
        Paint pGris = crearPintura(Color.GRAY, 8f, false);
        Paint pLinea = crearPintura(Color.LTGRAY, 9f, false);
        pLinea.setStrokeWidth(0.5f);

        String ahora = fmt.format(new Date());

        // ── Pagina 1: Cabecera + Inventario ──
        PdfDocument.Page pag = doc.startPage(new PdfDocument.PageInfo.Builder(ANCHO_PAGINA, ALTO_PAGINA, 1).create());
        Canvas cv = pag.getCanvas();
        float[] y = {40};

        // Encabezado general
        cv.drawText("Q-Estaciones de Servicio  |  Reporte Oficial", MARGEN, y[0], pTitulo); y[0] += 18;
        cv.drawText("Generado: " + ahora + "  |  Decreto 1428 de 2025", MARGEN, y[0], pGris); y[0] += 10;
        cv.drawLine(MARGEN, y[0], ANCHO_PAGINA - MARGEN, y[0], pLinea); y[0] += 16;

        // Seccion inventario
        cv.drawText("MOVIMIENTOS DE INVENTARIO", MARGEN, y[0], pSeccion); y[0] += 14;
        cv.drawLine(MARGEN, y[0], ANCHO_PAGINA - MARGEN, y[0], pLinea); y[0] += 10;

        // Cabecera tabla
        cv.drawText("Fecha", MARGEN, y[0], pCabecera);
        cv.drawText("Tipo", 155, y[0], pCabecera);
        cv.drawText("Combustible", 210, y[0], pCabecera);
        cv.drawText("Volumen (L)", 335, y[0], pCabecera);
        cv.drawText("Saldo Nuevo (L)", 430, y[0], pCabecera);
        y[0] += 12;
        cv.drawLine(MARGEN, y[0], ANCHO_PAGINA - MARGEN, y[0], pLinea); y[0] += 8;

        List<MovimientoInventario> movs = db.movimientoInventarioDao().obtenerTodos();
        for (MovimientoInventario m : movs) {
            if (y[0] > LIMITE_Y) break;
            cv.drawText(fmt.format(new Date(m.getFechaMovimiento())), MARGEN, y[0], pNormal);
            cv.drawText(m.getTipoMovimiento(), 155, y[0], pNormal);
            cv.drawText(m.getTipoCombustible(), 210, y[0], pNormal);
            cv.drawText(String.format("%.0f", m.getVolumenLitros()), 335, y[0], pNormal);
            cv.drawText(String.format("%.0f", m.getSaldoNuevo()), 430, y[0], pNormal);
            y[0] += 12;
        }
        if (movs.isEmpty()) {
            cv.drawText("Sin movimientos registrados.", MARGEN, y[0], pGris); y[0] += 12;
        }

        y[0] += 16;
        // Seccion precios
        if (y[0] < LIMITE_Y - 60) {
            cv.drawText("HISTORIAL DE PRECIOS NORMATIVOS", MARGEN, y[0], pSeccion); y[0] += 14;
            cv.drawLine(MARGEN, y[0], ANCHO_PAGINA - MARGEN, y[0], pLinea); y[0] += 10;
            cv.drawText("Zona", MARGEN, y[0], pCabecera);
            cv.drawText("Vehiculo", 155, y[0], pCabecera);
            cv.drawText("Precio Base", 280, y[0], pCabecera);
            cv.drawText("Subsidiado", 370, y[0], pCabecera);
            cv.drawText("Decreto", 460, y[0], pCabecera);
            y[0] += 12;
            cv.drawLine(MARGEN, y[0], ANCHO_PAGINA - MARGEN, y[0], pLinea); y[0] += 8;

            List<ReglaPrecios> reglas = db.reglaPreciosDao().obtenerTodas();
            for (ReglaPrecios r : reglas) {
                if (y[0] > LIMITE_Y) break;
                cv.drawText(r.getZona() != null ? r.getZona() : "", MARGEN, y[0], pNormal);
                cv.drawText(r.getTipoVehiculo() != null ? r.getTipoVehiculo() : "", 155, y[0], pNormal);
                cv.drawText(String.format("$%,.0f", r.getPrecioBase()), 280, y[0], pNormal);
                cv.drawText(String.format("$%,.0f", r.getPrecioSubsidiado()), 370, y[0], pNormal);
                cv.drawText(r.getDecreto() != null ? r.getDecreto() : "", 460, y[0], pNormal);
                y[0] += 12;
            }
            if (reglas.isEmpty()) {
                cv.drawText("Sin reglas de precios registradas.", MARGEN, y[0], pGris);
            }
        }

        // Pie de pagina
        cv.drawText("Q-Estaciones de Servicio  |  Reporte generado automaticamente", MARGEN, ALTO_PAGINA - 20, pGris);

        doc.finishPage(pag);

        // Guardar archivo
        File dir = contexto.getExternalFilesDir("Reportes");
        if (dir != null) dir.mkdirs();
        File archivo = new File(dir, "Reporte_" + fmtArchivo.format(new Date()) + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            doc.writeTo(fos);
            doc.close();
            return new ResultadoReporte(true, "PDF guardado en:\n" + archivo.getAbsolutePath(), archivo);
        } catch (IOException e) {
            doc.close();
            return new ResultadoReporte(false, "Error al guardar PDF: " + e.getMessage(), null);
        }
    }

    // ─────────────────────────────────────────────
    // CSV (compatible Excel)
    // ─────────────────────────────────────────────

    /**
     * Genera un archivo CSV con inventario, precios y validaciones de subsidio.
     */
    public ResultadoReporte generarCSV() {
        StringBuilder sb = new StringBuilder('\uFEFF'); // BOM UTF-8 para Excel
        sb.append("Reporte Oficial Q-Estaciones de Servicio\n");
        sb.append("Generado:,").append(fmt.format(new Date())).append("\n\n");

        // Movimientos inventario
        sb.append("MOVIMIENTOS DE INVENTARIO\n");
        sb.append("Fecha,Tipo,Combustible,Volumen (L),Saldo Anterior (L),Saldo Nuevo (L),Motivo,Responsable\n");
        List<MovimientoInventario> movs = db.movimientoInventarioDao().obtenerTodos();
        for (MovimientoInventario m : movs) {
            sb.append(fmt.format(new Date(m.getFechaMovimiento()))).append(",")
              .append(m.getTipoMovimiento()).append(",")
              .append(m.getTipoCombustible()).append(",")
              .append(String.format("%.0f", m.getVolumenLitros())).append(",")
              .append(String.format("%.0f", m.getSaldoAnterior())).append(",")
              .append(String.format("%.0f", m.getSaldoNuevo())).append(",")
              .append(csv(m.getMotivo())).append(",")
              .append(csv(m.getResponsable())).append("\n");
        }
        if (movs.isEmpty()) sb.append("Sin movimientos\n");

        // Reglas de precios
        sb.append("\nREGLAS DE PRECIOS NORMATIVOS\n");
        sb.append("Zona,Tipo Vehiculo,Precio Base (COP),Precio Subsidiado (COP),Decreto,Fecha Vigencia\n");
        List<ReglaPrecios> reglas = db.reglaPreciosDao().obtenerTodas();
        for (ReglaPrecios r : reglas) {
            sb.append(csv(r.getZona())).append(",")
              .append(csv(r.getTipoVehiculo())).append(",")
              .append(String.format("%.0f", r.getPrecioBase())).append(",")
              .append(String.format("%.0f", r.getPrecioSubsidiado())).append(",")
              .append(csv(r.getDecreto())).append(",")
              .append(fmt.format(new Date(r.getFechaCreacion()))).append("\n");
        }
        if (reglas.isEmpty()) sb.append("Sin reglas registradas\n");

        // Validaciones subsidio
        sb.append("\nVALIDACIONES DE SUBSIDIO\n");
        sb.append("Placa,Resultado,Tipo Vehiculo,Precio Aplicado (COP),Motivo Rechazo,Fecha\n");
        List<ValidacionSubsidio> valids = db.validacionSubsidioDao().obtenerTodas();
        for (ValidacionSubsidio v : valids) {
            sb.append(csv(v.getPlaca())).append(",")
              .append(csv(v.getResultado())).append(",")
              .append(csv(v.getTipoVehiculo())).append(",")
              .append(String.format("%.0f", v.getPrecioAplicado())).append(",")
              .append(csv(v.getMotivoRechazo())).append(",")
              .append(fmt.format(new Date(v.getFechaValidacion()))).append("\n");
        }
        if (valids.isEmpty()) sb.append("Sin validaciones registradas\n");

        File dir = contexto.getExternalFilesDir("Reportes");
        if (dir != null) dir.mkdirs();
        File archivo = new File(dir, "Reporte_" + fmtArchivo.format(new Date()) + ".csv");

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(sb.toString().getBytes("UTF-8"));
            return new ResultadoReporte(true, "CSV guardado en:\n" + archivo.getAbsolutePath(), archivo);
        } catch (IOException e) {
            return new ResultadoReporte(false, "Error al guardar CSV: " + e.getMessage(), null);
        }
    }

    // ─────────────────────────────────────────────
    // HU-12: Reporte por decreto
    // ─────────────────────────────────────────────

    /**
     * Genera un CSV con todas las reglas de precios de un decreto especifico (HU-12).
     * Permite a la Autoridad Reguladora auditar la normativa por decreto.
     *
     * @param decreto Texto del decreto a filtrar (busqueda parcial, ignorando mayusculas)
     */
    public ResultadoReporte generarReportePorDecreto(String decreto) {
        List<ReglaPrecios> todas = db.reglaPreciosDao().obtenerTodas();
        List<ReglaPrecios> filtradas = new ArrayList<>();
        String filtro = decreto != null ? decreto.trim().toLowerCase(Locale.getDefault()) : "";

        for (ReglaPrecios r : todas) {
            if (filtro.isEmpty() || (r.getDecreto() != null &&
                    r.getDecreto().toLowerCase(Locale.getDefault()).contains(filtro))) {
                filtradas.add(r);
            }
        }

        StringBuilder sb = new StringBuilder('\uFEFF');
        sb.append("Reporte por Decreto - Q-Estaciones de Servicio\n");
        sb.append("Decreto filtrado:,").append(filtro.isEmpty() ? "Todos" : decreto).append("\n");
        sb.append("Generado:,").append(fmt.format(new Date())).append("\n\n");
        sb.append("Zona,Tipo Vehiculo,Precio Base (COP),Precio Subsidiado (COP),Aplica Subsidio,Decreto,Fecha\n");

        for (ReglaPrecios r : filtradas) {
            sb.append(csv(r.getZona())).append(",")
              .append(csv(r.getTipoVehiculo())).append(",")
              .append(String.format("%.0f", r.getPrecioBase())).append(",")
              .append(String.format("%.0f", r.getPrecioSubsidiado())).append(",")
              .append(r.isAplicaSubsidio() ? "SI" : "NO").append(",")
              .append(csv(r.getDecreto())).append(",")
              .append(fmt.format(new Date(r.getFechaCreacion()))).append("\n");
        }
        if (filtradas.isEmpty()) sb.append("Sin registros para este decreto\n");
        sb.append("\nTotal registros:,").append(filtradas.size()).append("\n");

        File dir = contexto.getExternalFilesDir("Reportes");
        if (dir != null) dir.mkdirs();
        String nombre = "Decreto_" + fmtArchivo.format(new Date()) + ".csv";
        File archivo = new File(dir, nombre);

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(sb.toString().getBytes("UTF-8"));
            return new ResultadoReporte(true,
                    filtradas.size() + " registros exportados", archivo);
        } catch (IOException e) {
            return new ResultadoReporte(false, "Error: " + e.getMessage(), null);
        }
    }

    // ─────────────────────────────────────────────
    // Utilitarios
    // ─────────────────────────────────────────────

    private Paint crearPintura(int color, float tamano, boolean negrita) {
        Paint p = new Paint();
        p.setColor(color);
        p.setTextSize(tamano);
        p.setFakeBoldText(negrita);
        p.setAntiAlias(true);
        return p;
    }

    /** Escapa un valor para CSV (envuelve en comillas si contiene comas o comillas). */
    private String csv(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
