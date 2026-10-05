package ru.itmo.ticketing.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.NotFoundException;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User register(String email, String fullName, UserRole role) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new ConflictException("User with email " + normalized + " already exists");
        }
        return users.save(new User(normalized, fullName.trim(), role == null ? UserRole.CUSTOMER : role));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAll();
    }

    public User changeRole(Long id, UserRole role) {
        User user = get(id);
        user.setRole(role);
        return user;
    }

    public void delete(Long id) {
        users.delete(get(id));
    }
}
