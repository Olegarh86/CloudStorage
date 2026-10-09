package ru.cloudStorage.CloudStorage.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.cloudStorage.CloudStorage.model.CloudStorageUser;
import ru.cloudStorage.CloudStorage.repository.CloudStorageUserRepository;

@Service
public class CloudStorageUserDetailsService implements UserDetailsService {

    private final CloudStorageUserRepository userRepository;

    @Autowired
    public CloudStorageUserDetailsService(CloudStorageUserRepository CloudStorageUserRepository) {
        this.userRepository = CloudStorageUserRepository;
    }

    @NotNull
    @Override
    public UserDetails loadUserByUsername(@NotNull String username) throws UsernameNotFoundException {
        CloudStorageUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with name: " + username));

        return User.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}
