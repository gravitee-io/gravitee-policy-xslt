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
package io.gravitee.policy.xslt.transformer;

import io.gravitee.policy.xslt.transformer.saxon.SaxonTransformerFactory;
import io.gravitee.policy.xslt.utils.Sha1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class TransformerFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(TransformerFactory.class);

    private final Map<String, Templates> templateCache = new HashMap<>();

    private static TransformerFactory _instance = new TransformerFactory();

    public static TransformerFactory getInstance() {
        return _instance;
    }

    public Templates getTemplate(String xslt) throws Exception {
        String sha1 = Sha1.sha1(xslt);
        return templateCache.getOrDefault(sha1, createTemplate(xslt));
    }

    private Templates createTemplate(String xslt) throws Exception {
        javax.xml.transform.TransformerFactory factory = getTransformerFactory();
        StreamSource xslStream = new StreamSource(new StringReader(xslt));
        try {
            Templates templates = factory.newTemplates(xslStream);
            templates.getOutputProperties().setProperty(OutputKeys.INDENT, "yes");
            templates.getOutputProperties().setProperty("{http://xml.apache.org/xslt}indent-amount", "3");
            return templates;
        } catch (TransformerConfigurationException tcex) {
            LOGGER.error("An error occurs while getting the template from XSLT", tcex);
            throw tcex;
        }
    }

    private javax.xml.transform.TransformerFactory getTransformerFactory() {
        return javax.xml.transform.TransformerFactory.newInstance(
                SaxonTransformerFactory.class.getName(), this.getClass().getClassLoader());
    }
}
