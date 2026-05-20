package com.estaciones.qestaciones.hu14_usuario_particular;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.UsuarioDao;
import com.estaciones.qestaciones.modelo.Usuario;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla "¿A quién comprar?" para el USUARIO_PARTICULAR.
 * Tabs "Estaciones" y "Distribuidores" listan las empresas registradas (usuarios con
 * rol ESTACION o DISTRIBUIDOR). Al tocar una empresa abre CompraActividad con los
 * datos de la empresa seleccionada.
 */
public class SeleccionarEmpresaActividad extends AppCompatActivity {

    private UsuarioDao usuarioDao;
    private String nombreUsuario = "";

    private RecyclerView lista;
    private TextView textoSin;
    private MaterialButton tabEstaciones, tabDistribuidores;
    private EmpresasAdaptador adaptador;

    private String tipoActual = "ESTACION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_seleccionar_empresa);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();
        nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario == null) nombreUsuario = "";

        lista = findViewById(R.id.listaEmpresas);
        textoSin = findViewById(R.id.textoSinEmpresas);
        tabEstaciones = findViewById(R.id.tabEstaciones);
        tabDistribuidores = findViewById(R.id.tabDistribuidores);

        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new EmpresasAdaptador(this::elegirEmpresa);
        lista.setAdapter(adaptador);

        tabEstaciones.setOnClickListener(v -> seleccionarTab("ESTACION"));
        tabDistribuidores.setOnClickListener(v -> seleccionarTab("DISTRIBUIDOR"));
        findViewById(R.id.botonVolverEmpresas).setOnClickListener(v -> finish());

        seleccionarTab("ESTACION");
    }

    private void seleccionarTab(String tipo) {
        tipoActual = tipo;
        boolean esEstacion = "ESTACION".equals(tipo);
        // Resalta visualmente la tab activa
        tabEstaciones.setBackgroundTintList(getColorStateList(
                esEstacion ? R.color.primario : R.color.superficie));
        tabDistribuidores.setBackgroundTintList(getColorStateList(
                esEstacion ? R.color.superficie : R.color.primario));
        cargar();
    }

    private void cargar() {
        List<Usuario> empresas = usuarioDao.obtenerPorRol(tipoActual);
        if (empresas.isEmpty()) {
            textoSin.setVisibility(View.VISIBLE);
            lista.setVisibility(View.GONE);
        } else {
            textoSin.setVisibility(View.GONE);
            lista.setVisibility(View.VISIBLE);
            adaptador.actualizar(empresas, tipoActual);
        }
    }

    private void elegirEmpresa(Usuario empresa) {
        Intent i = new Intent(this, CompraActividad.class);
        i.putExtra("nombreUsuario", nombreUsuario);                 // comprador (cliente)
        i.putExtra("empresaVendedora", empresa.getNombreUsuario()); // scope de la empresa
        i.putExtra("empresaVendedoraNombre", empresa.getNombreCompleto());
        i.putExtra("tipoVendedor", tipoActual);                     // ESTACION o DISTRIBUIDOR
        startActivity(i);
    }

    private interface OnEmpresaClick { void click(Usuario u); }

    private static class EmpresasAdaptador extends RecyclerView.Adapter<EmpresasAdaptador.VH> {
        private List<Usuario> items = new ArrayList<>();
        private String tipo = "ESTACION";
        private final OnEmpresaClick listener;

        EmpresasAdaptador(OnEmpresaClick listener) { this.listener = listener; }

        void actualizar(List<Usuario> nuevos, String tipo) {
            this.items = nuevos;
            this.tipo = tipo;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empresa, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Usuario u = items.get(position);
            h.icono.setText("ESTACION".equals(tipo) ? "⛽" : "🚛");
            h.nombre.setText(u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getNombreUsuario());
            h.usuario.setText("@" + u.getNombreUsuario());
            String ubic = u.getCiudad() != null && !u.getCiudad().isEmpty()
                    ? "📍 " + u.getCiudad() : "";
            String dir = u.getDireccion() != null && !u.getDireccion().isEmpty()
                    ? (ubic.isEmpty() ? "" : " · ") + u.getDireccion() : "";
            String texto = (ubic + dir).trim();
            h.ubicacion.setVisibility(texto.isEmpty() ? View.GONE : View.VISIBLE);
            h.ubicacion.setText(texto);
            h.itemView.setOnClickListener(v -> listener.click(u));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView icono, nombre, usuario, ubicacion;
            VH(View v) {
                super(v);
                icono = v.findViewById(R.id.textoIconoEmpresa);
                nombre = v.findViewById(R.id.textoNombreEmpresa);
                usuario = v.findViewById(R.id.textoUsuarioEmpresa);
                ubicacion = v.findViewById(R.id.textoUbicacionEmpresa);
            }
        }
    }
}
