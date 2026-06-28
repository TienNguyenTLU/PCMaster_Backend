package com.edu.pcmaster.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.dto.auth.AuthGoogleRequest;
import com.edu.pcmaster.dto.auth.AuthLoginRequest;
import com.edu.pcmaster.dto.auth.AuthRegisterRequest;
import com.edu.pcmaster.dto.auth.AuthResponse;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.models.UserRole;
import com.edu.pcmaster.repositories.UserRepository;
import com.edu.pcmaster.security.JwtTokenProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider tokenProvider;
	private final String googleClientId;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AuthService(UserRepository userRepository,
						PasswordEncoder passwordEncoder,
						JwtTokenProvider tokenProvider,
						@Value("${google.client-id}") String googleClientId) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
		this.googleClientId = googleClientId;
	}

	public AuthResponse register(AuthRegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BadRequestException("Username already exists");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BadRequestException("Email already exists");
		}

		User user = new User();
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(UserRole.CUSTOMER);
		User saved = userRepository.save(user);

		String token = tokenProvider.generateToken(saved);
		return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
	}

	public AuthResponse login(AuthLoginRequest request) {
		User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
				.orElseThrow(() -> new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác"));
		if (!user.isActive()) {
			throw new BadRequestException("Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động");
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác");
		}
		String token = tokenProvider.generateToken(user);
		return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
	}

	public AuthResponse loginWithGoogle(AuthGoogleRequest request) {
		GoogleTokenInfo tokenInfo = verifyGoogleToken(request.idToken());

		User user = userRepository.findByEmail(tokenInfo.email)
				.orElseGet(() -> {
					
					String baseUsername = tokenInfo.email.split("@")[0];
					String username = baseUsername;
					int suffix = 1;
					while (userRepository.existsByUsername(username)) {
						username = baseUsername + suffix;
						suffix++;
					}

					User newUser = new User();
					newUser.setUsername(username);
					newUser.setEmail(tokenInfo.email);
					newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
					newUser.setRole(UserRole.CUSTOMER);
					newUser.setActive(true);
					return userRepository.save(newUser);
				});

		if (!user.isActive()) {
			throw new BadRequestException("Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động");
		}

		String token = tokenProvider.generateToken(user);
		return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
	}

	private GoogleTokenInfo verifyGoogleToken(String idToken) {
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
					.GET()
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				System.err.println("Google API error: Status " + response.statusCode() + " - Body: " + response.body());
				throw new BadRequestException("Invalid Google token response from Google APIs: " + response.body());
			}

			GoogleTokenInfo tokenInfo = objectMapper.readValue(response.body(), GoogleTokenInfo.class);

			if (tokenInfo == null || tokenInfo.email == null) {
				throw new BadRequestException("Invalid Google token payload (email is null)");
			}
			if (tokenInfo.iss == null || !tokenInfo.iss.contains("accounts.google.com")) {
				throw new BadRequestException("Invalid Google token issuer: " + tokenInfo.iss);
			}
			if (tokenInfo.aud == null || !googleClientId.equals(tokenInfo.aud)) {
				System.err.println("Google token audience mismatch! Token aud: " + tokenInfo.aud + ", configured client ID: " + googleClientId);
				throw new BadRequestException("Invalid Google token audience: " + tokenInfo.aud + " (configured: " + googleClientId + ")");
			}
			if (!"true".equals(tokenInfo.emailVerified)) {
				throw new BadRequestException("Google email not verified");
			}

			return tokenInfo;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			System.err.println("Google token verification error: " + e.getMessage());
			e.printStackTrace();
			throw new BadRequestException("Google token verification failed: " + e.getMessage());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class GoogleTokenInfo {
		public String iss;
		public String sub;
		public String aud;
		public String email;
		@JsonProperty("email_verified")
		public String emailVerified;
		public String name;
		public String picture;
	}
}


