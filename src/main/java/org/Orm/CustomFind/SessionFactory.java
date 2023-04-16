package org.Orm.CustomFind;

import lombok.RequiredArgsConstructor;
import org.Orm.CustomFind.Sessions.CacheableSession;
import org.Orm.CustomFind.Sessions.Session;

import javax.sql.DataSource;

@RequiredArgsConstructor
public class SessionFactory {
    private final DataSource dataSource;


    public Session createSession(){
        return new Session(dataSource);
    }

    public CacheableSession createCacheableSession(){
        return new CacheableSession(dataSource);
    }


}
