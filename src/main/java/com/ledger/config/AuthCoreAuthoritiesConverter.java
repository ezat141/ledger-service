package com.ledger.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AuthCoreAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (String scope : claimAsList(jwt, "scope")) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        for (String role : claimAsList(jwt, "roles")) {
            authorities.add(new SimpleGrantedAuthority(
                    role.startsWith("ROLE_") ? role : "ROLE_" + role));
        }
        for (String permission : claimAsList(jwt, "permissions")) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        return authorities;
    }

    /** The {@code scope} claim may arrive as a list or as a space-delimited string. */
    private static List<String> claimAsList(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim == null) {
            return List.of();
        }
        if (claim instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        String value = String.valueOf(claim).trim();
        return value.isEmpty() ? List.of() : List.of(value.split("\\s+"));
    }
}
