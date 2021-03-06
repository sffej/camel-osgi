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

package org.apache.camel.osgi.service;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.osgi.service.filter.Filters;
import org.apache.camel.osgi.service.util.OsgiDefaultProxyCreator;
import org.apache.camel.osgi.service.util.OsgiServiceList;
import org.apache.camel.util.ServiceHelper;
import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.Map;

/**
 * The {@code OsgiDefaultProducer} is responsible for sending exchanges to registered consumers.
 * <p/>
 * Sending to consumer policy is determined by the corresponding {@link #processor} which at the moment can be one of
 * {@link OsgiDefaultLoadBalancer}, {@link org.apache.camel.processor.loadbalancer.RandomLoadBalancer},
 * {@link org.apache.camel.processor.loadbalancer.RoundRobinLoadBalancer},
 * {@link org.apache.camel.processor.MulticastProcessor}.
 */
public class OsgiDefaultProducer extends DefaultProducer {

    /**
     * Dynamic list of OSGi services to send an exchange to.
     * <p/>
     * This list can be updated dynamically as new OSGi services leaves, arrives.
     */
    protected final OsgiServiceList<Processor> services;

    /**
     * The processor that is responsible for processing a given exchange, i.e. send it to OSGi consumers.
     */
    protected Processor processor;

    public OsgiDefaultProducer(OsgiDefaultEndpoint endpoint, Map<String, Object> props) {
        super(endpoint);
        this.services = new OsgiServiceList<Processor>(
            endpoint.getApplicationBundleContext(),
            Filters.allEq(props).value(),
            endpoint.getComponentClassLoader(),
            new OsgiDefaultProxyCreator());
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        processor.process(exchange);
    }

    /**
     * Creates a processor that is responsible for processing a given exchange, i.e. send it to OSGi consumers.
     *
     * @return processor that is responsible for processing a given exchange
     */
    protected Processor createProcessor() {
        return new OsgiDefaultLoadBalancer() {
            @Override
            public List<Processor> getProcessors() {
                return services;
            }
        };
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        services.startTracking();
        if(processor == null) {
            processor = createProcessor();
        }
        ServiceHelper.startService(processor);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(processor);
        services.stopTracking();
        super.doStop();
    }

    @Override
    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownService(processor);
        super.doShutdown();
    }

    @Override
    public OsgiDefaultEndpoint getEndpoint() {
        return (OsgiDefaultEndpoint) super.getEndpoint();
    }

    protected BundleContext getApplicationBundleContext() {
        return getEndpoint().getApplicationBundleContext();
    }
}

