package anton.davila.selfpotify.user.profile.service;

import anton.davila.selfpotify.user.profile.entity.Profile;
import anton.davila.selfpotify.user.profile.repository.ProfileRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    public Profile add(Profile p) {
        log.info("Añadiendo nuevo perfil: {}", p.getName());
        return profileRepository.save(p);
    }

    public List<Profile> getAll() {
        log.info("Recuperando todos los perfiles");
        return profileRepository.findAll();
    }

    public Optional<Profile> getById(long id) {
        log.info("Buscando perfil por ID: {}", id);
        return profileRepository.findById(id);
    }

    @Transactional
    public Profile update(long id, Profile profileData) {
        log.info("Actualizando perfil con ID: {}", id);
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el perfil con ID " + id));
        profile.copy(profileData);
        return profile;
    }

    public Profile delete(long id) {
        log.warn("Eliminando perfil con ID: {}", id);
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el perfil con ID " + id));
        profileRepository.delete(profile);
        return profile;
    }
}
