/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.oidchandler.core.util;

public class OPConstant {

    //Google Details
    public static final String google_issuer_url = "https://accounts.google.com";
    public static final String google_authorization_endpoint = "https://accounts.google.com/o/oauth2/auth";
    public static final String google_token_endpoint = "https://www.googleapis.com/oauth2/v4/token";
    public static final String google_userinfo_endpoint = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String google_revocation_endpoint = "https://accounts.google.com/o/oauth2/revoke";
    public static final String google_jwks_uri = "https://www.googleapis.com/oauth2/v3/certs";

    //Relying Party Details
    public static final String oidc_callback_url = "http://localhost:8080/";
    public static final String oidc_scope = "openid email profile";
}
