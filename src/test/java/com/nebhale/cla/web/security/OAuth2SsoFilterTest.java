/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.cla.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.web.client.RestOperations;

import com.nebhale.cla.GitHubUtils;

public final class OAuth2SsoFilterTest {

    private final RestOperations restOperations = mock(RestOperations.class);

    private final OAuth2SsoFilter oAuth2SsoFilter = new OAuth2SsoFilter(new String[] { "test.domain" }, "test-url", this.restOperations);

    @SuppressWarnings("rawtypes")
    @Test
    public void attemptAuthentication() throws IOException, ServletException {
        Map<String, Object> email = new HashMap<>();
        email.put("email", "email@test.domain");
        email.put("verified", true);

        Set<Map<String, Object>> emailInfo = new HashSet<>();
        emailInfo.add(email);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("login", "test-login");

        when(this.restOperations.exchange("https://api.github.com/user/emails", HttpMethod.GET, GitHubUtils.v3HttpEntity(), Set.class)).thenReturn(
            new ResponseEntity<Set>(emailInfo, HttpStatus.OK));
        when(this.restOperations.getForObject("https://api.github.com/user", Map.class)).thenReturn(userInfo);

        Authentication authentication = this.oAuth2SsoFilter.attemptAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse());

        AdminUser expectedAdminUser = new AdminUser("test-login");
        assertEquals(expectedAdminUser, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertEquals(expectedAdminUser.getAuthorities().iterator().next(), authentication.getAuthorities().iterator().next());
    }

    @SuppressWarnings("rawtypes")
    @Test(expected = BadCredentialsException.class)
    public void attemptAuthenticationNonVerifiedEmail() throws IOException, ServletException {
        Map<String, Object> email = new HashMap<>();
        email.put("email", "email@test.domain");
        email.put("verified", false);

        Set<Map<String, Object>> emailInfo = new HashSet<>();
        emailInfo.add(email);

        when(this.restOperations.exchange("https://api.github.com/user/emails", HttpMethod.GET, GitHubUtils.v3HttpEntity(), Set.class)).thenReturn(
            new ResponseEntity<Set>(emailInfo, HttpStatus.OK));

        this.oAuth2SsoFilter.attemptAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    @SuppressWarnings("rawtypes")
    @Test(expected = BadCredentialsException.class)
    public void attemptAuthenticationNonValidAdminDomain() throws IOException, ServletException {
        Map<String, Object> email = new HashMap<>();
        email.put("email", "email@other.domain");
        email.put("verified", true);

        Set<Map<String, Object>> emailInfo = new HashSet<>();
        emailInfo.add(email);

        when(this.restOperations.exchange("https://api.github.com/user/emails", HttpMethod.GET, GitHubUtils.v3HttpEntity(), Set.class)).thenReturn(
            new ResponseEntity<Set>(emailInfo, HttpStatus.OK));

        this.oAuth2SsoFilter.attemptAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    @Test
    public void unsuccessfulAuthentication() throws IOException, ServletException {
        this.oAuth2SsoFilter.unsuccessfulAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse(),
            new BadCredentialsException(null));
    }

    @Test(expected = AccessTokenRequiredException.class)
    public void unsuccessfulAuthenticationWithAccessTokenRequiredException() throws IOException, ServletException {
        this.oAuth2SsoFilter.unsuccessfulAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse(),
            new AccessTokenRequiredException(null));
    }

}