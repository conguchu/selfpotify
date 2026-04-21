package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.repository.AdminRepository;
import anton.davila.selfpotify.user.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @MockitoBean
    private AdminRepository adminRepository;

    private Admin adminOriginal;

    @BeforeEach
    void setUp() {
        adminOriginal = new Admin();
        adminOriginal.setId(1L);
        adminOriginal.setUsername("admin_root");
    }

    @Test
    void testAdd() {
        when(adminRepository.save(any(Admin.class))).thenReturn(adminOriginal);
        Admin result = adminService.add(new Admin());
        assertNotNull(result);
        assertEquals("admin_root", result.getUsername());
    }

    @Test
    void testGetAll() {
        when(adminRepository.findAll()).thenReturn(Arrays.asList(adminOriginal));
        List<Admin> admins = adminService.getAll();
        assertEquals(1, admins.size());
    }

    @Test
    void testUpdate_Success() {
        Admin newData = new Admin();
        newData.setUsername("super_admin");
        when(adminRepository.findById(1L)).thenReturn(Optional.of(adminOriginal));
        
        Admin updated = adminService.update(1L, newData);
        assertEquals("super_admin", updated.getUsername());
    }

    @Test
    void testDelete_Success() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(adminOriginal));
        Admin deleted = adminService.delete(1L);
        assertNotNull(deleted);
        verify(adminRepository, times(1)).delete(adminOriginal);
    }
}
