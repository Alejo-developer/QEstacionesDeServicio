package com.estaciones.qestaciones.hu15_saldo;

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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * "Saldos del Sistema" — vista solo-lectura para ADMINISTRADOR.
 * Lista cada usuario con su saldo actual, ordenado de mayor a menor.
 * Suma total al pie como auditoría rápida.
 */
public class SaldosSistemaActividad extends AppCompatActivity {

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private UsuarioDao usuarioDao;
    private RecyclerView lista;
    private TextView textoTotal;
    private SaldosAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_saldos_sistema);

        usuarioDao = BaseDatosEstaciones.obtenerInstancia(this).usuarioDao();

        lista = findViewById(R.id.listaSaldos);
        textoTotal = findViewById(R.id.textoSaldoTotalSistema);

        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new SaldosAdaptador();
        lista.setAdapter(adaptador);

        findViewById(R.id.botonVolverSaldos).setOnClickListener(v -> finish());

        cargar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        List<Usuario> todos = new ArrayList<>(usuarioDao.obtenerTodos());
        // Excluimos al admin de la suma (no opera con saldo) y ordenamos descendente.
        Collections.sort(todos, new Comparator<Usuario>() {
            @Override public int compare(Usuario a, Usuario b) {
                return Double.compare(b.getSaldoCop(), a.getSaldoCop());
            }
        });

        double total = 0;
        for (Usuario u : todos) {
            if (!"ADMINISTRADOR".equals(u.getRol())) total += u.getSaldoCop();
        }
        textoTotal.setText("$" + FMT.format(total));
        adaptador.actualizar(todos);
    }

    private static class SaldosAdaptador extends RecyclerView.Adapter<SaldosAdaptador.VH> {
        private List<Usuario> items = new ArrayList<>();

        void actualizar(List<Usuario> nuevos) { items = nuevos; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saldo_sistema, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Usuario u = items.get(position);
            h.nombre.setText(u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getNombreUsuario());
            h.usuario.setText("@" + u.getNombreUsuario());
            h.rol.setText(u.getRol() != null ? u.getRol() : "");
            h.saldo.setText("$" + FMT.format(u.getSaldoCop()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView nombre, usuario, rol, saldo;
            VH(View v) {
                super(v);
                nombre = v.findViewById(R.id.textoNombreSaldo);
                usuario = v.findViewById(R.id.textoUsuarioSaldoItem);
                rol = v.findViewById(R.id.textoRolSaldoItem);
                saldo = v.findViewById(R.id.textoSaldoItem);
            }
        }
    }
}
