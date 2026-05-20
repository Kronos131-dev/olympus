package com.kronos.olympusfront;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.kronos.olympusfront.databinding.ActivityCreateMealPlanBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.MealPlanRequest;
import com.kronos.olympusfront.network.dto.MealPlanResponse;
import com.kronos.olympusfront.network.dto.MealPresetResponse;
import com.kronos.olympusfront.network.dto.PlannedMealEntryRequest;
import com.kronos.olympusfront.network.dto.PlannedMealEntryResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateMealPlanActivity extends AppCompatActivity {

    private static final String TAG = "CreateMealPlan";

    // Libellés affichés et valeurs d'enum correspondantes (mêmes index)
    private static final String[] RECURRENCE_LABELS = {
            "Tous les jours", "Jours de la semaine choisis", "Un jour sur deux", "Personnalisé (tous les N jours)"
    };
    private static final String[] RECURRENCE_VALUES = {
            "DAILY", "SPECIFIC_WEEKDAYS", "EVERY_OTHER_DAY", "CUSTOM"
    };

    private ActivityCreateMealPlanBinding binding;

    private final List<MealPresetResponse> allPresets = new ArrayList<>();
    private final List<MealPresetResponse> selectedPresets = new ArrayList<>();
    // Entrées basées sur un aliment (créées hors de cet écran, ex: agent IA) : préservées à l'édition
    private final List<PlannedMealEntryResponse> preservedFoodEntries = new ArrayList<>();

    private Long editingPlanId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateMealPlanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Spinner de récurrence
        ArrayAdapter<String> recurrenceAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, RECURRENCE_LABELS);
        binding.spinnerRecurrence.setAdapter(recurrenceAdapter);
        binding.spinnerRecurrence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRecurrenceVisibility(RECURRENCE_VALUES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        binding.btnAddMeal.setOnClickListener(v -> showAddMealDialog());
        binding.btnSavePlan.setOnClickListener(v -> savePlan());

        fetchPresets();
        loadEditingPlanIfAny();
    }

    private void updateRecurrenceVisibility(String recurrenceValue) {
        binding.containerWeekdays.setVisibility(
                "SPECIFIC_WEEKDAYS".equals(recurrenceValue) ? View.VISIBLE : View.GONE);
        binding.containerCustom.setVisibility(
                "CUSTOM".equals(recurrenceValue) ? View.VISIBLE : View.GONE);
    }

    private void fetchPresets() {
        RetrofitClient.getApiService().getUserMealPresets().enqueue(new Callback<List<MealPresetResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<MealPresetResponse>> call,
                                   @NonNull Response<List<MealPresetResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPresets.clear();
                    allPresets.addAll(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MealPresetResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Erreur réseau (presets)", t);
            }
        });
    }

    private void loadEditingPlanIfAny() {
        String json = getIntent().getStringExtra(MealPlanActivity.EXTRA_MEAL_PLAN);
        if (json == null) return;

        MealPlanResponse plan = new Gson().fromJson(json, MealPlanResponse.class);
        if (plan == null) return;

        editingPlanId = plan.getId();
        binding.screenTitle.setText("MODIFIER LE PLAN");
        binding.etPlanName.setText(plan.getName());

        // Récurrence
        for (int i = 0; i < RECURRENCE_VALUES.length; i++) {
            if (RECURRENCE_VALUES[i].equals(plan.getRecurrenceType())) {
                binding.spinnerRecurrence.setSelection(i);
                updateRecurrenceVisibility(RECURRENCE_VALUES[i]);
                break;
            }
        }

        applyWeekdaysMask(plan.getWeekdaysMask());

        if (plan.getCustomIntervalDays() != null) {
            binding.etCustomInterval.setText(String.valueOf(plan.getCustomIntervalDays()));
        }

        // Entrées : presets d'un côté, aliments préservés de l'autre
        if (plan.getEntries() != null) {
            for (PlannedMealEntryResponse entry : plan.getEntries()) {
                if (entry.getMealPreset() != null) {
                    selectedPresets.add(entry.getMealPreset());
                } else if (entry.getFoodItem() != null) {
                    preservedFoodEntries.add(entry);
                }
            }
        }
        renderEntries();
    }

    private void applyWeekdaysMask(String mask) {
        if (mask == null) return;
        for (String token : mask.split(",")) {
            String d = token.trim().toUpperCase();
            if (d.startsWith("MON")) binding.cbMon.setChecked(true);
            else if (d.startsWith("TUE")) binding.cbTue.setChecked(true);
            else if (d.startsWith("WED")) binding.cbWed.setChecked(true);
            else if (d.startsWith("THU")) binding.cbThu.setChecked(true);
            else if (d.startsWith("FRI")) binding.cbFri.setChecked(true);
            else if (d.startsWith("SAT")) binding.cbSat.setChecked(true);
            else if (d.startsWith("SUN")) binding.cbSun.setChecked(true);
        }
    }

    private String buildWeekdaysMask() {
        StringBuilder sb = new StringBuilder();
        appendDay(sb, binding.cbMon.isChecked(), "MONDAY");
        appendDay(sb, binding.cbTue.isChecked(), "TUESDAY");
        appendDay(sb, binding.cbWed.isChecked(), "WEDNESDAY");
        appendDay(sb, binding.cbThu.isChecked(), "THURSDAY");
        appendDay(sb, binding.cbFri.isChecked(), "FRIDAY");
        appendDay(sb, binding.cbSat.isChecked(), "SATURDAY");
        appendDay(sb, binding.cbSun.isChecked(), "SUNDAY");
        return sb.toString();
    }

    private void appendDay(StringBuilder sb, boolean checked, String day) {
        if (!checked) return;
        if (sb.length() > 0) sb.append(",");
        sb.append(day);
    }

    private void showAddMealDialog() {
        if (allPresets.isEmpty()) {
            Toast.makeText(this, "Créez d'abord un repas dans l'onglet Repas", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[allPresets.size()];
        for (int i = 0; i < allPresets.size(); i++) {
            names[i] = allPresets.get(i).getName();
        }
        new AlertDialog.Builder(this, R.style.OlympusAlertDialogTheme)
                .setTitle("Ajouter un repas au plan")
                .setItems(names, (dialog, which) -> {
                    selectedPresets.add(allPresets.get(which));
                    renderEntries();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void renderEntries() {
        binding.entriesContainer.removeAllViews();

        for (int i = 0; i < selectedPresets.size(); i++) {
            final int index = i;
            addEntryRow(selectedPresets.get(i).getName(), () -> {
                selectedPresets.remove(index);
                renderEntries();
            });
        }
        for (int i = 0; i < preservedFoodEntries.size(); i++) {
            final int index = i;
            PlannedMealEntryResponse entry = preservedFoodEntries.get(i);
            String label = entry.getFoodItem().getName();
            if (entry.getQuantityGrams() != null) {
                label += " (" + entry.getQuantityGrams().intValue() + "g)";
            }
            addEntryRow(label, () -> {
                preservedFoodEntries.remove(index);
                renderEntries();
            });
        }
    }

    private void addEntryRow(String label, Runnable onRemove) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);
        row.setBackgroundColor(getColor(R.color.surface_container_low));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        row.setLayoutParams(rowParams);

        TextView nameView = new TextView(this);
        nameView.setText(label);
        nameView.setTextColor(getColor(R.color.marble_white));
        nameView.setTextSize(15);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);

        ImageView removeView = new ImageView(this);
        int size = (int) (24 * getResources().getDisplayMetrics().density);
        removeView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        removeView.setImageResource(android.R.drawable.ic_menu_delete);
        removeView.setColorFilter(0xFFD81B60);
        removeView.setOnClickListener(v -> onRemove.run());
        row.addView(removeView);

        binding.entriesContainer.addView(row);
    }

    private void savePlan() {
        String name = binding.etPlanName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Veuillez nommer le plan", Toast.LENGTH_SHORT).show();
            return;
        }

        String recurrenceType = RECURRENCE_VALUES[binding.spinnerRecurrence.getSelectedItemPosition()];

        if (selectedPresets.isEmpty() && preservedFoodEntries.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins un repas au plan", Toast.LENGTH_SHORT).show();
            return;
        }

        MealPlanRequest request = new MealPlanRequest();
        request.setName(name);
        request.setRecurrenceType(recurrenceType);
        request.setActive(true);

        if ("SPECIFIC_WEEKDAYS".equals(recurrenceType)) {
            String mask = buildWeekdaysMask();
            if (mask.isEmpty()) {
                Toast.makeText(this, "Sélectionnez au moins un jour", Toast.LENGTH_SHORT).show();
                return;
            }
            request.setWeekdaysMask(mask);
        } else if ("CUSTOM".equals(recurrenceType)) {
            String intervalStr = binding.etCustomInterval.getText().toString().trim();
            int interval;
            try {
                interval = Integer.parseInt(intervalStr);
            } catch (NumberFormatException e) {
                interval = 0;
            }
            if (interval <= 0) {
                Toast.makeText(this, "Intervalle invalide", Toast.LENGTH_SHORT).show();
                return;
            }
            request.setCustomIntervalDays(interval);
        }

        List<PlannedMealEntryRequest> entries = new ArrayList<>();
        for (MealPresetResponse preset : selectedPresets) {
            PlannedMealEntryRequest entryReq = new PlannedMealEntryRequest();
            entryReq.setMealPresetId(preset.getId());
            entries.add(entryReq);
        }
        for (PlannedMealEntryResponse food : preservedFoodEntries) {
            PlannedMealEntryRequest entryReq = new PlannedMealEntryRequest();
            entryReq.setFoodItemId(food.getFoodItem().getId());
            entryReq.setQuantityGrams(food.getQuantityGrams());
            entryReq.setPlannedTime(food.getPlannedTime());
            entries.add(entryReq);
        }
        request.setEntries(entries);

        binding.btnSavePlan.setEnabled(false);

        Callback<MealPlanResponse> callback = new Callback<MealPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealPlanResponse> call, @NonNull Response<MealPlanResponse> response) {
                binding.btnSavePlan.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(CreateMealPlanActivity.this, "Plan enregistré", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Erreur sauvegarde plan: " + response.code());
                    Toast.makeText(CreateMealPlanActivity.this, "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealPlanResponse> call, @NonNull Throwable t) {
                binding.btnSavePlan.setEnabled(true);
                Log.e(TAG, "Erreur réseau (sauvegarde plan)", t);
                Toast.makeText(CreateMealPlanActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        };

        if (editingPlanId != null) {
            RetrofitClient.getApiService().updateMealPlan(editingPlanId, request).enqueue(callback);
        } else {
            RetrofitClient.getApiService().createMealPlan(request).enqueue(callback);
        }
    }
}
