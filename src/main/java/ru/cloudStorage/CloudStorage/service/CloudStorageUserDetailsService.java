package ru.cloudStorage.CloudStorage.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.cloudStorage.CloudStorage.model.User;
import ru.cloudStorage.CloudStorage.repository.UserRepository;

@Service
public class CloudStorageUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CloudStorageUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @NotNull
    @Override
    public UserDetails loadUserByUsername(@NotNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with name " + username + " not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}
