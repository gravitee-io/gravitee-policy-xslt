/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.policy.xslt;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.xslt.configuration.XSLTTransformationPolicyConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class XSLTTransformationPolicyTest {

    @Mock
    private XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration;

    private XSLTTransformationPolicy xsltTransformationPolicy;

    @Mock
    protected ExecutionContext executionContext;

    @Before
    public void init() {
        initMocks(this);

        xsltTransformationPolicy = new XSLTTransformationPolicy(xsltTransformationPolicyConfiguration);
    }

    @Test
    public void shouldTransformInput() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");
        String expected = loadResource("/io/gravitee/policy/xslt/output01.xml");

        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);
        when(executionContext.getTemplateEngine()).thenReturn(new MockTemplateEngine());

        Buffer ret = xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
        Assert.assertNotNull(ret);

        Diff diff = DiffBuilder.compare(expected).ignoreWhitespace().withTest(ret.toString()).checkForIdentical().build();
        Assert.assertFalse("XML identical " + diff.toString(), diff.hasDifferences());
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionForInvalidStylesheet() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet_invalid.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");

        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);
        when(executionContext.getTemplateEngine()).thenReturn(new MockTemplateEngine());

        xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionForExternalEntityInjection() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file02.xml");

        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);
        when(executionContext.getTemplateEngine()).thenReturn(new MockTemplateEngine());

        xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
    }

    private String loadResource(String resource) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resource);
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw, "UTF-8");
        return sw.toString();
    }

    private class MockTemplateEngine implements TemplateEngine {

        @Override
        public String convert(String s) {
            return s;
        }

        @Override
        public <T> T getValue(String expression, Class<T> clazz) {
            return null;
        }

        @Override
        public TemplateContext getTemplateContext() {
            return null;
        }
    }
}
