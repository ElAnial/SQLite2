package com.example.sqlite;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptador de RecyclerView para inflar y visualizar los ítems del Historial de Auditoría.
 */
public class RegistrosAdapter extends RecyclerView.Adapter<RegistrosAdapter.RegistroViewHolder> {

    private final Context context;
    private final List<RegistroAudit> listaRegistros;

    public RegistrosAdapter(Context context, List<RegistroAudit> listaRegistros) {
        this.context = context;
        this.listaRegistros = listaRegistros;
    }

    @NonNull
    @Override
    public RegistroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_registro, parent, false);
        return new RegistroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RegistroViewHolder holder, int position) {
        RegistroAudit registro = listaRegistros.get(position);

        holder.tvDetalle.setText(registro.getDetalle());
        holder.tvUsuario.setText("Usuario: " + registro.getUsuario());
        holder.tvFecha.setText(registro.getFechaHora());

        String tipo = registro.getTipoAccion() != null ? registro.getTipoAccion().toUpperCase() : "GENERAL";
        holder.tvTagAccion.setText(tipo);

        // Estilo dinámico según la acción (ENTRADA, SALIDA, DESHABILITADO, CATEGORÍA, PROVEEDOR, etc.)
        configurarEstiloTag(holder.tvTagAccion, tipo);
    }

    @Override
    public int getItemCount() {
        return listaRegistros != null ? listaRegistros.size() : 0;
    }

    /**
     * Aplica colores y bordes redondeados (Pill badge) al Tag de la acción.
     */
    private void configurarEstiloTag(TextView tvTag, String tipo) {
        int bgColor;
        int textColor;

        switch (tipo) {
            case "ENTRADA":
                bgColor = Color.parseColor("#E8F5E9"); // Verde claro
                textColor = Color.parseColor("#2E7D32"); // Verde oscuro
                break;
            case "SALIDA":
                bgColor = Color.parseColor("#FFEBEE"); // Rojo claro
                textColor = Color.parseColor("#C62828"); // Rojo oscuro
                break;
            case "DESHABILITADO":
                bgColor = Color.parseColor("#ECEFF1"); // Gris azulado claro
                textColor = Color.parseColor("#455A64"); // Gris azulado oscuro
                break;
            case "CATEGORÍA":
            case "CATEGORIA":
                bgColor = Color.parseColor("#E3F2FD"); // Azul claro
                textColor = Color.parseColor("#1565C0"); // Azul oscuro
                break;
            case "PROVEEDOR":
                bgColor = Color.parseColor("#F3E5F5"); // Morado claro
                textColor = Color.parseColor("#6A1B9A"); // Morado oscuro
                break;
            default: // PRODUCTO, CREADO, GENERAL, etc.
                bgColor = Color.parseColor("#E0F7FA"); // Cyan claro
                textColor = Color.parseColor("#00838F"); // Cyan oscuro
                break;
        }

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(16f); // Bordes redondeados formato pill
        shape.setColor(bgColor);

        tvTag.setBackground(shape);
        tvTag.setTextColor(textColor);
    }

    public static class RegistroViewHolder extends RecyclerView.ViewHolder {
        TextView tvTagAccion, tvFecha, tvDetalle, tvUsuario;

        public RegistroViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagAccion = itemView.findViewById(R.id.tvTagAccion);
            tvFecha = itemView.findViewById(R.id.tvFechaRegistro);
            tvDetalle = itemView.findViewById(R.id.tvDetalleRegistro);
            tvUsuario = itemView.findViewById(R.id.tvUsuarioRegistro);
        }
    }
}
