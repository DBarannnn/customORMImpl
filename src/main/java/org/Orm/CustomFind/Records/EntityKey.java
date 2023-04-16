package org.Orm.CustomFind.Records;

import lombok.Getter;


public record EntityKey<T> (@Getter Class<T> type, @Getter Long id) {

}