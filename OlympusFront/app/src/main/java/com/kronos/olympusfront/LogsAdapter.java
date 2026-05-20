package com.kronos.olympusfront;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kronos.olympusfront.databinding.ItemLogEntryBinding;
import com.kronos.olympusfront.network.dto.LogEntryResponse;

import java.util.ArrayList;
import java.util.List;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {

    private List<LogEntryResponse> logs = new ArrayList<>();
    private final OnLogDeleteListener listener;

    public interface OnLogDeleteListener {
        void onDeleteClick(LogEntryResponse log);
    }

    public LogsAdapter(OnLogDeleteListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<LogEntryResponse> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLogEntryBinding binding = ItemLogEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntryResponse log = logs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        private final ItemLogEntryBinding binding;

        public LogViewHolder(@NonNull ItemLogEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LogEntryResponse log) {
            String name = "";
            double kcal = 0.0;
            String details = "";

            if (log.getMealPreset() != null) {
                name = log.getMealPreset().getName();
                kcal = log.getMealPreset().getTotalKcal() != null ? log.getMealPreset().getTotalKcal() : 0.0;
                details = "REPAS";
            } else if (log.getFoodItem() != null) {
                name = log.getFoodItem().getName();
                // Calcul basé sur 100g
                double kcal100g = log.getFoodItem().getKcal100g() != null ? log.getFoodItem().getKcal100g() : 0.0;
                double quantity = log.getQuantityGrams() != null ? log.getQuantityGrams() : 0.0;
                kcal = (kcal100g * quantity) / 100.0;
                details = quantity + "g";
            }

            // Badge "planifié" pour les entrées issues d'un plan de repas
            if (Boolean.TRUE.equals(log.getFromPlan())) {
                details = details.isEmpty() ? "PLANIFIÉ" : details + " · PLANIFIÉ";
            }

            binding.itemName.setText(name);
            binding.itemDetails.setText(details);
            binding.itemKcal.setText((int) kcal + "\nKCAL");

            binding.btnDeleteLog.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(log);
                }
            });
        }
    }
}