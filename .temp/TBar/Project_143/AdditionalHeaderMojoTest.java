/**
 * Copyright (C) 2008 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.maven.plugin.license;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class AdditionalHeaderMojoTest {
    @Test
    public void test_additionalHeaderDefinitions() throws Exception {
        LicenseCheckMojo check = new LicenseCheckMojo();
        check.defaultBasedir = new File("src/test/resources/check/def");
        check.legacyConfigHeader = "src/test/resources/check/header.txt";
        check.project = new MavenProjectStub();
        check.legacyConfigExcludes = new String[]{"*.xml"};
        check.strictCheck = true;

        try {
            check.execute();
            fail();
        } catch (MojoExecutionException e) {
            assertEquals("Some files do not have the expected license header", e.getMessage());
        }

        check.defaultHeaderDefinitions = new String[]{"/check/def/additionalHeaderDefinitions.xml"};
        check.execute();
    }
}