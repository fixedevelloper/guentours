/**
 * Security module: password hashing, JWT issuance/validation and the Spring
 * Security filter chain. Bridges to the user module to load account details.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user"}
)
package com.guentours.security;
