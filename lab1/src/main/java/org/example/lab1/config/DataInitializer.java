package org.example.lab1.config;

import org.example.lab1.post.Post;
import org.example.lab1.post.PostRepository;
import org.example.lab1.user.AppUser;
import org.example.lab1.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDatabase(
            AppUserRepository userRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.username}") String seedUsername,
            @Value("${app.seed.password}") String seedPassword,
            @Value("${app.seed.role}") String seedRole
    ) {
        return args -> {
            AppUser admin = userRepository.findByUsername(seedUsername)
                    .orElseGet(() -> userRepository.save(new AppUser(
                            seedUsername,
                            passwordEncoder.encode(seedPassword),
                            seedRole
                    )));

            if (postRepository.count() == 0) {
                postRepository.save(new Post("Security checklist", "Use JWT, bcrypt and parameterized queries.", admin));
                postRepository.save(new Post("CI/CD", "Run tests, SAST and SCA on every push.", admin));
            }
        };
    }
}
