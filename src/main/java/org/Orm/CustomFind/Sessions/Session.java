package org.Orm.CustomFind.Sessions;

import lombok.SneakyThrows;
import org.Orm.CustomFind.Annotations.Column;
import org.Orm.CustomFind.Annotations.Id;
import org.Orm.CustomFind.Annotations.Table;
import org.Orm.CustomFind.Records.EntityKey;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Session {
    private final static String SELECT_BY_ID_STATEMENT = "SELECT * FROM %s WHERE id = ?";
    private final DataSource dataSource;

    private final Map<EntityKey<?>, Object> cachedEntities = new HashMap<>();
    private final Map<EntityKey<?>, Object> entityCopies = new HashMap<>();

    public Session(DataSource dataSource){
        this.dataSource = dataSource;
    }

    public <T> T find(Class<T> type, Long id){
        EntityKey<T> key = new EntityKey<>(type, id);
        Object result = cachedEntities.computeIfAbsent(key,this::loadFromDb);
        return type.cast(result);
    }

    @SneakyThrows
    public <T> T loadFromDb(EntityKey<T> entityKey){
        Class<T> type = entityKey.type();
        Long id = entityKey.id();
        try(Connection connection = dataSource.getConnection()) {
            String selectQuery = prepareSelectQuery(type);
                try(PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)){
                    preparedStatement.setObject(1, id);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    return fromResultSetToObject(type, resultSet, entityKey);
                }
        }

    }

    @SneakyThrows
    private <T> T fromResultSetToObject(Class<T> type, ResultSet resultSet, EntityKey<T> entityKey) {
        resultSet.next();
        Constructor<T> constructor = type.getConstructor();
        T result = constructor.newInstance();
        T resultCopy = constructor.newInstance();

        for(Field field: type.getDeclaredFields()){
            Column annotation = field.getAnnotation(Column.class);
            String fieldName = annotation.value();
            field.setAccessible(true);
            field.set(result, resultSet.getObject(fieldName));
            field.set(resultCopy, resultSet.getObject(fieldName));
        }

        entityCopies.put(entityKey, resultCopy);
        return result;
    }


    private <T> String prepareSelectQuery(Class<T> type) {
        Table table = type.getAnnotation(Table.class);
        String tableName = table.value();
        return String.format(SELECT_BY_ID_STATEMENT, tableName);
    }

    @SneakyThrows
    public <T> boolean areTheSameObjects(Class<T> type,Object object1, Object object2){
        T firstEntity = type.cast(object1);
        T secondEntity = type.cast(object2);
        for(Field field: type.getDeclaredFields()){
            Object firstObjectField = field.get(firstEntity);
            Object secondObjectField = field.get(secondEntity);
            if(!firstEntity.equals(secondEntity)) return false;
        }
        return true;
    }



    public void close() {
        for (var entry : cachedEntities.entrySet()){
            EntityKey<?> entityKey = entry.getKey();
        Class<?> type = entityKey.getType();

        Object cachedObject = entry.getValue();
        Object initialObject = entityCopies.get(entry.getKey());

        if(!areTheSameObjects(type, cachedObject, initialObject)) {updateObject(prepareUpdateObject(type ,cachedObject, entityKey));}

        }
    }

    @SneakyThrows
    private<T> void updateObject(String preparedUpdateQuery) {


        try(var connection = dataSource.getConnection()){
            PreparedStatement preparedUpdateStatement = connection.prepareStatement(preparedUpdateQuery);
            preparedUpdateStatement.executeUpdate();

        }

    }

    @SneakyThrows
    public <T> String prepareUpdateObject(Class<T> type, Object cachedObject,  EntityKey<?> entityKey){
        //Cast updated Object
        T updateObject = type.cast(cachedObject);
        //
        String tableName = type.getAnnotation(Table.class).value();

        List<Field> declaredAnnotatedFields = findDeclaredAnnotatedFields(type);
        long number_of_db_columns = declaredAnnotatedFields.size();

        //Defining what field name is storing primary key
        String id_column_name = findIdColumn(type);
        String updateStatement = "UPDATE " + tableName + " SET ";
        StringBuilder stringBuilder = new StringBuilder(updateStatement);

        for(int i=0; i<number_of_db_columns; i++){
            String columnName = declaredAnnotatedFields.get(i).getAnnotation(Column.class).value();
            Object newColumnValue = declaredAnnotatedFields.get(i).get(updateObject);
            if(i == number_of_db_columns-1) {
                stringBuilder.append(columnName).append(" = ").append(formatObject(newColumnValue))
                        .append(" WHERE ").append(id_column_name).append(" = ").append(entityKey.id());
            }
            else stringBuilder.append(columnName).append(" = ").append(formatObject(newColumnValue)).append(",");
        }
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();

    }

    //Find all fields that are annotated with column
    private <T> List<Field> findDeclaredAnnotatedFields(Class<T> type){
        return   Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .toList();
    }

    //Find columnName that represents primary key
    private <T> String  findIdColumn(Class<T> type){
        List<Field> declaredAnnotatedFields = findDeclaredAnnotatedFields(type);

        Field id_field = declaredAnnotatedFields.stream().
                filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst().get();
        return  id_field.getAnnotation(Column.class).value();
    }

    //Convenient way to format object that is needed to be inserted in sql query/
    private Object formatObject(Object object){
        if(object instanceof String){
            return "'"+ object.toString() + "'";
        }
        return object;
    }

}
