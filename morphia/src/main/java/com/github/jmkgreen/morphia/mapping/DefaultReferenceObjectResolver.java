package com.github.jmkgreen.morphia.mapping;

/**
 * User: mutyonok
 * Date: 13.12.12
 * Time: 21:23
 */
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class DefaultReferenceObjectResolver implements ReferenceObjectResolver {
    public DBObject resolveObject(DBObject dbObject, DBRef dbRef, MappedField mf) {
        return dbRef.fetch();
    }

}

