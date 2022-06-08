package antifraud.service;

import antifraud.entity.User;
import antifraud.enums.Role;
import antifraud.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public Map<String, Object> register(User user) {
        if (repository.findUserByUsernameIgnoreCase(user.getUsername()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        if (repository.findAll().stream().findAny().isEmpty()) {
            user.setRole(Role.ADMINISTRATOR.getRoleWithPrefix());
            user.setAccountNonLocked(true);
        } else {
            user.setRole(Role.MERCHANT.getRoleWithPrefix());
            user.setAccountNonLocked(false);
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return getUserResponseBody(repository.save(user));
    }

    public Map<String, Object> update(User user) {
        return getUserResponseBody(repository.save(user));
    }

    private Map<String, Object> getUserResponseBody(User user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "username", user.getUsername(),
                "role", user.getRole().replaceAll("ROLE_", "")
        );
    }

    public List<Map<String, Object>> getList() {
        Iterable<User> users = repository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        users.forEach(user -> list.add(getUserResponseBody(user)));
        return list;
    }

    public Map<String, Object> delete(String username) {
        User user = repository.findUserByUsernameIgnoreCase(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(user);
        return getDeleteResponseBody(user.getUsername());
    }

    private Map<String, Object> getDeleteResponseBody(String username) {
        return Map.of("username", username, "status", "Deleted successfully!");
    }

    public User findUserByUsernameIgnoreCase(String username) {
        return repository.findUserByUsernameIgnoreCase(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND)
        );
    }

}
