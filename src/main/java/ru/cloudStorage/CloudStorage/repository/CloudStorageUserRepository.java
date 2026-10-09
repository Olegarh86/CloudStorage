package ru.cloudStorage.CloudStorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cloudStorage.CloudStorage.model.CloudStorageUser;

import java.util.Optional;

@Repository
public interface CloudStorageUserRepository extends JpaRepository<CloudStorageUser, Long> {
    Optional <CloudStorageUser> findByUserName(String userName);
}
