package com.estaciones.qestaciones.hu01_autenticacion;

import android.content.Context;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.Auditoria;
import java.util.List;

public class AuditoriaGestor {
    private final BaseDatosEstaciones db;

    public AuditoriaGestor(Context context) {
        this.db = BaseDatosEstaciones.obtenerInstancia(context);
    }

    public void registrarAccion(String usuario, String accion, String modulo, String detalle) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setAccion(accion);
        auditoria.setModulo(modulo);
        auditoria.setDetalle(detalle);
        db.auditoriaDao().insertar(auditoria);
    }

    public List<Auditoria> obtenerTodo() {
        return db.auditoriaDao().obtenerTodas();
    }
}
