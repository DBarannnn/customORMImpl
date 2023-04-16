package org.Orm;

import org.Orm.CustomFind.Sessions.Person;
import org.Orm.CustomFind.SessionFactory;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class Demo {
    public static void main(String[] args) {
        DataSource dataSource = initializeDataSource();
        SessionFactory sessionFactory = new SessionFactory(dataSource);
        var session = sessionFactory.createSession();

        Person person = session.find(Person.class, 1L);
        person.setFirstName("Dmytro");
        person.setLastName("Barann");

        Person person2 = session.find(Person.class, 2L);
        person2.setLastName("Hello");


        session.close();

    }

    public static DataSource initializeDataSource(){
        PGSimpleDataSource dataSource = new PGSimpleDataSource();

        dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("root");

        return dataSource;
    }
}
