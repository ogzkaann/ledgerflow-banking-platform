package dev.oguzkaandere.ledgerflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtValidationTest {
    private static final String ISSUER = "http://localhost:8090/realms/ledgerflow";
    private static final String AUDIENCE = "ledgerflow-api";

    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUpKeys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        var publicKey = (RSAPublicKey) pair.getPublic();
        var privateKey = (RSAPrivateKey) pair.getPrivate();
        var rsa = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("ledgerflow-test")
                .build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsa)));
        var nimbus = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER), new JwtAudienceValidator(AUDIENCE)));
        this.decoder = nimbus;
    }

    @Test
    void acceptsRealRs256SignatureIssuerAudienceAndLifetime() {
        var jwt = decoder.decode(token(ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300)));

        assertThat(jwt.getSubject()).isEqualTo("operator-test");
        assertThat(jwt.getAudience()).contains(AUDIENCE);
    }

    @Test
    void rejectsWrongIssuerAudienceExpiredMalformedAndUnsupportedAlgorithm() {
        assertThatThrownBy(() -> decoder.decode(token(
                        "https://wrong.example",
                        List.of(AUDIENCE),
                        Instant.now().plusSeconds(300))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(
                        token(ISSUER, List.of("wrong-audience"), Instant.now().plusSeconds(300))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(
                        token(ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(120))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode("not-a-jwt")).isInstanceOf(JwtException.class);

        String rs256 = token(ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));
        String unsupported = "eyJhbGciOiJIUzI1NiJ9." + rs256.substring(rs256.indexOf('.') + 1);
        assertThatThrownBy(() -> decoder.decode(unsupported)).isInstanceOf(JwtException.class);
    }

    private String token(String issuer, List<String> audience, Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(300) : now.minusSeconds(1);
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("operator-test")
                .audience(audience)
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .claim("realm_access", java.util.Map.of("roles", List.of("ledgerflow-operator")))
                .build();
        var headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId("ledgerflow-test")
                .build();
        return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
}
