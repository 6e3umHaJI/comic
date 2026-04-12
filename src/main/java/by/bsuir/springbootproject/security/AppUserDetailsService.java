package by.bsuir.springbootproject.security;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.principal.UserPrincipal;
import by.bsuir.springbootproject.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUsername(login, login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        return new UserPrincipal(user);
    }
}