package com.kronos.olympusfront;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kronos.olympusfront.databinding.FragmentChatBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.ChatMessageDto;
import com.kronos.olympusfront.network.dto.ChatResponseDto;
import com.kronos.olympusfront.network.dto.ConversationSummaryDto;
import com.kronos.olympusfront.network.dto.UserResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private FragmentChatBinding binding;
    private ChatAdapter adapter;
    private Long conversationId;
    private Uri pendingImageUri;
    private UserResponse currentUser;

    // Reconnaissance vocale (push-to-talk) : remplit le champ de saisie
    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && binding != null) {
                    List<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        binding.etMessage.setText(matches.get(0));
                        binding.etMessage.setSelection(matches.get(0).length());
                    }
                }
            });

    // Sélecteur d'image (photo de plat)
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingImageUri = uri;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Photo jointe au prochain message", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.recyclerChat.setLayoutManager(layoutManager);
        binding.recyclerChat.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.btnMic.setOnClickListener(v -> startVoiceInput());
        binding.btnPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnProvider.setOnClickListener(v -> toggleProvider());

        fetchProfile();
        loadLatestConversation();
        updateEmptyState();
    }

    private void fetchProfile() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateProviderButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Erreur profil (chat)", t);
            }
        });
    }

    private void updateProviderButton() {
        if (binding == null || currentUser == null) return;
        boolean gemini = "GEMINI".equalsIgnoreCase(currentUser.getAiProvider());
        binding.btnProvider.setText(gemini ? "Gemini" : "Mistral");
    }

    private void toggleProvider() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Profil non chargé", Toast.LENGTH_SHORT).show();
            return;
        }
        String next = "GEMINI".equalsIgnoreCase(currentUser.getAiProvider()) ? "MISTRAL" : "GEMINI";
        currentUser.setAiProvider(next);
        RetrofitClient.getApiService().updateProfile(currentUser).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateProviderButton();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Agent : " + binding.btnProvider.getText(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Impossible de changer d'agent", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLatestConversation() {
        RetrofitClient.getApiService().getConversations().enqueue(new Callback<List<ConversationSummaryDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ConversationSummaryDto>> call,
                                   @NonNull Response<List<ConversationSummaryDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    conversationId = response.body().get(0).getId();
                    loadConversationMessages(conversationId);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ConversationSummaryDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Erreur liste conversations", t);
            }
        });
    }

    private void loadConversationMessages(Long id) {
        RetrofitClient.getApiService().getConversationMessages(id).enqueue(new Callback<List<ChatMessageDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatMessageDto>> call,
                                   @NonNull Response<List<ChatMessageDto>> response) {
                if (response.isSuccessful() && response.body() != null && binding != null) {
                    adapter.setMessages(response.body());
                    updateEmptyState();
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatMessageDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Erreur messages conversation", t);
            }
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant");
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Reconnaissance vocale indisponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        if (binding == null) return;
        String text = binding.etMessage.getText().toString().trim();
        boolean hasImage = pendingImageUri != null;
        if (text.isEmpty() && !hasImage) {
            return;
        }
        if (text.isEmpty()) {
            text = "Analyse cette photo et ajoute les aliments à mon journal.";
        }

        adapter.addMessage(new ChatMessageDto("USER", text));
        updateEmptyState();
        scrollToBottom();
        binding.etMessage.setText("");
        binding.btnSend.setEnabled(false);

        RequestBody messagePart = RequestBody.create(MediaType.parse("text/plain"), text);
        RequestBody conversationPart = conversationId != null
                ? RequestBody.create(MediaType.parse("text/plain"), String.valueOf(conversationId))
                : null;

        MultipartBody.Part imagePart = null;
        if (hasImage) {
            try {
                byte[] bytes = readBytes(pendingImageUri);
                RequestBody imageBody = RequestBody.create(MediaType.parse("image/*"), bytes);
                imagePart = MultipartBody.Part.createFormData("image", "photo.jpg", imageBody);
            } catch (IOException e) {
                Log.e(TAG, "Erreur lecture image", e);
                Toast.makeText(getContext(), "Impossible de lire la photo", Toast.LENGTH_SHORT).show();
            }
        }
        pendingImageUri = null;

        RetrofitClient.getApiService().sendAgentMessage(messagePart, conversationPart, imagePart)
                .enqueue(new Callback<ChatResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<ChatResponseDto> call,
                                           @NonNull Response<ChatResponseDto> response) {
                        if (binding == null) return;
                        binding.btnSend.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            ChatResponseDto body = response.body();
                            conversationId = body.getConversationId();
                            adapter.addMessage(new ChatMessageDto("ASSISTANT", body.getReply()));
                            updateEmptyState();
                            scrollToBottom();
                            if (body.getActionsTaken() != null && !body.getActionsTaken().isEmpty()
                                    && getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Actions : " + joinActions(body.getActionsTaken()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            adapter.addMessage(new ChatMessageDto("ASSISTANT",
                                    "Désolé, une erreur est survenue (" + response.code() + ")."));
                            updateEmptyState();
                            scrollToBottom();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ChatResponseDto> call, @NonNull Throwable t) {
                        Log.e(TAG, "Erreur envoi message agent", t);
                        if (binding == null) return;
                        binding.btnSend.setEnabled(true);
                        adapter.addMessage(new ChatMessageDto("ASSISTANT",
                                "Échec de l'envoi. Vérifiez votre connexion."));
                        updateEmptyState();
                        scrollToBottom();
                    }
                });
    }

    private String joinActions(List<String> actions) {
        StringBuilder sb = new StringBuilder();
        for (String action : actions) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(action);
        }
        return sb.toString();
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) {
                throw new IOException("Flux image introuvable");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private void scrollToBottom() {
        if (binding != null && adapter.getItemCount() > 0) {
            binding.recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void updateEmptyState() {
        if (binding != null) {
            binding.chatEmpty.setVisibility(adapter.getMessageCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
