package org.apache.sling.servlets.oidc_rp;

public class OidcToken {

    private final OidcTokenState state;
    private final String value;

    public OidcToken(OidcTokenState state, String value) {
        this.state = state;
        this.value = value;
    }

    public OidcTokenState getState() {
        return state;
    }

    public String getValue() {
        if ( state != OidcTokenState.VALID )
            throw new IllegalStateException("Can't retrieve a token value when the token state is "  + state);
        return value;
    }
}
