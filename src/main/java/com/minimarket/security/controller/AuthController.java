package com.minimarket.security.controller;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.security.model.LoginRequest;
import com.minimarket.security.model.LoginResponse;
import com.minimarket.security.model.RegisterRequest;
import com.minimarket.security.model.RegisterResponse;
import com.minimarket.service.UsuarioValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y generación de tokens JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final long expirationSeconds;
    private final String issuer;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds,
            @Value("${app.jwt.issuer}") String issuer) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar cliente", description = "Crea una cuenta pública solo con ROLE_CLIENTE.", security = {})
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol no permitido", content = @Content),
            @ApiResponse(responseCode = "409", description = "Usuario o correo duplicado", content = @Content)
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe ingresar usuario, correo y contraseña"));
        }
        if (solicitaRolNoCliente(request.getRoles())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El registro público solo permite ROLE_CLIENTE"));
        }

        String username;
        String correo;
        try {
            username = UsuarioValidation.validarUsername(request.getUsername());
            correo = UsuarioValidation.validarCorreo(request.getCorreo());
            UsuarioValidation.validarPassword(request.getPassword());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }

        if (usuarioRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El nombre de usuario ya existe"));
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya existe"));
        }

        Rol clienteRol = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseThrow(() -> new IllegalStateException("Rol de cliente no configurado"));

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(Set.of(clienteRol));

        Usuario guardado = usuarioRepository.save(usuario);
        List<String> roles = guardado.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
        RegisterResponse response = new RegisterResponse(
                guardado.getId(), guardado.getUsername(), guardado.getCorreo(), roles);

        return ResponseEntity
                .created(URI.create("/api/usuarios/" + guardado.getId()))
                .body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un token Bearer JWT.", security = {})
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación correcta",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Credenciales incompletas", content = @Content),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content)
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe ingresar usuario y contraseña"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .sorted()
                    .toList();

            Instant issuedAt = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .issuedAt(issuedAt)
                    .expiresAt(issuedAt.plusSeconds(expirationSeconds))
                    .subject(authentication.getName())
                    .claim("roles", roles)
                    .build();

            JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

            String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
            return ResponseEntity.ok(new LoginResponse(
                    token,
                    "Bearer",
                    expirationSeconds,
                    authentication.getName(),
                    roles));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contraseña incorrectos"));
        }
    }

    private boolean solicitaRolNoCliente(List<String> roles) {
        return roles != null && roles.stream()
                .filter(rol -> rol != null && !rol.isBlank())
                .map(String::trim)
                .anyMatch(rol -> !"ROLE_CLIENTE".equals(rol));
    }
}
