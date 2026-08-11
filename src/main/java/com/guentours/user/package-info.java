/**
 * User module: account lifecycle, including transparent auto-provisioning of
 * an account during checkout when a guest email is not yet registered.
 *
 * <p>{@code type = OPEN}: {@code User} (user.domain) and {@code UserService}/
 * {@code PendingPasswordResetLinkSource}-style ports (user.service) are read directly by several
 * other modules (security, booking, notification, partners, reseller) that all fundamentally need
 * identity data, rather than only through this module's root package.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.guentours.user;
