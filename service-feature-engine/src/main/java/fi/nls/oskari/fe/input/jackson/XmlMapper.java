package fi.nls.oskari.fe.input.jackson;


import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.XmlSerializerProvider;
import com.fasterxml.jackson.dataformat.xml.util.XmlRootNameLookup;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

/**
 * Customized {@link ObjectMapper} that will read and write XML instead of JSON,
 * using XML-backed {@link com.fasterxml.jackson.core.JsonFactory}
 * implementation ({@link XmlFactory}).
 *<p>
 * Mapper itself overrides some aspects of functionality to try to handle
 * data binding aspects as similar to JAXB as possible.
 */
public class XmlMapper extends ObjectMapper
{
    // as of 2.5
    private static final long serialVersionUID = -1212805483143619605L;

    protected final static JacksonXmlModule DEFAULT_XML_MODULE = new JacksonXmlModule();
    
    // need to hold on to module instance just in case copy() is used
    protected final JacksonXmlModule _xmlModule;
    
    /*
    /**********************************************************
    /* Life-cycle: construction, configuration
    /**********************************************************
     */

    public XmlMapper() {
        this(new XmlFactory());
    }

    /** @since 2.4 */
    public XmlMapper(XMLInputFactory inputF, XMLOutputFactory outF) {
        this(new XmlFactory(inputF, outF));
    }

    /** @since 2.4 */
    public XmlMapper(XMLInputFactory inputF) {
        this(new XmlFactory(inputF));
    }

    public XmlMapper(XmlFactory xmlFactory) {
        this(xmlFactory, DEFAULT_XML_MODULE);
    }

    public XmlMapper(JacksonXmlModule module) {
        this(new XmlFactory(), module);
    }

    public XmlMapper(XmlFactory xmlFactory, JacksonXmlModule module)
    {
        /* Need to override serializer provider (due to root name handling);
         * deserializer provider fine as is
         */
        super(xmlFactory, new XmlSerializerProvider(new XmlRootNameLookup()), null);
        _xmlModule = module;
        // but all the rest is done via Module interface!
        if (module != null) {
            registerModule(module);
        }
    }
    
    // @since 2.1
    @Override
    public XmlMapper copy()
    {
        _checkInvalidCopy(XmlMapper.class);
        return new XmlMapper((XmlFactory) _jsonFactory.copy(), _xmlModule);
    }
    
    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Additional XML-specific configurations
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Access to configuration settings
    /**********************************************************
     */

    @Override
    public XmlFactory getFactory() {
        return (XmlFactory) _jsonFactory;
    }
    
    public ObjectMapper configure(ToXmlGenerator.Feature f, boolean state) {
        ((XmlFactory)_jsonFactory).configure(f, state);
        return this;
    }

    public ObjectMapper configure(FromXmlParser.Feature f, boolean state) {
        ((XmlFactory)_jsonFactory).configure(f, state);
        return this;
    }

    public ObjectMapper enable(ToXmlGenerator.Feature f) {
        ((XmlFactory)_jsonFactory).enable(f);
        return this;
    }

    public ObjectMapper enable(FromXmlParser.Feature f) {
        ((XmlFactory)_jsonFactory).enable(f);
        return this;
    }

    public ObjectMapper disable(ToXmlGenerator.Feature f) {
        ((XmlFactory)_jsonFactory).disable(f);
        return this;
    }

    public ObjectMapper disable(FromXmlParser.Feature f) {
        ((XmlFactory)_jsonFactory).disable(f);
        return this;
    }

    /*
    /**********************************************************
    /* XML-specific access
    /**********************************************************
     */

    /**
     * Method for reading a single XML value from given XML-specific input
     * source; useful for incremental data-binding, combining traversal using
     * basic Stax {@link XMLStreamReader} with data-binding by Jackson.
     * 
     * @since 2.4
     */
    public <T> T readValue(XMLStreamReader r, Class<T> valueType) throws IOException {
        return readValue(r, _typeFactory.constructType(valueType));
    } 

    /**
     * Method for reading a single XML value from given XML-specific input
     * source; useful for incremental data-binding, combining traversal using
     * basic Stax {@link XMLStreamReader} with data-binding by Jackson.
     * 
     * @since 2.4
     */
    public <T> T readValue(XMLStreamReader r, TypeReference<T> valueTypeRef) throws IOException {
        return readValue(r, _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method for reading a single XML value from given XML-specific input
     * source; useful for incremental data-binding, combining traversal using
     * basic Stax {@link XMLStreamReader} with data-binding by Jackson.
     * 
     * @since 2.4
     */
    @SuppressWarnings("resource")
    public <T> T readValue(XMLStreamReader r, JavaType valueType) throws IOException
    {
        FromXmlParser p = getFactory().createParser(r);
        return super.readValue(p,  valueType);
    } 

    /**
     * Method for serializing given value using specific {@link XMLStreamReader}:
     * useful when building large XML files by binding individual items, one at
     * a time.
     * 
     * @since 2.4
     */
    public void writeValue(XMLStreamWriter w0, Object value) throws IOException {
        @SuppressWarnings("resource")
        ToXmlGenerator g = getFactory().createGenerator(w0);
        super.writeValue(g, value);
        /* NOTE: above call should do flush(); and we should NOT close here.
         * Finally, 'g' has no buffers to release.
         */
    }
}
