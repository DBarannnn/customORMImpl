package org.Orm.CustomFind.Sessions;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.Orm.CustomFind.Annotations.Column;
import org.Orm.CustomFind.Annotations.Id;
import org.Orm.CustomFind.Annotations.Table;


@Table("persons")
@Setter
@EqualsAndHashCode
public class Person {
    @Id
    @Column("id")
    Long id;
    @Column("first_name")
    String firstName;
    @Column("last_name")
    String lastName;
}
