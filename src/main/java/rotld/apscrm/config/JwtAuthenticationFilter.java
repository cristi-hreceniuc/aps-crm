package rotld.apscrm.config;


import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import rotld.apscrm.services.JwtService;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String subject = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (subject != null && authentication == null) {
                // Check if this is a kid token (subject starts with "kid:")
                if (subject.startsWith("kid:")) {
                    // Kid authentication - create a simple authentication with KID authority
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            new KidPrincipal(subject, jwt, jwtService),
                            null,
                            List.of(new SimpleGrantedAuthority("KID"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // Regular user authentication
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(subject);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }

    /**
     * Simple principal for kid authentication
     */
    public record KidPrincipal(String subject, String token, JwtService jwtService) {
        public Long getProfileId() {
            return jwtService.extractClaim(token, claims -> claims.get("profile_id", Long.class));
        }

        public boolean isPremium() {
            return Boolean.TRUE.equals(jwtService.extractClaim(token, claims -> claims.get("is_premium", Boolean.class)));
        }

        public String getSpecialistId() {
            return jwtService.extractClaim(token, claims -> claims.get("specialist_id", String.class));
        }
    }
}