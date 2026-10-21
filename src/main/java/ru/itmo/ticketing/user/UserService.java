package ru.itmo.ticketing.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository users;

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
    public Page<User> list(Pageable pageable) {
        return users.findAll(pageable);
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
