package com.estaciones.qestaciones.hu14_usuario_particular;

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
import com.estaciones.qestaciones.datos.TransaccionDao;
import com.estaciones.qestaciones.modelo.Transaccion;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Historial de compras realizadas por el USUARIO_PARTICULAR logueado.
 * Filtra las transacciones por compradorUsuario = nombreUsuario (set en CompraActividad).
 */
public class HistorialComprasActividad extends AppCompatActivity {

    private TransaccionDao transaccionDao;
    private String nombreUsuario = "";
    private RecyclerView lista;
    private TextView textoSin;
    private ComprasAdaptador adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_historial_compras);

        transaccionDao = BaseDatosEstaciones.obtenerInstancia(this).transaccionDao();
        nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario == null) nombreUsuario = "";

        if (nombreUsuario.isEmpty()) {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        lista = findViewById(R.id.listaCompras);
        textoSin = findViewById(R.id.textoSinCompras);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new ComprasAdaptador();
        lista.setAdapter(adaptador);

        findViewById(R.id.botonVolverCompras).setOnClickListener(v -> finish());

        cargar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        List<Transaccion> compras = transaccionDao.obtenerPorComprador(nombreUsuario);
        if (compras.isEmpty()) {
            textoSin.setVisibility(View.VISIBLE);
            lista.setVisibility(View.GONE);
        } else {
            textoSin.setVisibility(View.GONE);
            lista.setVisibility(View.VISIBLE);
            adaptador.actualizar(compras);
        }
    }

    private static class ComprasAdaptador extends RecyclerView.Adapter<ComprasAdaptador.VH> {
        private List<Transaccion> items = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        private final NumberFormat pesos = NumberFormat.getNumberInstance(new Locale("es", "CO"));

        void actualizar(List<Transaccion> nuevos) { items = nuevos; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compra, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Transaccion t = items.get(position);
            String tipo = t.getTipoVendedor() != null ? t.getTipoVendedor() : "";
            String vendedor = t.getEmpresaVendedoraUsuario() != null
                    ? t.getEmpresaVendedoraUsuario()
                    : (t.getNombreEstacion() != null ? t.getNombreEstacion() : "Vendedor");
            String icono = "ESTACION".equals(tipo) ? "⛽" : "🚛";
            h.vendedor.setText(icono + "  " + vendedor);
            h.fecha.setText(fmt.format(new Date(t.getFechaVenta())));
            h.combustible.setText(String.format(Locale.getDefault(),
                    "%.2f L %s  ·  $%s/L",
                    t.getVolumenLitros(),
                    t.getTipoCombustible() != null ? t.getTipoCombustible() : "",
                    pesos.format(t.getPrecioUnitario())));
            h.subsidio.setText(t.isSubsidioAplicado() ? "✅ Subsidio aplicado" : "Sin subsidio");
            h.subsidio.setTextColor(h.itemView.getContext().getColor(
                    t.isSubsidioAplicado() ? R.color.exito : R.color.texto_secundario));
            h.total.setText("$" + pesos.format(t.getTotalCop()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView vendedor, fecha, combustible, subsidio, total;
            VH(View v) {
                super(v);
                vendedor = v.findViewById(R.id.textoCompraVendedor);
                fecha = v.findViewById(R.id.textoCompraFecha);
                combustible = v.findViewById(R.id.textoCompraCombustible);
                subsidio = v.findViewById(R.id.textoCompraSubsidio);
                total = v.findViewById(R.id.textoCompraTotal);
            }
        }
    }
}
