package anton.davila.selfpotify.user.service;

import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User add(User u) {
        log.info("Añadiendo nuevo usuario: {}", u.getUsername());
        return userRepository.save(u);
    }

    public List<User> getAll() {
        log.info("Recuperando todos los usuarios");
        return userRepository.findAll();
    }

    public Optional<User> getById(long id) {
        log.info("Buscando usuario por ID: {}", id);
        return userRepository.findById(id);
    }

    @Transactional
    public User update(long id, User userData) {
        log.info("Actualizando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
        user.copy(userData);
        return user;
    }

    public User delete(long id) {
        log.warn("Eliminando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
        userRepository.delete(user);
        return user;
    }
}
