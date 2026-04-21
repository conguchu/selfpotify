package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.user.profile.entity.Profile;
import anton.davila.selfpotify.user.profile.repository.ProfileRepository;
import anton.davila.selfpotify.user.profile.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @MockitoBean
    private ProfileRepository profileRepository;

    private Profile profileOriginal;

    @BeforeEach
    void setUp() {
        profileOriginal = new Profile();
        profileOriginal.setId(1L);
        profileOriginal.setName("John's Profile");
    }

    @Test
    void testAdd() {
        when(profileRepository.save(any(Profile.class))).thenReturn(profileOriginal);
        Profile result = profileService.add(new Profile());
        assertNotNull(result);
        assertEquals("John's Profile", result.getName());
    }

    @Test
    void testGetAll() {
        when(profileRepository.findAll()).thenReturn(Arrays.asList(profileOriginal));
        List<Profile> profiles = profileService.getAll();
        assertEquals(1, profiles.size());
    }

    @Test
    void testUpdate_Success() {
        Profile newData = new Profile();
        newData.setName("Updated Profile Name");
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profileOriginal));
        
        Profile updated = profileService.update(1L, newData);
        assertEquals("Updated Profile Name", updated.getName());
    }

    @Test
    void testDelete_Success() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profileOriginal));
        Profile deleted = profileService.delete(1L);
        assertNotNull(deleted);
        verify(profileRepository, times(1)).delete(profileOriginal);
    }
}
