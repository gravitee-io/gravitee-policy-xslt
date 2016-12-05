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

import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.http.stream.TransformableResponseStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.policy.xslt.configuration.PolicyScope;
import io.gravitee.policy.xslt.configuration.XSLTTransformationPolicyConfiguration;
import io.gravitee.policy.xslt.transformer.TransformerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XSLTTransformationPolicy {

    /**
     * XSLT transformation configuration
     */
    private final XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration;

    public XSLTTransformationPolicy(final XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration) {
        this.xsltTransformationPolicyConfiguration = xsltTransformationPolicyConfiguration;
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Response response, ExecutionContext executionContext) {
        if (xsltTransformationPolicyConfiguration.getScope() == null || xsltTransformationPolicyConfiguration.getScope() == PolicyScope.RESPONSE) {
            return TransformableResponseStreamBuilder
                    .on(response)
                    .contentType(MediaType.APPLICATION_XML)
                    .transform(toXSLT(executionContext))
                    .build();
        }

        return null;
    }

    @OnRequestContent
    public ReadWriteStream onRequestContent(Request request, ExecutionContext executionContext) {
        if (xsltTransformationPolicyConfiguration.getScope() == PolicyScope.REQUEST) {
            return TransformableRequestStreamBuilder
                    .on(request)
                    .contentType(MediaType.APPLICATION_XML)
                    .transform(toXSLT(executionContext))
                    .build();
        }

        return null;
    }

    public Function<Buffer, Buffer> toXSLT(ExecutionContext executionContext) {
        return input -> {
            try {
                // Get XSL stylesheet and transform it using internal template engine
                String stylesheet = executionContext.getTemplateEngine()
                        .convert(xsltTransformationPolicyConfiguration.getStylesheet());

                Templates template = TransformerFactory.getInstance().getTemplate(stylesheet);

                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setNamespaceAware(true);
                XMLReader r = spf.newSAXParser().getXMLReader();
                EntityResolver er = new CustomResolver();
                r.setEntityResolver(er);

                InputStream xslInputStream = new ByteArrayInputStream(input.getBytes());
                SAXSource saxSource = new SAXSource(r, new InputSource(xslInputStream));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Result result = new StreamResult(baos);
                Transformer transformer = template.newTransformer();

                // Add parameters
                if (xsltTransformationPolicyConfiguration.getParameters() != null) {
                    xsltTransformationPolicyConfiguration.getParameters().forEach(
                            parameter -> {
                                if (parameter.getName() != null && ! parameter.getName().trim().isEmpty()) {
                                    transformer.setParameter(parameter.getName(), parameter.getValue());
                                }
                            });
                }

                transformer.transform(saxSource, result);
                return Buffer.buffer(baos.toString());
            } catch (Exception ex) {
                throw new TransformationException("Unable to apply XSL Transformation: " + ex.getMessage(), ex);
            }
        };
    }

    class CustomResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
                return new InputSource(); // Do not allow unknown entities, by returning blank path
        }
    }
}
