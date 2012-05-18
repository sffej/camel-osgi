package org.apache.camel.osgi;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.TypeConverter;
import org.apache.camel.impl.DefaultComponent;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class OsgiComponent extends DefaultComponent {

    /**
     * OSGi service property that contains the name of the exposed into the OSGi registry camel processor.
     */
    protected static final String SERVICE_NAME_PROP = "camelOsgiEndpointName";

    /**
     * The value of the {@link Constants#OBJECTCLASS} property of the exposed into the OSGi registry camel processor.
     */
    protected static final String OBJECT_CLASS = Processor.class.getName();

    @Override
    protected Endpoint createEndpoint(String uri, String path, Map<String, Object> params) throws Exception {
        OsgiEndpointType endpointType = OsgiEndpointType.fromPath(path);
        OsgiDefaultEndpoint endpoint = endpointType.createEndpoint(uri, this);

        setProperties(endpoint, params);

        // properties that cannot be set on endpoint are exposed as published OSGi service
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.OBJECTCLASS, OBJECT_CLASS);
        props.put(SERVICE_NAME_PROP, endpointType.getName(path));
        for(Iterator<Entry<String, Object>> iter = params.entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, Object> entry = iter.next();
            props.put(entry.getKey(), entry.getValue());
            iter.remove();
        }

        // convert some of predefined properties
        convertProperties(props);

        endpoint.setProps(props);

        return endpoint;
    }

    protected void convertProperties(Map<String, Object> props) {
        // convert service id and ranking to the corresponding types
        if (props.containsKey(Constants.SERVICE_ID)) {
            props.put(Constants.SERVICE_ID, convertValue(props.get(Constants.SERVICE_ID), Long.class));
        }
        if (props.containsKey(Constants.SERVICE_RANKING)) {
            props.put(Constants.SERVICE_RANKING, convertValue(props.get(Constants.SERVICE_RANKING), Integer.class));
        }
    }

    protected Object convertValue(Object value, Class<?> clazz) {
        TypeConverter converter = getCamelContext().getTypeConverter();
        
        Object answer = converter.convertTo(clazz, value);
        return answer != null ? answer : value;
    }
    
    @Override
    protected boolean useIntrospectionOnEndpoint() {
        return false;
    }
}
