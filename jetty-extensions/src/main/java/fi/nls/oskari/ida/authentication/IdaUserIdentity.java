package fi.nls.oskari.ida.authentication;

import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.List;

/**
 * Created by urho.tamminen on 2.3.2015.
 */
public class IdaUserIdentity implements UserIdentity
{
    private Subject _subject;
    private Principal _principal;
    private List<String> _roles;

    public IdaUserIdentity(
        Subject subject, Principal principal, List<String> roles)
    {
        _subject = subject;
        _principal = principal;
        _roles = roles;
    }

    public Subject getSubject() {
        return _subject;
    }

    public Principal getUserPrincipal() {
        return _principal;
    }

    public boolean isUserInRole(String role, Scope scope) {
        return _roles.contains(role);
    }
}