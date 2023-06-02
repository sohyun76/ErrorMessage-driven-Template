/**
 * Copyright © 2010-2011 Nokia
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
package com.github.restdriver.serverdriver.acceptance;

import static com.github.restdriver.serverdriver.RestServerDriver.*;
import static com.github.restdriver.serverdriver.polling.Poller.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.restdriver.clientdriver.ClientDriverRequest;
import com.github.restdriver.clientdriver.ClientDriverResponse;
import com.github.restdriver.clientdriver.ClientDriverRule;
import com.github.restdriver.clientdriver.exception.ClientDriverFailedExpectationException;
import com.github.restdriver.serverdriver.polling.Poller;

public class PollingAcceptanceTest {
    
    @Rule
    public ClientDriverRule driver = new ClientDriverRule();
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private String baseUrl;
    
    @Before
    public void getServerDetails() {
        baseUrl = driver.getBaseUrl();
        driver.addExpectation(new ClientDriverRequest("/"), new ClientDriverResponse("NOT YET...", "text/plain"));
        driver.addExpectation(new ClientDriverRequest("/"), new ClientDriverResponse("NOT YET...", "text/plain"));
        driver.addExpectation(new ClientDriverRequest("/"), new ClientDriverResponse("NOW!", "text/plain"));
    }
    
    @Test
    public void pollerReturnsSuccessEventually() {
        new Poller() {
            @Override
            public void poll() {
                loudly();
                assertThat(get(baseUrl).asText(), is("NOW!"));
            }
        };
    }
    
    @Test
    public void pollerTriesCorrectNumberOfTimes() {
        expectedException.expect(ClientDriverFailedExpectationException.class);
        
        new Poller(times(2)) { // not enough times!
            @Override
            public void poll() {
                assertThat(get(baseUrl).asText(), is("NOW!"));
            }
        };
    }
    
    @Test
    public void pollerTriesCorrectNumberOfTimesWithDurationSpecified() {
        expectedException.expect(ClientDriverFailedExpectationException.class);
        
        new Poller(times(2), every(100, TimeUnit.MILLISECONDS)) { // not enough times!
            @Override
            public void poll() {
                assertThat(get(baseUrl).asText(), is("NOW!"));
            }
        };
    }
    
}
