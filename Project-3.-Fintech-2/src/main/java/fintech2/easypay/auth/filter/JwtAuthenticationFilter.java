package fintech2.easypay.auth.filter;

import fintech2.easypay.auth.dto.UserPrincipal;
import fintech2.easypay.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String phoneNumber;
        
        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Bearer 제거하고 JWT 토큰 추출
        jwt = authHeader.substring(7);
        
        try {
            // JWT에서 휴대폰 번호 추출
            phoneNumber = jwtService.extractPhoneNumber(jwt);
            
            // 휴대폰 번호가 있고, 현재 인증된 사용자가 없으면
            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 사용자 정보 조회
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(phoneNumber);
                
                // JWT 토큰이 유효하면 인증 설정
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
        } catch (Exception e) {
            // JWT 토큰이 유효하지 않으면 로그만 남기고 계속 진행
            logger.warn("Invalid JWT token: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
} 