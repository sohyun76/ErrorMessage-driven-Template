/*
 * Copyright 2013-2015 Urs Wolfer
 *
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

package com.urswolfer.gerrit.client.rest.http.config;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

import com.google.gerrit.extensions.api.config.Server;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.JsonElement;
import com.urswolfer.gerrit.client.rest.http.GerritRestClient;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Urs Wolfer
 */
public class ServerRestClient extends Server.NotImplemented implements Server {
    private final GerritRestClient gerritRestClient;
    private final AtomicReference<String> version = new AtomicReference<String>();

    public ServerRestClient(GerritRestClient gerritRestClient) {
        this.gerritRestClient = gerritRestClient;
    }

    @Override
    public String getVersion() throws RestApiException {
        try {
            JsonElement jsonElement = gerritRestClient.getRequest("/config/server/version");
            version.set(jsonElement.getAsString());
            return version.get();
        } catch (HttpStatusException e) {
            int statusCode = e.getStatusCode();
            if (statusCode == SC_NOT_FOUND) { // Gerrit older than 2.8
                return "<2.8";
            } else {
                throw e;
            }
        }
    }

    public String getVersionCached() throws RestApiException {
        String gerritVersion = version.get();
        return gerritVersion == null ? getVersion() : gerritVersion;
    }
}
