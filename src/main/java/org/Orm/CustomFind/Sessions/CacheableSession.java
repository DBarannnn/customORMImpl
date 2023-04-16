package org.Orm.CustomFind.Sessions;


import org.Orm.CustomFind.Records.EntityKey;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class CacheableSession extends Session{
    private final Map<EntityKey<?>, Object> cachedEntities = new HashMap<>();

    public CacheableSession(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public <T> T find(Class<T> type, Long id){
        EntityKey<T> key = new EntityKey<>(type, id);
        Object result = cachedEntities.computeIfAbsent(key,k -> super.find(type, id) );
        return type.cast(result);
    }

}
