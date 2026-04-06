package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kronos.olympusfront.databinding.FragmentProfileBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.TokenManager;
import com.kronos.olympusfront.network.dto.UserResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchProfile();

        binding.editBiometricsButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Édition des biométriques (À venir)", Toast.LENGTH_SHORT).show();
        });

        binding.logoutButton.setOnClickListener(v -> {
            if (getContext() != null) {
                TokenManager tokenManager = new TokenManager(getContext());
                tokenManager.clearToken();
                
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void fetchProfile() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Log.e(TAG, "Erreur récupération profil: " + response.code());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Impossible de récupérer le profil", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Erreur réseau (profil)", t);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(UserResponse user) {
        if (!isAdded() || binding == null) return;

        // Nom
        String name = user.getEmail().split("@")[0].toUpperCase();
        binding.profileName.setText("LÉGIONNAIRE\n" + name);

        // Vocation / Activité
        String goalText = user.getGoal().replace("_", " ");
        binding.profileObjective.setText(goalText);
        binding.profileActivity.setText(user.getActivityLevel());

        // Mensurations
        binding.profileHeight.setText(String.valueOf(user.getHeightCm().intValue()));
        binding.profileWeight.setText(String.valueOf(user.getCurrentWeightKg()));
        
        String natureText = user.getGender().equals("MALE") ? "MASCULIN" : "FÉMININ";
        binding.profileNature.setText(natureText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}