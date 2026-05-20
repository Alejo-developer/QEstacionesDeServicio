package com.estaciones.qestaciones.hu01_autenticacion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListaUsuariosActividad extends AppCompatActivity {

    private UsuarioDao usuarioDao;
    private RecyclerView lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_lista_usuarios);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());

        lista = findViewById(R.id.listaUsuarios);
        lista.setLayoutManager(new LinearLayoutManager(this));

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        List<Usuario> usuarios = usuarioDao.obtenerTodos();
        lista.setAdapter(new UsuariosAdaptador(usuarios, usuarioDao, this::cargarUsuarios));
    }

    // ─── Adaptador ────────────────────────────────────────────────────────────

    private class UsuariosAdaptador extends RecyclerView.Adapter<UsuariosAdaptador.UsuarioViewHolder> {

        private List<Usuario> usuarios;
        private final UsuarioDao dao;
        private final Runnable recargar;
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        private final SimpleDateFormat fmtLargo = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        UsuariosAdaptador(List<Usuario> usuarios, UsuarioDao dao, Runnable recargar) {
            this.usuarios = usuarios;
            this.dao = dao;
            this.recargar = recargar;
        }

        @NonNull
        @Override
        public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false);
            return new UsuarioViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UsuarioViewHolder h, int position) {
            Usuario u = usuarios.get(position);
            h.nombre.setText(u.getNombreCompleto());
            h.usuario.setText("@" + u.getNombreUsuario());
            h.rol.setText(u.getRol());

            if (u.isBloqueado()) {
                if (u.getFechaBanHasta() > 0) {
                    h.estado.setText("BANEADO hasta " + fmt.format(new Date(u.getFechaBanHasta())));
                } else {
                    h.estado.setText("BLOQUEADO");
                }
                h.estado.setTextColor(h.itemView.getContext().getColor(R.color.error));
            } else {
                h.estado.setText("ACTIVO");
                h.estado.setTextColor(h.itemView.getContext().getColor(R.color.exito));
            }

            // Fecha de registro (historial de cuándo se dio de alta la empresa/usuario)
            h.fechaRegistro.setText("📅 Registrado: " + fmtLargo.format(new Date(u.getFechaCreacion())));

            // Datos de empresa / perfil: solo mostrar si hay info que exhibir
            StringBuilder datos = new StringBuilder();
            if (notBlank(u.getCorreo())) append(datos, "✉ " + u.getCorreo());
            if (notBlank(u.getTelefono())) append(datos, "📞 " + u.getTelefono());
            boolean esEmpresa = "ESTACION".equals(u.getRol()) || "DISTRIBUIDOR".equals(u.getRol());
            if (esEmpresa && notBlank(u.getNit())) append(datos, "🆔 NIT " + u.getNit());
            if (!esEmpresa && notBlank(u.getCedula())) append(datos, "🆔 CC " + u.getCedula());
            if (notBlank(u.getCiudad())) append(datos, "📍 " + u.getCiudad());
            if (notBlank(u.getDireccion())) append(datos, u.getDireccion());
            if (datos.length() == 0) {
                h.datosEmpresa.setVisibility(View.GONE);
            } else {
                h.datosEmpresa.setVisibility(View.VISIBLE);
                h.datosEmpresa.setText(datos.toString());
            }

            boolean esAdmin = "admin".equals(u.getNombreUsuario());

            h.botonBanear.setVisibility((!u.isBloqueado() && !esAdmin) ? View.VISIBLE : View.GONE);
            h.botonBanear.setOnClickListener(v -> mostrarDialogoBan(u));

            h.botonDesbloquear.setVisibility((u.isBloqueado() && !esAdmin) ? View.VISIBLE : View.GONE);
            h.botonDesbloquear.setOnClickListener(v -> confirmarDesbloqueo(u));

            h.botonEliminar.setVisibility(esAdmin ? View.GONE : View.VISIBLE);
            h.botonEliminar.setOnClickListener(v -> confirmarEliminacion(u));
        }

        private void mostrarDialogoBan(Usuario u) {
            Calendar hoy = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(
                    ListaUsuariosActividad.this,
                    (view, year, month, day) -> {
                        Calendar fechaFin = Calendar.getInstance();
                        fechaFin.set(year, month, day, 23, 59, 59);
                        long hasta = fechaFin.getTimeInMillis();
                        if (hasta <= System.currentTimeMillis()) {
                            Toast.makeText(ListaUsuariosActividad.this,
                                    "La fecha debe ser futura", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dao.banearHasta(u.getId(), hasta);
                        Toast.makeText(ListaUsuariosActividad.this,
                                "@" + u.getNombreUsuario() + " baneado hasta "
                                        + fmt.format(new Date(hasta)), Toast.LENGTH_SHORT).show();
                        recargar.run();
                    },
                    hoy.get(Calendar.YEAR),
                    hoy.get(Calendar.MONTH),
                    hoy.get(Calendar.DAY_OF_MONTH)
            );
            picker.setTitle("Fecha fin del ban para @" + u.getNombreUsuario());
            picker.getDatePicker().setMinDate(System.currentTimeMillis());
            picker.show();
        }

        private void confirmarDesbloqueo(Usuario u) {
            new AlertDialog.Builder(ListaUsuariosActividad.this)
                    .setTitle("Desbloquear usuario")
                    .setMessage("¿Desbloquear la cuenta de @" + u.getNombreUsuario() + "?")
                    .setPositiveButton("Desbloquear", (d, w) -> {
                        dao.desbloquear(u.getId());
                        Toast.makeText(ListaUsuariosActividad.this,
                                "@" + u.getNombreUsuario() + " desbloqueado", Toast.LENGTH_SHORT).show();
                        recargar.run();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

        private static void append(StringBuilder sb, String s) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(s);
        }

        private void confirmarEliminacion(Usuario u) {
            new AlertDialog.Builder(ListaUsuariosActividad.this)
                    .setTitle("Eliminar cuenta")
                    .setMessage("¿Eliminar permanentemente la cuenta de @" + u.getNombreUsuario()
                            + " (" + u.getRol() + ")?\nEsta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        dao.eliminar(u);
                        Toast.makeText(ListaUsuariosActividad.this,
                                "Cuenta @" + u.getNombreUsuario() + " eliminada", Toast.LENGTH_SHORT).show();
                        recargar.run();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        @Override
        public int getItemCount() { return usuarios.size(); }

        class UsuarioViewHolder extends RecyclerView.ViewHolder {
            TextView nombre, usuario, rol, estado, fechaRegistro, datosEmpresa;
            MaterialButton botonBanear, botonDesbloquear, botonEliminar;

            UsuarioViewHolder(View v) {
                super(v);
                nombre = v.findViewById(R.id.textoNombreCompleto);
                usuario = v.findViewById(R.id.textoNombreUsuario);
                rol = v.findViewById(R.id.textoRolUsuario);
                estado = v.findViewById(R.id.textoEstado);
                fechaRegistro = v.findViewById(R.id.textoFechaRegistro);
                datosEmpresa = v.findViewById(R.id.textoDatosEmpresa);
                botonBanear = v.findViewById(R.id.botonBanear);
                botonDesbloquear = v.findViewById(R.id.botonDesbloquear);
                botonEliminar = v.findViewById(R.id.botonEliminarUser);
            }
        }
    }
}
