package com.windschief.auth;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SpotifyIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private final SpotifyTokenValidator tokenValidator;

    @Inject
    public SpotifyIdentityProvider(SpotifyTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return tokenValidator.validateToken(request.getToken().getToken())
                .onItem().transform(spotifyUser -> {
                    if (spotifyUser != null) {
                        return QuarkusSecurityIdentity.builder()
                                .setPrincipal(spotifyUser::id)
                                .addRole("user")
                                .addAttribute("displayName", spotifyUser.displayName())
                                .addAttribute("email", spotifyUser.email())
                                .addAttribute("country", spotifyUser.country())
                                .addAttribute("spotifyToken", request.getToken().getToken())
                                .build();
                    }
                    return null;
                });
    }
}