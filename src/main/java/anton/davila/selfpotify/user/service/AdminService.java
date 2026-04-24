package anton.davila.selfpotify.user.service;

import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.repository.AdminRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    public Admin add(Admin a) {
        log.info("Añadiendo nuevo admin: {}", a.getUsername());
        return adminRepository.save(a);
    }

    public List<Admin> getAll() {
        log.info("Recuperando todos los admins");
        return adminRepository.findAll();
    }

    public Optional<Admin> getById(long id) {
        log.info("Buscando admin por ID: {}", id);
        return adminRepository.findById(id);
    }

    @Transactional
    public Admin update(long id, Admin adminData) {
        log.info("Actualizando admin con ID: {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el admin con ID " + id));
        admin.copy(adminData);
        return admin;
    }

    public Admin delete(long id) {
        log.warn("Eliminando admin con ID: {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el admin con ID " + id));
        adminRepository.delete(admin);
        return admin;
    }
}
