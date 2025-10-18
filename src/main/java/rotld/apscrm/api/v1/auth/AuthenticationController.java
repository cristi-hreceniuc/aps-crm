package rotld.apscrm.api.v1.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.auth.dto.RefreshRequest;
import rotld.apscrm.api.v1.auth.dto.TokenResponse;
import rotld.apscrm.api.v1.auth.service.RefreshTokenService;
import rotld.apscrm.api.v1.user.dto.*;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;
import rotld.apscrm.services.AuthenticationService;
import rotld.apscrm.services.JwtService;

@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public User register(@RequestBody RegisterUserDto registerUserDto) {
        return authenticationService.signup(registerUserDto);
    }

    @PostMapping("/login")
    public LoginResponse authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);
        var t = authenticationService.issueTokens(authenticatedUser);

        return LoginResponse.builder()
                .token(jwtToken)
                .expiresIn(jwtService.getExpirationTime())
                .user(new UserProfileDto(authenticatedUser.getFirstName() + " " + authenticatedUser.getLastName(), authenticatedUser.getEmail()))
                .refreshToken(t.refresh())
                .refreshExpiresIn(t.refreshExpMs())
                .build();
    }

    /** Reînnoiește access token-ul pe baza refresh-ului (și rotește refresh-ul). */
    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest req) {
        var rt = refreshTokenService.validateUsable(req.refreshToken());
        // încarcă userul
        User user = userRepository.findById(rt.getUserId()).orElseThrow();
        // gen. access nou
        String access = jwtService.generateToken(user);
        long accessExp = jwtService.getExpirationTime();
        // rotește refresh-ul
        var newRt = refreshTokenService.rotate(rt, user.getId(), jwtService.getRefreshExpirationTime());

        return TokenResponse.builder()
                .token(access).expiresIn(accessExp)
                .refreshToken(newRt.getToken()).refreshExpiresIn(jwtService.getRefreshExpirationTime())
                .build();
    }

    /** Logout opțional: revocă toate refresh-urile userului curent. */
    @PostMapping("/logout")
    public void logout() {
        var auth = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        var user = userRepository.findByEmail(auth.getUsername()).orElseThrow();
        refreshTokenService.revokeAllForUser(user.getId());
    }

    // 1) Forgot – trimite email cu linkul
    @PostMapping(value = "/forgot", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> forgot(@RequestBody ForgotPasswordDto dto) {
        authenticationService.requestPasswordReset(dto.email());
        // 202 – indiferent dacă există cont sau nu
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    // 2) Pagina HTML cu 2 câmpuri (password + confirm)
    @GetMapping(value = "/reset", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String resetForm(@RequestParam(name = "token", required = false) String token) {
        String html = """
                <!doctype html><html lang="ro"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
                <title>Setează parolă nouă – Logopedy</title>
                <style>
                  body{font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:#0b1220;color:#e5e7eb;margin:0}
                  .wrap{max-width:420px;margin:8vh auto;padding:24px;background:#131a2a;border-radius:16px}
                  h1{font-size:20px;margin:0 0 12px}
                  label{display:block;margin:12px 0 6px}
                  input{width:100%;padding:12px;border-radius:10px;border:1px solid #334155;background:#0b1220;color:#e5e7eb}
                  button{margin-top:16px;width:100%;padding:12px;border:0;border-radius:10px;background:#3b82f6;color:#fff;font-weight:600}
                  .err{color:#fca5a5;font-size:13px;display:none;margin-top:6px}
                </style></head><body>
                <div class="wrap">
                  <h1>Setează parolă nouă</h1>
                  <form id="f" method="POST" action="/api/v1/auth/reset/confirm">
                    <input type="hidden" name="token" value="{{TOKEN}}">
                    <label>Parolă nouă</label>
                    <input type="password" id="p1" name="password" minlength="8" required>
                    <label>Confirmă parola</label>
                    <input type="password" id="p2" name="confirmPassword" minlength="8" required>
                    <div id="m" class="err">Parolele nu coincid.</div>
                    <button type="submit">Salvează parola</button>
                  </form>
                </div>
                <script>
                  const f=document.getElementById('f'), p1=document.getElementById('p1'), p2=document.getElementById('p2'), m=document.getElementById('m');
                  f.addEventListener('submit', (e)=>{ if(p1.value!==p2.value){ e.preventDefault(); m.style.display='block'; }});
                </script>
                </body></html>
                """;
        return html.replace("{{TOKEN}}", token == null ? "" : token);
    }

    @PostMapping(value = "/reset/confirm", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String resetConfirmForm(ConfirmResetForm form) {
        authenticationService.confirmPasswordReset(form.getToken(), form.getPassword(), form.getConfirmPassword());
        return "redirect:/api/v1/auth/reset/success";
    }

    @PostMapping(value = "/reset/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> resetConfirmJson(@RequestBody ConfirmResetDto dto) {
        authenticationService.confirmPasswordReset(dto.token(), dto.password(), dto.confirmPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/reset/success", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> success() {
        String html = """
                <!doctype html><html lang="ro"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
                <title>Parolă resetată</title>
                <style>body{font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:#0b1220;color:#e5e7eb}
                .wrap{max-width:420px;margin:18vh auto;padding:24px;background:#131a2a;border-radius:16px;text-align:center}
                a{color:#93c5fd}</style></head><body>
                <div class="wrap">
                  <h1>Parola a fost resetată cu succes</h1>
                  <p>Poți închide această pagină.</p>
                </div></body></html>
                """;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .body(html);
    }

    // Pas 1: cere OTP (nu divulgăm dacă email există)
    @PostMapping(value = "/forgot1", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgot(@RequestBody ForgotOtpDto dto) {
        authenticationService.requestPasswordResetOtp(dto.email());
        return ResponseEntity.accepted().build(); // 202 ACCEPTED
    }

    // Pas 2: verifică OTP + setează parola
    @PostMapping(value = "/reset1", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reset(@RequestBody ResetWithOtpDto dto) {
        authenticationService.resetPasswordWithOtp(dto.email(), dto.otp(), dto.password(), dto.confirmPassword());
        return ResponseEntity.noContent().build(); // 204 NO CONTENT
    }
}