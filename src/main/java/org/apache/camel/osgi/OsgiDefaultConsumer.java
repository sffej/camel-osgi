package org.apache.camel.osgi;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.SuspendableService;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OsgiDefaultConsumer extends ServiceSupport implements Consumer, SuspendableService, Processor {

    private ServiceRegistration registration;
    private OsgiDefaultEndpoint endpoint;
    private Map<String, Object> props;
    private Processor processor;

    public OsgiDefaultConsumer(OsgiDefaultEndpoint endpoint, Processor processor, Map<String, Object> props) {
        this.endpoint = endpoint;
        this.props = props;
        this.processor = processor;
    }

    @Override
    protected void doStart() throws Exception {
        ServiceHelper.startServices(processor);
        register();
    }

    @Override
    protected void doStop() throws Exception {
        unregister();
        ServiceHelper.stopServices(processor);
    }

    @Override
    protected void doResume() throws Exception {
        register();
    }

    @Override
    protected void doSuspend() throws Exception {
        unregister();
    }

    @Override
    public OsgiDefaultEndpoint getEndpoint() {
        return endpoint;
    }

    protected BundleContext getBundleContext() {
        return getEndpoint().getAppBundleContext();
    }

    protected void register() {
        if(this.registration == null) {
            this.registration = getBundleContext().registerService(
                    OsgiComponent.OBJECT_CLASS, this, new Hashtable<String, Object>(props));
        }
    }

    protected void unregister() {
        if(this.registration != null) {
            this.registration.unregister();
            this.registration = null;
        }
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        Exchange copy = copyExchange(exchange);
        try {
            processor.process(copy);
        } finally {
            ExchangeHelper.copyResults(exchange, copy);
        }
    }

    /**
     * Creates exchange copy, so that {@link org.apache.camel.Exchange#getContext()} will return the {@code CamelContext}
     * of the this consumer endpoint instead of the producer endpoint, that sent the provided exchange.
     *
     * @param exchange an exchange to copy
     *
     * @return exchange copy
     */
    protected Exchange copyExchange(Exchange exchange) {
        OsgiDefaultEndpoint endpoint = getEndpoint();

        DefaultExchange copy = new DefaultExchange(endpoint.getCamelContext(), exchange.getPattern());
        copy.setFromEndpoint(endpoint);
        copy.setProperty(Exchange.CORRELATION_ID, exchange.getExchangeId());

        if (exchange.hasProperties()) {
            copy.setProperties(new ConcurrentHashMap<String, Object>(exchange.getProperties()));
        }
        copy.getIn().copyFrom(exchange.getIn());
        if (exchange.hasOut()) {
            copy.getOut().copyFrom(exchange.getOut());
        }
        copy.setException(exchange.getException());
        return copy;
    }

}
