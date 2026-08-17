package com.ledger.ledger;

import org.springframework.security.access.AccessDeniedException;

/**
 * A write refused because the token carries no tenant, as distinct from one refused for
 * lacking a permission.
 *
 * <p>A distinct type rather than a distinguishing message: the error handler has to tell
 * these apart to describe them honestly, and matching on message text would couple that
 * decision to a string anyone might reword without realising what depended on it.
 */
public class TenantRequiredException extends AccessDeniedException {

    public TenantRequiredException(String message) {
        super(message);
    }
}
