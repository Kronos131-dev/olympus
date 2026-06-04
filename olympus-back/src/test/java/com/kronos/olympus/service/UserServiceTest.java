package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.mapper.UserMapper;
import com.kronos.olympus.model.User;
import com.kronos.olympus.repository.UserMetricsRepository;
import com.kronos.olympus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie la synchronisation du poids depuis une app tierce liée (Chiron) :
 * mise à jour + historisation de la métrique, et no-op si le poids est inchangé.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMetricsRepository userMetricsRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserService userService;

    @Test
    void updateWeightByEmail_metAJourLePoidsEtHistoriseLaMetrique() {
        User user = User.builder().id(1L).email("a@b.c").currentWeightKg(80.0).build();
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().targetKcal(2200.0).build());
        when(userMetricsRepository.findTopByUserIdOrderByRecordedDateDesc(1L))
                .thenReturn(Optional.empty());

        userService.updateWeightByEmail("a@b.c", 82.5);

        assertEquals(82.5, user.getCurrentWeightKg());
        verify(userRepository).save(user);
        verify(userMetricsRepository).save(any());
    }

    @Test
    void updateWeightByEmail_neFaitRienSiPoidsInchange() {
        User user = User.builder().id(1L).email("a@b.c").currentWeightKg(80.0).build();
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));

        userService.updateWeightByEmail("a@b.c", 80.0);

        verify(userRepository, never()).save(any());
        verify(userMetricsRepository, never()).save(any());
    }
}
