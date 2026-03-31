package ru.yandex.practicum.catsgram.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.DuplicatedDataException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.model.User;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<String, Long> emailToId = new HashMap<>(); // для быстрой проверки уникальности email

    public Collection<User> findAll() {
        return users.values();
    }

    public User create(User user) {
        // проверяем, что email указан
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ConditionsNotMetException("Имейл должен быть указан");
        }

        // проверяем, что email не используется
        if (emailToId.containsKey(user.getEmail())) {
            throw new DuplicatedDataException("Этот имейл уже используется");
        }

        // формируем дополнительные данные
        user.setId(getNextId());
        user.setRegistrationDate(Instant.now());

        // сохраняем пользователя
        users.put(user.getId(), user);
        emailToId.put(user.getEmail(), user.getId());

        return user;
    }

    public User update(User updatedUser) {
        // проверяем, что id указан
        if (updatedUser.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        // проверяем, что пользователь существует
        if (!users.containsKey(updatedUser.getId())) {
            throw new NotFoundException("Пользователь с id = " + updatedUser.getId() + " не найден");
        }

        User existingUser = users.get(updatedUser.getId());

        // проверяем уникальность email, если он изменился
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            // если email изменился и новый email уже используется другим пользователем
            if (emailToId.containsKey(updatedUser.getEmail()) &&
                    !emailToId.get(updatedUser.getEmail()).equals(updatedUser.getId())) {
                throw new DuplicatedDataException("Этот имейл уже используется");
            }

            // удаляем старую связь email -> id
            emailToId.remove(existingUser.getEmail());
            // обновляем email
            existingUser.setEmail(updatedUser.getEmail());
            // добавляем новую связь
            emailToId.put(updatedUser.getEmail(), existingUser.getId());
        }

        // обновляем остальные поля, если они не null
        if (updatedUser.getUsername() != null) {
            existingUser.setUsername(updatedUser.getUsername());
        }

        if (updatedUser.getPassword() != null) {
            existingUser.setPassword(updatedUser.getPassword());
        }

        return existingUser;
    }

    // метод для поиска пользователя по id
    public Optional<User> findUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    // вспомогательный метод для генерации идентификатора нового пользователя
    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}