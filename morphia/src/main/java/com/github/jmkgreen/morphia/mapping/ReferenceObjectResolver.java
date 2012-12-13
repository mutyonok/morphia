package com.github.jmkgreen.morphia.mapping;

/**
 * User: mutyonok
 * Date: 13.12.12
 * Time: 21:24
 */
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public interface ReferenceObjectResolver {
    DBObject resolveObject(final DBObject dbObject, final DBRef dbRef, final MappedField mf);
}

