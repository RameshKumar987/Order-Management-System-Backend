package com.oms.auth_service.seeder;

import com.oms.auth_service.entity.Role;
import com.oms.auth_service.entity.RoleName;
import com.oms.auth_service.entity.User;
import com.oms.auth_service.repository.RoleRepository;
import com.oms.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(
                            new Role(null, roleName)
                    ));
        }

        //create admin user if not exists
        if (userRepository.findByEmail("admin@oms.com").isEmpty()) {

            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow();

            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@oms.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(adminRole);

            userRepository.save(admin);
        }
    }
}
