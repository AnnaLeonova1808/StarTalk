package com.example.startalk.service.impl;

import com.example.startalk.dto.JwtAuthenticationDto;
import com.example.startalk.dto.RefreshTokenDto;
import com.example.startalk.dto.UserCredentialsDto;
import com.example.startalk.dto.UserDto;
import com.example.startalk.entity.User;
import com.example.startalk.mapper.UserMapper;
import com.example.startalk.repository.UserRepository;
import com.example.startalk.security.jwt.JwtService;
import com.example.startalk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.AuthenticationException;
import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public JwtAuthenticationDto singIn(UserCredentialsDto userCredentialsDto) throws AuthenticationException {
        User user = findByCredentials(userCredentialsDto);
        return jwtService.generateAuthToken(user.getEmail());
    }

    @Override
    public JwtAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto) throws Exception {
        String refreshToken = refreshTokenDto.getRefreshToken();
        if (refreshToken != null && jwtService.validateJwtToken(refreshToken)) {
            User user = findByEmail(jwtService.getEmailFromToken(refreshToken));
            return jwtService.refreshBaseToken(user.getEmail(), refreshToken);
        }
        throw new  AuthenticationException("Invalid refresh token");
    }

    @Override
    @Transactional
    public UserDto getUserById(String id) throws ChangeSetPersister.NotFoundException {
        return userMapper.toDto(userRepository.findByUserId(UUID.fromString(id))
                .orElseThrow(ChangeSetPersister.NotFoundException::new));
    }

    @Override
    @Transactional
    public UserDto getUserByEmail(String email) throws ChangeSetPersister.NotFoundException {
        return userMapper.toDto(userRepository.findByEmail(email)
                .orElseThrow(ChangeSetPersister.NotFoundException::new));
    }

    @Override
    public String addUser(UserDto userDto){
        User user = userMapper.toEntity(userDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "User added";
    }

    private User findByCredentials(UserCredentialsDto userCredentialsDto) throws AuthenticationException {
        Optional<User> optionalUser = userRepository.findByEmail(userCredentialsDto.getEmail());
        if (optionalUser.isPresent()){
            User user = optionalUser.get();
            if (passwordEncoder.matches(userCredentialsDto.getPassword(), user.getPassword())){
                return user;
            }
        }
        throw new AuthenticationException("Email or password is not correct");
    }

    private User findByEmail(String email) throws Exception {
        return userRepository.findByEmail(email).orElseThrow(()->
                new Exception(String.format("User with email %s not found", email)));
    }
}