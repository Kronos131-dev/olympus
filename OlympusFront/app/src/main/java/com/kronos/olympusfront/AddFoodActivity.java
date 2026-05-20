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

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.kronos.olympusfront.databinding.ActivityAddFoodBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.AiMealRequest;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.FoodItemRequest;
import com.kronos.olympusfront.network.dto.FoodItemResponse;
import com.kronos.olympusfront.network.dto.LogEntryRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddFoodActivity extends AppCompatActivity {

    private static final String TAG = "AddFoodActivity";
    private ActivityAddFoodBinding binding;
    private FoodSearchAdapter adapter;
    private EditText activeAiInput;

    // Register the launcher and result handler for barcode scanning
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(AddFoodActivity.this, "Scan annulé", Toast.LENGTH_LONG).show();
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
        binding = ActivityAddFoodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new FoodSearchAdapter((foodItem, quantity) -> logFoodItem(foodItem, quantity));
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSearchResults.setAdapter(adapter);

        binding.btnSearch.setOnClickListener(v -> performTextSearch());
        
        // Launch camera for scanning
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
                    adapter.setFoodItems(new ArrayList<>());
                }
            }
        });

        // Bouton pour créer un aliment manuellement
        binding.btnCustomMacros.setOnClickListener(v -> showCreateCustomFoodDialog());
        
        // Bouton pour analyser un repas avec l'IA
        if (binding.btnAiFood != null) {
            binding.btnAiFood.setOnClickListener(v -> showAiMealDialog());
        }
    }

    private void performTextSearch() {
        String query = binding.etSearchFood.getText().toString().trim();
        if (query.isEmpty()) return;

        RetrofitClient.getApiService().searchFoodItemsByName(query).enqueue(new Callback<List<FoodItemResponse>>() {
            @Override
            public void onResponse(Call<List<FoodItemResponse>> call, Response<List<FoodItemResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setFoodItems(response.body());
                } else {
                    adapter.setFoodItems(new ArrayList<>());
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
                    adapter.setFoodItems(result);
                    
                    // Hide keyboard after barcode search
                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                } else {
                    Toast.makeText(AddFoodActivity.this, "Code-barre non trouvé", Toast.LENGTH_SHORT).show();
                    adapter.setFoodItems(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<FoodItemResponse> call, Throwable t) {
                Log.e(TAG, "Barcode search error", t);
                Toast.makeText(AddFoodActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
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
                            adapter.setFoodItems(result);
                            Toast.makeText(AddFoodActivity.this, "Aliment créé ! Entrez la quantité à consommer.", Toast.LENGTH_LONG).show();
                            
                            // Hide keyboard
                            View view = getCurrentFocus();
                            if (view != null) {
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                        } else {
                            Toast.makeText(AddFoodActivity.this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<FoodItemResponse> call, Throwable t) {
                        Toast.makeText(AddFoodActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
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
                            adapter.setFoodItems(result);
                            Toast.makeText(AddFoodActivity.this, "Repas analysé par l'IA ! Confirmez la quantité.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AddFoodActivity.this, "L'IA n'a pas pu analyser ce repas.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<FoodItemResponse> call, Throwable t) {
                        dialog.dismiss();
                        // Ajoute cette ligne pour voir l'erreur exacte dans le Logcat (ex: MalformedJsonException)
                        Log.e("AiMealDialog", "Erreur de l'appel IA ou de parsing JSON : ", t);
                        Toast.makeText(AddFoodActivity.this, "Erreur de connexion avec l'IA", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void logFoodItem(FoodItemResponse foodItem, double quantity) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        LogEntryRequest request = new LogEntryRequest();
        request.setTargetDate(today);
        request.setFoodItemId(foodItem.getId());
        request.setQuantityGrams(quantity);

        RetrofitClient.getApiService().addLogEntry(request).enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddFoodActivity.this, quantity + "g de " + foodItem.getName() + " ajouté !", Toast.LENGTH_SHORT).show();
                    finish(); // Retourne à l'accueil
                } else {
                    Toast.makeText(AddFoodActivity.this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                Toast.makeText(AddFoodActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}