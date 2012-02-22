package esl.cuenet.mapper.tree;

import esl.cuenet.source.*;

import java.util.HashMap;

public class SourceMapper {

    private HashMap<String, String> shorthandNamespaceMap = new HashMap<String, String>();
    private HashMap<String, ISource> sourceMap = new HashMap<String, ISource>();
    private AccessorFactory accessorFactory = AccessorFactory.getInstance();
    private MapperFactory mapperFactory = MapperFactory.getInstance();

    protected SourceMapper() {

    }

    private static SourceMapper sourceMapper = new SourceMapper();

    public static SourceMapper constructSourceMapper() {
        return sourceMapper;
    }

    public void addNamespaceMapping(String uri, String shorthand) {
        if (shorthandNamespaceMap.containsKey(shorthand))
            throw new RuntimeException("");

        shorthandNamespaceMap.put(shorthand, uri);
    }
    
    public ISource createSource (String name) {

        IAccessor accessor = accessorFactory.getAccessor(name);
        if (accessor == null) throw new SourceInitializationException("Accessor not available for " + name);
        
        IMapper mapper = mapperFactory.get();
        if (mapper == null) throw new SourceInitializationException("Mapper not available for " + name);

        ISource source = new Source(name, accessor, mapper);
        sourceMap.put(name, source);

        return source;

    }
    
    public ISource get (String sourceName) {
        return sourceMap.get(sourceName);
    }

}
