package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.UserRepository;
import anton.davila.selfpotify.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private User userOriginal;

    @BeforeEach
    void setUp() {
        userOriginal = new User();
        userOriginal.setId(1L);
        userOriginal.setUsername("john_doe");
    }

    @Test
    void testAdd() {
        when(userRepository.save(any(User.class))).thenReturn(userOriginal);
        User result = userService.add(new User());
        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
    }

    @Test
    void testGetAll() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(userOriginal));
        List<User> users = userService.getAll();
        assertEquals(1, users.size());
    }

    @Test
    void testUpdate_Success() {
        User newData = new User();
        newData.setUsername("jane_doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOriginal));
        
        User updated = userService.update(1L, newData);
        assertEquals("jane_doe", updated.getUsername());
    }

    @Test
    void testDelete_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOriginal));
        User deleted = userService.delete(1L);
        assertNotNull(deleted);
        verify(userRepository, times(1)).delete(userOriginal);
    }
}
