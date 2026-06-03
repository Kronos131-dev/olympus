package com.kronos.olympusfront;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kronos.olympusfront.databinding.FragmentPlanBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.FoodItemResponse;
import com.kronos.olympusfront.network.dto.MealPlanGenerationRequest;
import com.kronos.olympusfront.network.dto.MealPlanResponse;
import com.kronos.olympusfront.network.dto.MealPresetResponse;
import com.kronos.olympusfront.network.dto.PlannedMealEntryRequest;
import com.kronos.olympusfront.network.dto.PlannedMealEntryResponse;
import com.kronos.olympusfront.network.dto.WeeklyPlanRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Emploi du temps hebdomadaire : 7 cartes-jour, chacune listant les repas planifiés
 * de ce jour. L'utilisateur peut ajouter/retirer des repas ou laisser l'Oracle générer
 * la semaine entière.
 */
public class PlanFragment extends Fragment {

    private static final String TAG = "PlanFragment";

    private static final String[] DAYS =
            {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
    private static final String[] DAY_LABELS =
            {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};

    private FragmentPlanBinding binding;
    private final List<PlannedMealEntryResponse> entries = new ArrayList<>();
    private List<MealPresetResponse> presets = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnGeneratePlan.setOnClickListener(v -> showGenerateDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchPresets();
        loadWeeklyPlan();
    }

    private void fetchPresets() {
        RetrofitClient.getApiService().getUserMealPresets()
                .enqueue(new Callback<List<MealPresetResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<MealPresetResponse>> call,
                                           @NonNull Response<List<MealPresetResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            presets = response.body();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<MealPresetResponse>> call, @NonNull Throwable t) {
                        // Les presets ne sont pas indispensables : on ignore silencieusement.
                    }
                });
    }

    private void loadWeeklyPlan() {
        setLoading(true);
        RetrofitClient.getApiService().getWeeklyPlan().enqueue(new Callback<MealPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealPlanResponse> call,
                                   @NonNull Response<MealPlanResponse> response) {
                if (binding == null) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    applyResponse(response.body());
                } else {
                    toast("Erreur de chargement du planning (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealPlanResponse> call, @NonNull Throwable t) {
                if (binding == null) return;
                setLoading(false);
                toast("Erreur réseau");
            }
        });
    }

    private void applyResponse(MealPlanResponse plan) {
        entries.clear();
        if (plan.getEntries() != null) {
            entries.addAll(plan.getEntries());
        }
        renderWeek();
    }

    private void renderWeek() {
        if (binding == null) return;
        LinearLayout container = binding.daysContainer;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < DAYS.length; i++) {
            final String day = DAYS[i];
            View card = inflater.inflate(R.layout.item_plan_day, container, false);
            ((TextView) card.findViewById(R.id.day_name)).setText(DAY_LABELS[i]);

            LinearLayout mealsContainer = card.findViewById(R.id.meals_container);
            TextView empty = card.findViewById(R.id.day_empty);

            List<PlannedMealEntryResponse> dayEntries = entriesForDay(day);
            empty.setVisibility(dayEntries.isEmpty() ? View.VISIBLE : View.GONE);

            for (final PlannedMealEntryResponse entry : dayEntries) {
                View row = inflater.inflate(R.layout.item_plan_meal, mealsContainer, false);
                ((TextView) row.findViewById(R.id.meal_name)).setText(displayName(entry));
                ImageButton remove = row.findViewById(R.id.btn_remove_meal);
                remove.setOnClickListener(v -> {
                    entries.remove(entry);
                    saveWeek();
                });
                mealsContainer.addView(row);
            }

            card.findViewById(R.id.btn_add_meal).setOnClickListener(v -> showAddMealDialog(day));
            container.addView(card);
        }
    }

    private List<PlannedMealEntryResponse> entriesForDay(String day) {
        List<PlannedMealEntryResponse> result = new ArrayList<>();
        for (PlannedMealEntryResponse e : entries) {
            if (day.equals(e.getDayOfWeek())) {
                result.add(e);
            }
        }
        return result;
    }

    private String displayName(PlannedMealEntryResponse entry) {
        if (entry.getFoodItem() != null) {
            String name = entry.getFoodItem().getName();
            if (entry.getQuantityGrams() != null) {
                return name + "  ·  " + Math.round(entry.getQuantityGrams()) + " g";
            }
            return name;
        }
        if (entry.getMealPreset() != null) {
            return entry.getMealPreset().getName();
        }
        return "Repas";
    }

    private void showAddMealDialog(String day) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
                .setTitle("Ajouter — " + dayLabel(day))
                .setItems(new String[]{"🍽️  Un de mes repas enregistrés",
                                "🔍  Rechercher un aliment"},
                        (dialog, which) -> {
                            if (which == 0) {
                                pickPreset(day);
                            } else {
                                searchFoodDialog(day);
                            }
                        })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void pickPreset(String day) {
        if (getContext() == null) return;
        if (presets.isEmpty()) {
            toast("Aucun repas enregistré. Créez-en un dans l'onglet Repas.");
            return;
        }
        String[] names = new String[presets.size()];
        for (int i = 0; i < presets.size(); i++) {
            names[i] = presets.get(i).getName();
        }
        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
                .setTitle("Mes repas enregistrés")
                .setItems(names, (dialog, which) -> {
                    PlannedMealEntryResponse entry = new PlannedMealEntryResponse();
                    entry.setMealPreset(presets.get(which));
                    entry.setDayOfWeek(day);
                    entries.add(entry);
                    saveWeek();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void searchFoodDialog(String day) {
        if (getContext() == null) return;
        final EditText input = styledEditText("Nom de l'aliment…", false);
        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
                .setTitle("Rechercher un aliment")
                .setView(input)
                .setPositiveButton("Rechercher", (dialog, which) ->
                        runFoodSearch(day, input.getText().toString().trim()))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void runFoodSearch(String day, String query) {
        if (query.isEmpty()) {
            return;
        }
        setLoading(true);
        RetrofitClient.getApiService().searchFoodItemsByName(query)
                .enqueue(new Callback<List<FoodItemResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<FoodItemResponse>> call,
                                           @NonNull Response<List<FoodItemResponse>> response) {
                        if (binding == null) return;
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {
                            showFoodResults(day, response.body());
                        } else {
                            toast("Aucun aliment trouvé pour « " + query + " »");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<FoodItemResponse>> call,
                                          @NonNull Throwable t) {
                        if (binding == null) return;
                        setLoading(false);
                        toast("Erreur réseau");
                    }
                });
    }

    private void showFoodResults(String day, List<FoodItemResponse> results) {
        if (getContext() == null) return;
        int limit = Math.min(results.size(), 25);
        final List<FoodItemResponse> shown = new ArrayList<>(results.subList(0, limit));
        String[] names = new String[shown.size()];
        for (int i = 0; i < shown.size(); i++) {
            FoodItemResponse food = shown.get(i);
            int kcal = food.getKcal100g() != null ? (int) Math.round(food.getKcal100g()) : 0;
            names[i] = food.getName() + "  ·  " + kcal + " kcal/100g";
        }
        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
                .setTitle("Résultats")
                .setItems(names, (dialog, which) -> askQuantityThenAdd(day, shown.get(which)))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void askQuantityThenAdd(String day, FoodItemResponse food) {
        if (getContext() == null) return;
        final EditText input = styledEditText("Quantité en grammes", true);
        input.setText("100");
        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
                .setTitle(food.getName())
                .setView(input)
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    double qty;
                    try {
                        qty = Double.parseDouble(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        qty = 100.0;
                    }
                    if (qty <= 0) {
                        qty = 100.0;
                    }
                    PlannedMealEntryResponse entry = new PlannedMealEntryResponse();
                    entry.setFoodItem(food);
                    entry.setQuantityGrams(qty);
                    entry.setDayOfWeek(day);
                    entries.add(entry);
                    saveWeek();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showGenerateDialog() {
        if (getContext() == null) return;
        final EditText input = styledEditText("Objectif, allergies, préférences… (optionnel)", false);
        input.setMinLines(2);
        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
                .setTitle("Générer ma semaine avec l'Oracle")
                .setView(input)
                .setPositiveButton("Générer", (dialog, which) ->
                        generate(input.getText().toString().trim()))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private EditText styledEditText(String hint, boolean numeric) {
        EditText input = new EditText(getContext());
        input.setHint(hint);
        input.setHintTextColor(ContextCompat.getColor(getContext(), R.color.outline_variant));
        input.setTextColor(ContextCompat.getColor(getContext(), R.color.marble_white));
        if (numeric) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        return input;
    }

    private void generate(String prompt) {
        setLoading(true);
        toast("L'Oracle prépare votre semaine…");
        RetrofitClient.getApiService().generateMealPlan(new MealPlanGenerationRequest(prompt))
                .enqueue(new Callback<MealPlanResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealPlanResponse> call,
                                           @NonNull Response<MealPlanResponse> response) {
                        if (binding == null) return;
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            applyResponse(response.body());
                            toast("Planning généré !");
                        } else {
                            toast("L'Oracle n'a pas pu générer le planning (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealPlanResponse> call, @NonNull Throwable t) {
                        if (binding == null) return;
                        setLoading(false);
                        toast("Erreur réseau");
                    }
                });
    }

    private void saveWeek() {
        setLoading(true);
        List<PlannedMealEntryRequest> requests = new ArrayList<>();
        for (PlannedMealEntryResponse entry : entries) {
            PlannedMealEntryRequest request = new PlannedMealEntryRequest();
            request.setDayOfWeek(entry.getDayOfWeek());
            if (entry.getFoodItem() != null) {
                request.setFoodItemId(entry.getFoodItem().getId());
                request.setQuantityGrams(entry.getQuantityGrams() != null
                        ? entry.getQuantityGrams() : 100.0);
            } else if (entry.getMealPreset() != null) {
                request.setMealPresetId(entry.getMealPreset().getId());
            }
            requests.add(request);
        }
        RetrofitClient.getApiService().saveWeeklyPlan(new WeeklyPlanRequest(requests))
                .enqueue(new Callback<MealPlanResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealPlanResponse> call,
                                           @NonNull Response<MealPlanResponse> response) {
                        if (binding == null) return;
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            applyResponse(response.body());
                        } else {
                            toast("Erreur d'enregistrement (" + response.code() + ")");
                            loadWeeklyPlan();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealPlanResponse> call, @NonNull Throwable t) {
                        if (binding == null) return;
                        setLoading(false);
                        toast("Erreur réseau");
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (binding != null) {
            binding.planLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private String dayLabel(String day) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) {
                return DAY_LABELS[i];
            }
        }
        return day;
    }

    private void toast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
