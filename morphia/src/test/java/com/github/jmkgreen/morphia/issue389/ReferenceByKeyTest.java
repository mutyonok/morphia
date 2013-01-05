package com.github.jmkgreen.morphia.issue389;

import com.github.jmkgreen.morphia.TestBase;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Reference;
import com.github.jmkgreen.morphia.mapping.MappedField;
import com.github.jmkgreen.morphia.mapping.ReferenceObjectResolver;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mutyonok
 * Date: 13.12.12
 * Time: 22:53
 */
public class ReferenceByKeyTest extends TestBase {
    static class ReferenceByKeyResolver implements ReferenceObjectResolver {

        public DBObject resolveObject(DBObject dbObject, DBRef dbRef, MappedField mf) {
            String field = mf.getAnnotation(Reference.class).referenceField();
            DBObject query = new BasicDBObject(field, dbRef.getId()) ;
            return dbRef.getDB().getCollection(dbRef.getRef()).findOne(query);
        }
    }
    static class Father {
        @Id
        ObjectId id;

        @Reference(referenceField = "name", objectResolver = ReferenceByKeyResolver.class)
        List<Son> sons = new ArrayList<Son>();
    }
    static class Son {
        @Id ObjectId id;

        String name;

        Son() {
        }

        public Son(String name) {
            this.name = name;
        }
    }
    @Test
    public void testEntityLoadedByKey() throws Exception {
        morphia.map(Son.class, Father.class);


        Son first = new Son("first");
        ds.save(first, new Son("second"));
        Father father = new Father();
        father.sons = Arrays.asList(first);

        ds.save(father);

        Father father1 = ds.get(Father.class, father.id);
        assertNotNull(father1);
        assertNotNull(father1.sons);
        assertEquals(2, father1.sons.size());
    }

    private DBRef createDBRef(String id) {
        return new DBRef(db, "Son", new BasicDBObject("name", id));
    }
}
