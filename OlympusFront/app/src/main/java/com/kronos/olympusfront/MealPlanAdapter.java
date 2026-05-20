package com.kronos.olympusfront;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kronos.olympusfront.databinding.ItemMealPlanBinding;
import com.kronos.olympusfront.network.dto.MealPlanResponse;

import java.util.ArrayList;
import java.util.List;

public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.PlanViewHolder> {

    public interface OnPlanActionListener {
        void onEditPlan(MealPlanResponse plan);
        void onDeletePlan(MealPlanResponse plan);
    }

    private List<MealPlanResponse> plans = new ArrayList<>();
    private final OnPlanActionListener listener;

    public MealPlanAdapter(OnPlanActionListener listener) {
        this.listener = listener;
    }

    public void setPlans(List<MealPlanResponse> plans) {
        this.plans = plans != null ? plans : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMealPlanBinding binding = ItemMealPlanBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PlanViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        holder.bind(plans.get(position));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    /** Décrit la récurrence d'un plan en français lisible. */
    public static String describeRecurrence(MealPlanResponse plan) {
        if (plan.getRecurrenceType() == null) return "";
        switch (plan.getRecurrenceType()) {
            case "DAILY":
                return "Tous les jours";
            case "EVERY_OTHER_DAY":
                return "Un jour sur deux";
            case "CUSTOM":
                Integer n = plan.getCustomIntervalDays();
                return "Tous les " + (n != null ? n : "?") + " jours";
            case "SPECIFIC_WEEKDAYS":
                return describeWeekdays(plan.getWeekdaysMask());
            default:
                return plan.getRecurrenceType();
        }
    }

    private static String describeWeekdays(String mask) {
        if (mask == null || mask.trim().isEmpty()) return "Certains jours";
        StringBuilder sb = new StringBuilder();
        for (String token : mask.split(",")) {
            String d = token.trim().toUpperCase();
            String fr;
            if (d.startsWith("MON")) fr = "Lun";
            else if (d.startsWith("TUE")) fr = "Mar";
            else if (d.startsWith("WED")) fr = "Mer";
            else if (d.startsWith("THU")) fr = "Jeu";
            else if (d.startsWith("FRI")) fr = "Ven";
            else if (d.startsWith("SAT")) fr = "Sam";
            else if (d.startsWith("SUN")) fr = "Dim";
            else continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(fr);
        }
        return sb.length() > 0 ? sb.toString() : "Certains jours";
    }

    class PlanViewHolder extends RecyclerView.ViewHolder {
        private final ItemMealPlanBinding binding;

        public PlanViewHolder(@NonNull ItemMealPlanBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MealPlanResponse plan) {
            binding.planName.setText(plan.getName());
            binding.planRecurrence.setText(describeRecurrence(plan));

            int count = plan.getEntries() != null ? plan.getEntries().size() : 0;
            binding.planSummary.setText(count + (count > 1 ? " éléments planifiés" : " élément planifié"));

            binding.planCard.setOnClickListener(v -> {
                if (listener != null) listener.onEditPlan(plan);
            });
            binding.btnDeletePlan.setOnClickListener(v -> {
                if (listener != null) listener.onDeletePlan(plan);
            });
        }
    }
}
