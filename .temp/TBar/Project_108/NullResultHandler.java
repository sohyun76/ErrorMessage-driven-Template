/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.impl.result;

import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.result.ResultHandler;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullResultHandler implements ResultHandler {

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure(CommandResult result) {
    }

    @Override
    public void onValidationFailure(CommandResult result, Exception exception) {
    }

    @Override
    public void onExecutionFailure(CommandResult result, CommandException exception) {
    }
}
