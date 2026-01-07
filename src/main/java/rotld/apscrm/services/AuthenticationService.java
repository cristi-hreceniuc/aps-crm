package rotld.apscrm.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.auth.entity.PendingRegistration;
import rotld.apscrm.api.v1.auth.entity.RefreshToken;
import rotld.apscrm.api.v1.auth.repository.PendingRegistrationRepository;
import rotld.apscrm.api.v1.auth.service.RefreshTokenService;
import rotld.apscrm.api.v1.user.dto.*;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;
import rotld.apscrm.api.v1.user.service.UserService;
import rotld.apscrm.exception.DuplicateEmailException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailSenderService emailSenderService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserService userService;

    @Value("${app.reset.frontend-url}")
    private String resetUrlBase;

    @Value("${app.reset.ttl-minutes:60}")
    private long resetTtlMinutes;

    @Value("${app.otp.ttl-minutes:15}")
    private long otpTtlMinutes;
    @Value("${app.otp.max-attempts:5}")
    private int otpMaxAttempts;
    @Value("${app.otp.lockout-minutes:15}")
    private long otpLockoutMinutes;

    public User signup(RegisterUserDto input) {
        // Check if email already exists
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new DuplicateEmailException(input.email());
        }

        // Validate role - USER, SPECIALIST, or VOLUNTEER (CRM web)
        UserRole role = input.userRole() != null ? input.userRole() : UserRole.USER;
        if (role != UserRole.USER && role != UserRole.SPECIALIST && role != UserRole.VOLUNTEER) {
            throw new IllegalArgumentException("Invalid user role. Only USER, SPECIALIST, PREMIUM, or VOLUNTEER roles are allowed.");
        }

        // VOLUNTEER accounts (created by admins via CRM) are ACTIVE immediately
        // Mobile app accounts (USER, SPECIALIST, PREMIUM) start as PENDING
        UserStatus status = (role == UserRole.VOLUNTEER) ? UserStatus.ACTIVE : UserStatus.PENDING;

        User user = User.builder()
                .lastName(input.lastName())
                .firstName(input.firstName())
                .gender(input.gender())
                .email(input.email())
                .password(passwordEncoder.encode(input.password()))
                .userRole(role)
                .userStatus(status)
                .isPremium(Boolean.FALSE)
                .build();

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        return userRepository.findByEmail(input.email())
                .orElseThrow();
    }

    public void requestPasswordReset(String email) {
        var opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            // idempotent: nu divulgăm existența
            log.info("Password reset requested for non-existing email {}", email);
            return;
        }
        var user = opt.get();

        var rawToken = UUID.randomUUID().toString().replace("-", "");
        var expiresAt = Instant.now().plus(resetTtlMinutes, ChronoUnit.MINUTES);

        user.setResetToken(rawToken);
        user.setResetTokenExpiresAt(expiresAt);
        userRepository.save(user);

        var resetLink = resetUrlBase + rawToken;
        var html = buildResetEmailHtml(
                user.getFirstName() != null ? " " + user.getFirstName() : "",
                resetLink,
                String.valueOf(resetTtlMinutes)
        );

        try {
            emailSenderService.sendEmail(
                    user.getEmail(),
                    "Resetează parola contului tău – Logopedy",
                    html
            );
        } catch (Exception e) {
            log.error("Failed sending reset email", e);
            throw new RuntimeException("Nu am putut trimite e-mailul de resetare.");
        }
    }

    public void confirmPasswordReset(String token, String password, String confirmPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token lipsă.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Parola trebuie să aibă cel puțin 8 caractere.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Parolele nu coincid.");
        }

        var user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid sau expirat."));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expirat. Solicită o nouă resetare.");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    private String buildResetEmailHtml(String firstNameOpt, String resetLink, String ttlMinutes) {
        String template = """
        <!doctype html><html lang="ro"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
        <title>Resetare parolă – Logopedy</title>
        <style>
          body{margin:0;padding:0;background:#f6f7fb;font-family:-apple-system, Segoe UI, Roboto, Arial, sans-serif;color:#111827}
          .container{max-width:600px;margin:0 auto;background:#ffffff}
          .header{padding:24px 24px 0;text-align:center}
          .logo{height:40px}
          .content{padding:24px;line-height:1.55;font-size:15px}
          .btn-wrap{text-align:center;margin:24px 0}
          .btn{display:inline-block;padding:12px 20px;border-radius:10px;background:#2563eb;color:#ffffff;font-weight:600}
          .meta{font-size:12px;color:#6b7280}
          .footer{ text-align:center;color:#6b7280;font-size:12px;padding:16px 24px 24px}
          @media (prefers-color-scheme: dark){ body{background:#0b1220;color:#e5e7eb}.container{background:#131a2a}.meta,.footer{color:#9ca3af}.btn{background:#3b82f6} }
        </style></head><body>
        <div class="container"><div class="header">
          <img class="logo" src="cid:logopedy-logo.png" alt="Logopedy">
        </div>
        <div class="content">
          <p class="meta">Ai solicitat resetarea parolei pentru contul tău Logopedy.</p>
          <h1 style="font-size:20px;margin:0 0 12px 0;">Salut{{FIRST_NAME_OPT}},</h1>
          <p>Pentru a-ți reseta parola, apasă pe butonul de mai jos. Linkul este valabil {{TTL_MINUTES}} minute.</p>
          <div class="btn-wrap"><a class="btn" href="{{RESET_LINK}}" target="_blank" rel="noopener">Resetează parola</a></div>
          <p>Dacă butonul nu funcționează, copiază și lipește acest link în browser:</p>
          <p style="word-break:break-all;"><a href="{{RESET_LINK}}" target="_blank" rel="noopener">{{RESET_LINK}}</a></p>
          <hr style="border:none;border-top:1px solid #e5e7eb;margin:20px 0;">
          <p class="meta">E-mail trimis automat de Logopedy. Pentru asistență: suport@logopedy.app.</p>
        </div><div class="footer">© {{YEAR}} Logopedy</div></div>
        </body></html>
        """;

        return template
                .replace("{{FIRST_NAME_OPT}}", firstNameOpt == null ? "" : firstNameOpt)
                .replace("{{TTL_MINUTES}}", ttlMinutes)
                .replace("{{RESET_LINK}}", resetLink)
                .replace("{{YEAR}}", String.valueOf(java.time.Year.now().getValue()));
    }

    private String buildOtpEmailHtml(String firstNameOpt, String code, long ttlMinutes) {
        return """
    <!doctype html><html lang="ro"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
    <title>Cod resetare parolă – Logopedy</title>
    <style>
      body{margin:0;padding:0;background:#f6f7fb;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;color:#111827}
      .box{max-width:600px;margin:0 auto;background:#fff;padding:24px}
      .code{font-size:28px;letter-spacing:4px;font-weight:700;background:#111827;color:#fff;display:inline-block;padding:10px 14px;border-radius:10px}
      .meta{color:#6b7280;font-size:12px;margin-top:16px}
    </style></head><body>
      <div class="box">
        <p>Salut%s,</p>
        <p>Codul tău de resetare a parolei este:</p>
        <div class="code">%s</div>
        <p>Codul expiră în %d minute și poate fi folosit o singură dată.</p>
        <p class="meta">Dacă nu ai cerut tu această resetare, poți ignora mesajul.</p>
      </div>
    </body></html>
    """.formatted(firstNameOpt == null ? "" : " " + firstNameOpt, code, ttlMinutes);
    }

    private static final SecureRandom RNG = new SecureRandom();

    public void requestPasswordResetOtp(String email) {
        var opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            // idempotent: nu divulgăm existența
            return;
        }
        var user = opt.get();

        // lockout?
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(Instant.now())) {
            // nu divulgăm motivul
            return;
        }

        // generează OTP 6 cifre (000000–999999), hash-uiește pentru stocare
        String otp = String.format("%06d", RNG.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(otp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpTtlMinutes));

        user.setOtpHash(otpHash);
        user.setOtpExpiresAt(expiresAt);
        user.setOtpAttempts(0);
        user.setOtpLockedUntil(null);
        userRepository.save(user);

        String html = buildOtpEmailHtml(user.getFirstName(), otp, otpTtlMinutes);
        try {
            emailSenderService.sendEmail(user.getEmail(), "Cod resetare parolă – Logopedy", html);
        } catch (Exception e) {
            throw new RuntimeException("Nu am putut trimite emailul cu codul de resetare.");
        }
    }

    public void resetPasswordWithOtp(String email, String otp, String password, String confirmPassword) {
        // validări de bază
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Parola trebuie să aibă cel puțin 8 caractere.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Parolele nu coincid.");
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Cod invalid sau expirat."));

        // lockout check
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Prea multe încercări. Încearcă mai târziu.");
        }

        // expirat?
        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(Instant.now()) || user.getOtpHash() == null) {
            throw new IllegalArgumentException("Cod invalid sau expirat.");
        }

        // match?
        boolean ok = passwordEncoder.matches(otp, user.getOtpHash());
        if (!ok) {
            int attempts = (user.getOtpAttempts() == 0 ? 0 : user.getOtpAttempts()) + 1;
            user.setOtpAttempts(attempts);

            if (attempts >= otpMaxAttempts) {
                user.setOtpLockedUntil(Instant.now().plus(Duration.ofMinutes(otpLockoutMinutes)));
                // opțional: invalidează codul
                // user.setOtpHash(null); user.setOtpExpiresAt(null);
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Cod invalid.");
        }

        // succes → setează parola și invalidează OTP
        user.setPassword(passwordEncoder.encode(password));
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        user.setOtpLockedUntil(null);
        userRepository.save(user);
    }

    public record Tokens(String access, long accessExpMs, String refresh, long refreshExpMs) {}

    public Tokens issueTokens(User user) {
        // Include isPremium claim in JWT token
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("is_premium", Boolean.TRUE.equals(user.getIsPremium()));
        String access = jwtService.generateToken(claims, user);
        long accessExp = jwtService.getExpirationTime();
        RefreshToken rt = refreshTokenService.create(user.getId(), jwtService.getRefreshExpirationTime());
        return new Tokens(access, accessExp, rt.getToken(), jwtService.getRefreshExpirationTime());
    }

    // ============== REGISTRATION WITH OTP ==============

    /**
     * Step 1: Request OTP for registration - stores pending registration and sends OTP to email
     */
    public void requestRegistrationOtp(RegisterOtpRequestDto input) {
        // Check if email already exists as a registered user
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new DuplicateEmailException(input.email());
        }

        // Validate role - only USER, SPECIALIST, or PREMIUM allowed for registration
        UserRole role = input.userRole() != null ? input.userRole() : UserRole.USER;
        if (role != UserRole.USER && role != UserRole.SPECIALIST) {
            throw new IllegalArgumentException("Invalid user role. Only USER, SPECIALIST, or PREMIUM roles are allowed.");
        }

        // Validate password
        if (input.password() == null || input.password().length() < 8) {
            throw new IllegalArgumentException("Parola trebuie să aibă cel puțin 8 caractere.");
        }

        // Check for existing pending registration
        var existingPending = pendingRegistrationRepository.findByEmail(input.email());
        
        // If there's an existing pending registration with lockout, check it
        if (existingPending.isPresent()) {
            var pending = existingPending.get();
            if (pending.getOtpLockedUntil() != null && pending.getOtpLockedUntil().isAfter(Instant.now())) {
                throw new IllegalArgumentException("Prea multe încercări. Încearcă mai târziu.");
            }
            // Delete old pending registration
            pendingRegistrationRepository.delete(pending);
        }

        // Generate OTP 6 digits (000000–999999), hash for storage
        String otp = String.format("%06d", RNG.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(otp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpTtlMinutes));

        // Create pending registration
        PendingRegistration pendingRegistration = PendingRegistration.builder()
                .email(input.email())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .gender(input.gender())
                .passwordHash(passwordEncoder.encode(input.password()))
                .userRole(role)
                .otpHash(otpHash)
                .otpExpiresAt(expiresAt)
                .otpAttempts(0)
                .build();

        pendingRegistrationRepository.save(pendingRegistration);

        // Send OTP email
        String html = buildRegistrationOtpEmailHtml(input.firstName(), otp, otpTtlMinutes);
        try {
            emailSenderService.sendEmail(input.email(), "Cod verificare înregistrare – Logopedy", html);
        } catch (Exception e) {
            log.error("Failed sending registration OTP email", e);
            throw new RuntimeException("Nu am putut trimite emailul cu codul de verificare.");
        }
    }

    /**
     * Step 2: Verify OTP and complete registration
     */
    public User verifyRegistrationOtp(String email, String otp) {
        var pendingOpt = pendingRegistrationRepository.findByEmail(email);
        if (pendingOpt.isEmpty()) {
            throw new IllegalArgumentException("Cod invalid sau expirat.");
        }

        var pending = pendingOpt.get();

        // Lockout check
        if (pending.getOtpLockedUntil() != null && pending.getOtpLockedUntil().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Prea multe încercări. Încearcă mai târziu.");
        }

        // Expired check
        if (pending.getOtpExpiresAt() == null || pending.getOtpExpiresAt().isBefore(Instant.now()) || pending.getOtpHash() == null) {
            throw new IllegalArgumentException("Cod invalid sau expirat. Te rog încearcă să te înregistrezi din nou.");
        }

        // Verify OTP
        boolean ok = passwordEncoder.matches(otp, pending.getOtpHash());
        if (!ok) {
            int attempts = pending.getOtpAttempts() + 1;
            pending.setOtpAttempts(attempts);

            if (attempts >= otpMaxAttempts) {
                pending.setOtpLockedUntil(Instant.now().plus(Duration.ofMinutes(otpLockoutMinutes)));
            }
            pendingRegistrationRepository.save(pending);
            throw new IllegalArgumentException("Cod invalid.");
        }

        // Check again if email was registered in the meantime
        if (userRepository.findByEmail(email).isPresent()) {
            pendingRegistrationRepository.delete(pending);
            throw new DuplicateEmailException(email);
        }

        // Success - create actual user
        User user = User.builder()
                .lastName(pending.getLastName())
                .firstName(pending.getFirstName())
                .gender(pending.getGender())
                .email(pending.getEmail())
                .password(pending.getPasswordHash()) // Already hashed
                .userRole(pending.getUserRole())
                .userStatus(UserStatus.PENDING)
                .isPremium(Boolean.FALSE)
                .build();

        User savedUser = userRepository.save(user);

        // Delete pending registration
        pendingRegistrationRepository.delete(pending);

        return savedUser;
    }

    private String buildRegistrationOtpEmailHtml(String firstName, String code, long ttlMinutes) {
        return """
    <!doctype html><html lang="ro"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
    <title>Cod verificare înregistrare – Logopedy</title>
    <style>
      body{margin:0;padding:0;background:#f6f7fb;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;color:#111827}
      .box{max-width:600px;margin:0 auto;background:#fff;padding:24px}
      .code{font-size:28px;letter-spacing:4px;font-weight:700;background:#111827;color:#fff;display:inline-block;padding:10px 14px;border-radius:10px}
      .meta{color:#6b7280;font-size:12px;margin-top:16px}
    </style></head><body>
      <div class="box">
        <p>Salut%s,</p>
        <p>Bine ai venit la Logopedy! Pentru a-ți finaliza înregistrarea, introdu următorul cod:</p>
        <div class="code">%s</div>
        <p>Codul expiră în %d minute și poate fi folosit o singură dată.</p>
        <p class="meta">Dacă nu ai cerut tu această înregistrare, poți ignora mesajul.</p>
      </div>
    </body></html>
    """.formatted(firstName == null || firstName.isEmpty() ? "" : " " + firstName, code, ttlMinutes);
    }

    /**
     * Resend OTP for pending registration
     */
    public void resendRegistrationOtp(String email) {
        var pendingOpt = pendingRegistrationRepository.findByEmail(email);
        if (pendingOpt.isEmpty()) {
            // Don't reveal if registration exists or not
            return;
        }

        var pending = pendingOpt.get();

        // Lockout check
        if (pending.getOtpLockedUntil() != null && pending.getOtpLockedUntil().isAfter(Instant.now())) {
            return;
        }

        // Generate new OTP
        String otp = String.format("%06d", RNG.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(otp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpTtlMinutes));

        pending.setOtpHash(otpHash);
        pending.setOtpExpiresAt(expiresAt);
        pending.setOtpAttempts(0);
        pending.setOtpLockedUntil(null);
        pendingRegistrationRepository.save(pending);

        // Send OTP email
        String html = buildRegistrationOtpEmailHtml(pending.getFirstName(), otp, otpTtlMinutes);
        try {
            emailSenderService.sendEmail(email, "Cod verificare înregistrare – Logopedy", html);
        } catch (Exception e) {
            log.error("Failed sending registration OTP email", e);
            throw new RuntimeException("Nu am putut trimite emailul cu codul de verificare.");
        }
    }

    // ============== ACCOUNT DELETION WITH OTP ==============

    /**
     * Step 1: Request OTP for account deletion - sends OTP to user's email
     */
    public void requestAccountDeletionOtp(String email) {
        var opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            // idempotent: don't reveal if account exists
            return;
        }
        var user = opt.get();

        // lockout check
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(Instant.now())) {
            // don't reveal the reason
            return;
        }

        // Generate OTP 6 digits (000000–999999), hash for storage
        String otp = String.format("%06d", RNG.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(otp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpTtlMinutes));

        user.setOtpHash(otpHash);
        user.setOtpExpiresAt(expiresAt);
        user.setOtpAttempts(0);
        user.setOtpLockedUntil(null);
        userRepository.save(user);

        String html = buildAccountDeletionOtpEmailHtml(user.getFirstName(), otp, otpTtlMinutes);
        try {
            emailSenderService.sendEmail(user.getEmail(), "Cod confirmare ștergere cont – Logopedy", html);
        } catch (Exception e) {
            log.error("Failed sending account deletion OTP email", e);
            throw new RuntimeException("Nu am putut trimite emailul cu codul de confirmare.");
        }
    }

    /**
     * Step 2: Verify OTP and delete account with all related data
     */
    public void confirmAccountDeletion(String email, String otp) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Cod invalid sau expirat."));

        // lockout check
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Prea multe încercări. Încearcă mai târziu.");
        }

        // expired check
        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(Instant.now()) || user.getOtpHash() == null) {
            throw new IllegalArgumentException("Cod invalid sau expirat.");
        }

        // verify OTP
        boolean ok = passwordEncoder.matches(otp, user.getOtpHash());
        if (!ok) {
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);

            if (attempts >= otpMaxAttempts) {
                user.setOtpLockedUntil(Instant.now().plus(Duration.ofMinutes(otpLockoutMinutes)));
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Cod invalid.");
        }

        // OTP valid - delete the account and all related data
        userService.delete(java.util.UUID.fromString(user.getId()));
    }

    private String buildAccountDeletionOtpEmailHtml(String firstName, String code, long ttlMinutes) {
        return """
    <!doctype html><html lang="ro"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
    <title>Cod confirmare ștergere cont – Logopedy</title>
    <style>
      body{margin:0;padding:0;background:#f6f7fb;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;color:#111827}
      .box{max-width:600px;margin:0 auto;background:#fff;padding:24px}
      .code{font-size:28px;letter-spacing:4px;font-weight:700;background:#dc2626;color:#fff;display:inline-block;padding:10px 14px;border-radius:10px}
      .warning{background:#fef2f2;border-left:4px solid #dc2626;padding:12px 16px;margin:16px 0;color:#991b1b}
      .meta{color:#6b7280;font-size:12px;margin-top:16px}
    </style></head><body>
      <div class="box">
        <p>Salut%s,</p>
        <p>Ai solicitat ștergerea contului tău Logopedy. Pentru a confirma, introdu următorul cod:</p>
        <div class="code">%s</div>
        <div class="warning">
          <strong>Atenție!</strong> Această acțiune este ireversibilă. Toate datele tale, inclusiv profilurile și progresul, vor fi șterse permanent.
        </div>
        <p>Codul expiră în %d minute și poate fi folosit o singură dată.</p>
        <p class="meta">Dacă nu ai cerut tu această ștergere, ignoră acest mesaj și contul tău va rămâne în siguranță.</p>
      </div>
    </body></html>
    """.formatted(firstName == null || firstName.isEmpty() ? "" : " " + firstName, code, ttlMinutes);
    }

    // ============== KID AUTHENTICATION ==============

    /**
     * Authenticate a kid using their license key UUID.
     * Returns tokens with kid-specific claims.
     */
    public KidTokens authenticateKid(String keyUuid, 
                                      rotld.apscrm.api.v1.logopedy.repository.LicenseKeyRepo keyRepo) {
        var key = keyRepo.findByKeyUuidAndIsActiveTrue(keyUuid)
                .orElseThrow(() -> new IllegalArgumentException("Cheie invalidă sau dezactivată"));

        if (key.getProfile() == null) {
            throw new IllegalArgumentException("Cheia nu este activată pentru niciun profil");
        }

        // Premium is inherited from the specialist's is_premium flag
        boolean isPremium = Boolean.TRUE.equals(key.getSpecialist().getIsPremium());

        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("profile_id", key.getProfile().getId());
        claims.put("key_id", key.getId());
        claims.put("is_kid", true);
        claims.put("is_premium", isPremium);
        claims.put("specialist_id", key.getSpecialist().getId());

        String subject = "kid:" + key.getProfile().getId();
        String accessToken = jwtService.generateKidToken(claims, subject);
        
        // Create a refresh token for the kid (using key ID as reference)
        RefreshToken refreshToken = refreshTokenService.createForKid(key.getId());

        return new KidTokens(
                accessToken,
                jwtService.getExpirationTime(),
                refreshToken.getToken(),
                jwtService.getRefreshExpirationTime(),
                key.getProfile().getId(),
                key.getProfile().getName(),
                isPremium
        );
    }

    public record KidTokens(
            String accessToken,
            long accessExpiresIn,
            String refreshToken,
            long refreshExpiresIn,
            Long profileId,
            String profileName,
            boolean isPremium
    ) {}

    /**
     * Refresh a kid's access token using their refresh token.
     */
    public KidTokens refreshKidToken(String refreshTokenStr, 
                                      rotld.apscrm.api.v1.logopedy.repository.LicenseKeyRepo keyRepo) {
        RefreshToken oldRt = refreshTokenService.validateUsable(refreshTokenStr);
        
        // Kid refresh tokens have userId in format "kid:{keyId}"
        String kidUserId = oldRt.getUserId();
        if (!kidUserId.startsWith("kid:")) {
            throw new IllegalArgumentException("Token invalid pentru copil");
        }
        
        Long keyId = Long.parseLong(kidUserId.substring(4));
        var key = keyRepo.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Cheia nu mai există"));
        
        if (!key.isActive() || key.getProfile() == null) {
            throw new IllegalArgumentException("Cheia este dezactivată sau nu are profil asociat");
        }
        
        // Premium is inherited from the specialist's is_premium flag
        boolean isPremium = Boolean.TRUE.equals(key.getSpecialist().getIsPremium());

        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("profile_id", key.getProfile().getId());
        claims.put("key_id", key.getId());
        claims.put("is_kid", true);
        claims.put("is_premium", isPremium);
        claims.put("specialist_id", key.getSpecialist().getId());

        String subject = "kid:" + key.getProfile().getId();
        String accessToken = jwtService.generateKidToken(claims, subject);
        
        // Rotate the refresh token
        long kidRefreshExpMs = 30L * 24 * 60 * 60 * 1000; // 30 days
        RefreshToken newRt = refreshTokenService.rotate(oldRt, kidUserId, kidRefreshExpMs);

        return new KidTokens(
                accessToken,
                jwtService.getExpirationTime(),
                newRt.getToken(),
                jwtService.getRefreshExpirationTime(),
                key.getProfile().getId(),
                key.getProfile().getName(),
                isPremium
        );
    }
}