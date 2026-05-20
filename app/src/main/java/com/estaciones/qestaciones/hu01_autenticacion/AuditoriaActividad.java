package com.estaciones.qestaciones.hu01_autenticacion;

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
import com.estaciones.qestaciones.modelo.Auditoria;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuditoriaActividad extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_auditoria);
        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        
        RecyclerView lista = findViewById(R.id.listaAuditoria);
        lista.setLayoutManager(new LinearLayoutManager(this));
        
        AuditoriaGestor gestor = new AuditoriaGestor(this);
        List<Auditoria> logs = gestor.obtenerTodo();
        lista.setAdapter(new AuditoriaAdaptador(logs));
    }

    private static class AuditoriaAdaptador extends RecyclerView.Adapter<AuditoriaAdaptador.AuditoriaViewHolder> {
        private final List<Auditoria> logs;
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault());

        AuditoriaAdaptador(List<Auditoria> logs) { this.logs = logs; }

        @NonNull
        @Override
        public AuditoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auditoria, parent, false);
            return new AuditoriaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AuditoriaViewHolder h, int position) {
            Auditoria a = logs.get(position);
            h.accion.setText(a.getAccion());
            h.usuario.setText("👤 " + a.getUsuario());
            h.modulo.setText(a.getModulo());
            h.detalle.setText(a.getDetalle());
            h.fecha.setText(fmt.format(new Date(a.getFecha())));
        }

        @Override
        public int getItemCount() { return logs.size(); }

        static class AuditoriaViewHolder extends RecyclerView.ViewHolder {
            TextView accion, usuario, modulo, detalle, fecha;
            AuditoriaViewHolder(View v) {
                super(v);
                accion = v.findViewById(R.id.textoAccion);
                usuario = v.findViewById(R.id.textoUsuario);
                modulo = v.findViewById(R.id.textoModulo);
                detalle = v.findViewById(R.id.textoDetalle);
                fecha = v.findViewById(R.id.textoFecha);
            }
        }
    }
}
