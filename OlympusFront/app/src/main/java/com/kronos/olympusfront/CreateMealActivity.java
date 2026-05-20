package com.kronos.olympusfront;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.kronos.olympusfront.databinding.ActivityCreateMealBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.AiMealRequest;
import com.kronos.olympusfront.network.dto.FoodItemRequest;
import com.kronos.olympusfront.network.dto.FoodItemResponse;
import com.kronos.olympusfront.network.dto.MealIngredientRequest;
import com.kronos.olympusfront.network.dto.MealIngredientResponse;
import com.kronos.olympusfront.network.dto.MealPresetRequest;
import com.kronos.olympusfront.network.dto.MealPresetResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateMealActivity extends AppCompatActivity {

    private static final String TAG = "CreateMealActivity";
    private ActivityCreateMealBinding binding;

    private FoodSearchAdapter searchAdapter;
    private SelectedIngredientsAdapter ingredientsAdapter;

    private Long editingMealId = null; // null if creating a new meal
    private boolean isIngredientsListExpanded = false;
    private EditText activeAiInput;

    // Register the launcher and result handler for barcode scanning
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(CreateMealActivity.this, "Scan annulé", Toast.LENGTH_LONG).show();
                } else {
                    String barcode = result.getContents();
                    binding.etBarcode.setText(barcode);
                    performBarcodeSearch(barcode);
                }
            });

    // Register launcher for Speech to Text
    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty() && activeAiInput != null) {
                        String currentText = activeAiInput.getText().toString();
                        String newText = matches.get(0);
                        if (!currentText.isEmpty()) {
                            activeAiInput.setText(currentText + " " + newText);
                        } else {
                            activeAiInput.setText(newText);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateMealBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Search Recycler
        searchAdapter = new FoodSearchAdapter((foodItem, quantity) -> {
            ingredientsAdapter.addIngredient(new SelectedIngredientsAdapter.SelectedIngredient(foodItem, quantity));
            updateEmptyState();
            Toast.makeText(this, foodItem.getName() + " ajouté (" + quantity + "g)", Toast.LENGTH_SHORT).show();
            // Clear search results after adding
            searchAdapter.setFoodItems(new ArrayList<>());
            binding.etSearchFood.setText("");
            binding.etBarcode.setText("");
        });
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSearchResults.setAdapter(searchAdapter);

        // Setup Selected Ingredients Recycler
        ingredientsAdapter = new SelectedIngredientsAdapter(position -> {
            ingredientsAdapter.removeIngredient(position);
            updateEmptyState();
        });
        binding.recyclerSelectedIngredients.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSelectedIngredients.setAdapter(ingredientsAdapter);
        updateEmptyState();
        
        // Handle expand/collapse for selected ingredients
        binding.btnToggleIngredients.setOnClickListener(v -> {
            isIngredientsListExpanded = !isIngredientsListExpanded;
            if (isIngredientsListExpanded) {
                binding.btnToggleIngredients.setRotation(180); // Flèche vers le haut
                binding.recyclerSelectedIngredients.setVisibility(View.GONE);
            } else {
                binding.btnToggleIngredients.setRotation(0); // Flèche vers le bas
                binding.recyclerSelectedIngredients.setVisibility(View.VISIBLE);
            }
        });

        // Search Button (Text)
        binding.btnSearch.setOnClickListener(v -> performTextSearch());

        // Scan Button (Camera)
        binding.btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Scannez un code-barre d'aliment");
            options.setCameraId(0);  // Use a specific camera of the device
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });

        // Search automatically when text changes in the search bar
        binding.etSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 3) { // Trigger search when at least 3 characters are typed
                    performTextSearch();
                } else if (s.length() == 0) {
                    searchAdapter.setFoodItems(new ArrayList<>());
                }
            }
        });
        
        // Bouton pour créer un aliment manuellement
        binding.btnCustomMacros.setOnClickListener(v -> showCreateCustomFoodDialog());
        
        // Bouton pour analyser un repas avec l'IA
        if(binding.btnAiFood != null) {
            binding.btnAiFood.setOnClickListener(v -> showAiMealDialog());
        }

        // Save Button
        binding.btnSaveMeal.setOnClickListener(v -> saveMeal());

        // Vérifier si on est en mode édition
        if (getIntent().hasExtra("EXTRA_MEAL_PRESET")) {
            String mealJson = getIntent().getStringExtra("EXTRA_MEAL_PRESET");
            MealPresetResponse editingMeal = new Gson().fromJson(mealJson, MealPresetResponse.class);
            if (editingMeal != null) {
                editingMealId = editingMeal.getId();
                populateForEditing(editingMeal);
            }
        }
    }

    private void populateForEditing(MealPresetResponse meal) {
        binding.etMealName.setText(meal.getName());
        binding.btnSaveMeal.setText("METTRE À JOUR LE REPAS");

        if (meal.getIngredients() != null) {
            for (MealIngredientResponse ingredient : meal.getIngredients()) {
                if (ingredient.getFoodItem() != null && ingredient.getQuantityGrams() != null) {
                    ingredientsAdapter.addIngredient(new SelectedIngredientsAdapter.SelectedIngredient(
                            ingredient.getFoodItem(),
                            ingredient.getQuantityGrams()
                    ));
                }
            }
            updateEmptyState();
        }
    }

    private void performTextSearch() {
        String query = binding.etSearchFood.getText().toString().trim();
        if (query.isEmpty()) return;

        RetrofitClient.getApiService().searchFoodItemsByName(query).enqueue(new Callback<List<FoodItemResponse>>() {
            @Override
            public void onResponse(Call<List<FoodItemResponse>> call, Response<List<FoodItemResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    searchAdapter.setFoodItems(response.body());
                } else {
                    searchAdapter.setFoodItems(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<FoodItemResponse>> call, Throwable t) {
                Log.e(TAG, "Search error", t);
            }
        });
    }

    private void performBarcodeSearch(String barcode) {
        if (barcode == null || barcode.isEmpty()) return;

        RetrofitClient.getApiService().getFoodItemByBarcode(barcode).enqueue(new Callback<FoodItemResponse>() {
            @Override
            public void onResponse(Call<FoodItemResponse> call, Response<FoodItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FoodItemResponse> result = new ArrayList<>();
                    result.add(response.body());
                    searchAdapter.setFoodItems(result);
                    
                    // Hide keyboard after barcode search
                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                } else {
                    Toast.makeText(CreateMealActivity.this, "Code-barre non trouvé", Toast.LENGTH_SHORT).show();
                    searchAdapter.setFoodItems(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<FoodItemResponse> call, Throwable t) {
                Log.e(TAG, "Barcode search error", t);
                Toast.makeText(CreateMealActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showCreateCustomFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.OlympusAlertDialogTheme);
        builder.setTitle("Créer un aliment personnalisé");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_food, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_custom_name);
        EditText etKcal = dialogView.findViewById(R.id.et_custom_kcal);
        EditText etProt = dialogView.findViewById(R.id.et_custom_prot);
        EditText etCarbs = dialogView.findViewById(R.id.et_custom_carbs);
        EditText etFats = dialogView.findViewById(R.id.et_custom_fats);

        builder.setPositiveButton("Créer et Ajouter", (dialog, which) -> {
            try {
                String name = etName.getText().toString();
                double kcal = Double.parseDouble(etKcal.getText().toString());
                double prot = Double.parseDouble(etProt.getText().toString());
                double carbs = Double.parseDouble(etCarbs.getText().toString());
                double fats = Double.parseDouble(etFats.getText().toString());

                if (name.isEmpty()) {
                    Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show();
                    return;
                }

                FoodItemRequest newFood = new FoodItemRequest(name, kcal, prot, carbs, fats);

                RetrofitClient.getApiService().createManualFoodItem(newFood).enqueue(new Callback<FoodItemResponse>() {
                    @Override
                    public void onResponse(Call<FoodItemResponse> call, Response<FoodItemResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // On affiche l'aliment créé dans la liste pour le consommer
                            List<FoodItemResponse> result = new ArrayList<>();
                            result.add(response.body());
                            searchAdapter.setFoodItems(result);
                            Toast.makeText(CreateMealActivity.this, "Aliment créé ! Entrez la quantité à consommer.", Toast.LENGTH_LONG).show();
                            
                            // Hide keyboard
                            View view = getCurrentFocus();
                            if (view != null) {
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                        } else {
                            Toast.makeText(CreateMealActivity.this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<FoodItemResponse> call, Throwable t) {
                        Toast.makeText(CreateMealActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Veuillez entrer des nombres valides", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }
    
    private void showAiMealDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.OlympusAlertDialogTheme);
        builder.setTitle("Analyse de Repas IA");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_food, null);
        builder.setView(dialogView);

        EditText etDescription = dialogView.findViewById(R.id.et_ai_description);
        View progressBar = dialogView.findViewById(R.id.ai_progress);
        View btnMic = dialogView.findViewById(R.id.btn_mic);
        
        btnMic.setOnClickListener(v -> {
            activeAiInput = etDescription;
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Décrivez votre repas (ex: 8 sushis saumon, 1 bol de riz...)");
            try {
                speechLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "La reconnaissance vocale n'est pas supportée sur cet appareil", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setPositiveButton("Analyser", null); // We set it to null initially to override default closing behavior
        builder.setNegativeButton("Annuler", null);
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String description = etDescription.getText().toString().trim();
                
                if (description.isEmpty()) {
                    Toast.makeText(this, "Veuillez décrire votre repas", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Show loading
                progressBar.setVisibility(View.VISIBLE);
                etDescription.setEnabled(false);
                btnMic.setEnabled(false);
                button.setEnabled(false);
                
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etDescription.getWindowToken(), 0);
                }
                
                AiMealRequest request = new AiMealRequest(description);
                
                RetrofitClient.getApiService().analyzeMealWithAi(request).enqueue(new Callback<FoodItemResponse>() {
                    @Override
                    public void onResponse(Call<FoodItemResponse> call, Response<FoodItemResponse> response) {
                        dialog.dismiss();
                        
                        if (response.isSuccessful() && response.body() != null) {
                            List<FoodItemResponse> result = new ArrayList<>();
                            result.add(response.body());
                            searchAdapter.setFoodItems(result);
                            Toast.makeText(CreateMealActivity.this, "Repas analysé par l'IA ! Saisissez 100g pour ajouter la totalité.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(CreateMealActivity.this, "L'IA n'a pas pu analyser ce repas.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<FoodItemResponse> call, Throwable t) {
                        dialog.dismiss();
                        Toast.makeText(CreateMealActivity.this, "Erreur de connexion avec l'IA", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }


    private void saveMeal() {
        String mealName = binding.etMealName.getText().toString().trim();
        if (mealName.isEmpty()) {
            Toast.makeText(this, "Veuillez donner un nom au repas", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SelectedIngredientsAdapter.SelectedIngredient> selected = ingredientsAdapter.getIngredients();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins un ingrédient", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveMeal.setEnabled(false);

        List<MealIngredientRequest> ingredientRequests = new ArrayList<>();
        for (SelectedIngredientsAdapter.SelectedIngredient item : selected) {
            ingredientRequests.add(new MealIngredientRequest(item.foodItem.getId(), item.quantityGrams));
        }

        MealPresetRequest request = new MealPresetRequest(mealName, ingredientRequests);

        if (editingMealId != null) {
            // Mode Update
            RetrofitClient.getApiService().updateMealPreset(editingMealId, request).enqueue(new Callback<MealPresetResponse>() {
                @Override
                public void onResponse(Call<MealPresetResponse> call, Response<MealPresetResponse> response) {
                    binding.btnSaveMeal.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateMealActivity.this, "Repas mis à jour !", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.e(TAG, "Error updating meal: " + response.code());
                        Toast.makeText(CreateMealActivity.this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<MealPresetResponse> call, Throwable t) {
                    binding.btnSaveMeal.setEnabled(true);
                    Log.e(TAG, "Network error updating meal", t);
                    Toast.makeText(CreateMealActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Mode Création
            RetrofitClient.getApiService().createMealPreset(request).enqueue(new Callback<MealPresetResponse>() {
                @Override
                public void onResponse(Call<MealPresetResponse> call, Response<MealPresetResponse> response) {
                    binding.btnSaveMeal.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateMealActivity.this, "Repas sauvegardé !", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.e(TAG, "Error saving meal: " + response.code());
                        Toast.makeText(CreateMealActivity.this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<MealPresetResponse> call, Throwable t) {
                    binding.btnSaveMeal.setEnabled(true);
                    Log.e(TAG, "Network error saving meal", t);
                    Toast.makeText(CreateMealActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateEmptyState() {
        if (ingredientsAdapter.getItemCount() == 0) {
            binding.tvEmptyIngredients.setVisibility(View.VISIBLE);
            binding.recyclerSelectedIngredients.setVisibility(View.GONE);
            binding.btnToggleIngredients.setVisibility(View.GONE);
        } else {
            binding.tvEmptyIngredients.setVisibility(View.GONE);

            // On affiche TOUJOURS le bouton dès qu'il y a au moins 1 ingrédient
            binding.btnToggleIngredients.setVisibility(View.VISIBLE);

            // Respect toggle state (false par défaut = liste visible / descendue)
            if (isIngredientsListExpanded) {
                binding.recyclerSelectedIngredients.setVisibility(View.GONE);
            } else {
                binding.recyclerSelectedIngredients.setVisibility(View.VISIBLE);
            }
        }
    }
}