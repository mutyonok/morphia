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
            return dbRef.getDB().getCollection(dbRef.getRef()).findOne(dbRef.getId());
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

        Son(String name) {
            this.name = name;
        }
    }
    @Test
    public void testEntityLoadedByKey() throws Exception {
        morphia.map(Son.class, Father.class);
        ds.save(new Son("first"), new Son("second"));
        BasicDBList sons = new BasicDBList();
        sons.addAll(Arrays.asList(createDBRef("first"), createDBRef("second"), createDBRef("third")));
        BasicDBObject father = new BasicDBObject("sons", sons);
        ds.getCollection(Father.class).insert(father, WriteConcern.SAFE);

        Object id = father.get("_id");

        Father father1 = ds.get(Father.class, id);
        assertNotNull(father1);
        assertNotNull(father1.sons);
        assertEquals(2, father1.sons.size());
    }

    private DBRef createDBRef(String id) {
        return new DBRef(db, "Son", new BasicDBObject("name", id));
    }
}
