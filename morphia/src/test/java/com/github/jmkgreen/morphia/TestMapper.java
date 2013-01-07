package com.github.jmkgreen.morphia;

import com.github.jmkgreen.morphia.annotations.*;
import com.github.jmkgreen.morphia.mapping.lazy.LazyFeatureDependencies;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests mapper functions; this is tied to some of the internals.
 *
 * @author scotthernandez
 *
 */
public class TestMapper extends TestBase {
	public static class A {
		static int loadCount = 0;
		@Id ObjectId id;

		String getId() {
			return id.toString();
		}

		@PostLoad
		protected void postConstruct() {
			if (A.loadCount > 1) {
				throw new RuntimeException();
			}

			A.loadCount++;
		}
	}

	@Entity("holders")
	public static class HoldsMultipleA {
		@Id ObjectId id;
		@Reference
		A a1;
		@Reference
		A a2;
	}

	@Entity("holders")
	public static class HoldsMultipleALazily {
		@Id ObjectId id;
		@Reference(lazy = true)
		A a1;
		@Reference
		A a2;
		@Reference(lazy = true)
		A a3;
	}

	public static class CustomId implements Serializable {

		private static final long serialVersionUID = 1L;

		@Property("v")
		ObjectId id = new ObjectId();
		@Property("t")
		String type;

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof CustomId)) {
				return false;
			}
			CustomId other = (CustomId) obj;
			if (id == null) {
				if (other.id != null) {
					return false;
				}
			} else if (!id.equals(other.id)) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CustomId [");
			if (id != null) {
				builder.append("id=").append(id).append(", ");
			}
			if (type != null) {
				builder.append("type=").append(type);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	public static class UsesCustomIdObject {
		@Id CustomId id;
		String text;

        public UsesCustomIdObject() {
        }

        public UsesCustomIdObject(CustomId id, String text) {
            this.id = id;
            this.text = text;
        }
    }

	public static class ReferencesCustomId {
		@Id String id;
		@Reference UsesCustomIdObject child;
		@Reference
        List<UsesCustomIdObject> children = new ArrayList<UsesCustomIdObject>();
        @Reference
        Map<String, UsesCustomIdObject> childMap = new HashMap<String, UsesCustomIdObject>();
	}

    /**
     * Should only load one instance of A.
     * Ensure {@link A#id A's id} is zero at start of each test
     * using it!
     * @throws Exception
     */
	@Test
	public void SingleLookup() throws Exception {
		A.loadCount = 0;
		A a = new A();
		HoldsMultipleA holder = new HoldsMultipleA();
		holder.a1 = a;
		holder.a2 = a;
		ds.save(a, holder);
		holder = ds.get(HoldsMultipleA.class, holder.id);
		Assert.assertEquals(1, A.loadCount);
		Assert.assertTrue(holder.a1 == holder.a2);
	}

	@Test
	public void SingleProxy() throws Exception {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled())
        {
            return;
        }

		A.loadCount = 0;
		A a = new A();
		HoldsMultipleALazily holder = new HoldsMultipleALazily();
		holder.a1 = a;
		holder.a2 = a;
		holder.a3 = a;
		ds.save(a, holder);
		Assert.assertEquals(0, A.loadCount);
		holder = ds.get(HoldsMultipleALazily.class, holder.id);
		Assert.assertNotNull(holder.a2);
		Assert.assertEquals(1, A.loadCount);
		Assert.assertFalse(holder.a1 == holder.a2);
		// FIXME currently not guaranteed:
		// Assert.assertTrue(holder.a1 == holder.a3);

		// A.loadCount=0;
		// Assert.assertEquals(holder.a1.getId(), holder.a2.getId());

	}

	@Test
	public void SerializableId() throws Exception {
		CustomId cId = new CustomId();
		cId.id = new ObjectId();
		cId.type = "banker";

		UsesCustomIdObject ucio = new UsesCustomIdObject();
		ucio.id = cId;
		ucio.text = "hllo";
		ds.save(ucio);
	}

    @Test
    public void testName() throws Exception {
        DBCollection test = db.getCollection("test");
        DBCollection test2 = db.getCollection("test2");
        test.save(new BasicDBObject("_id", new BasicDBObject("name", "Alex").append("email", "test@example.com")));
        test2.save(new BasicDBObject("_id", "test2").append("ref", new DBRef(db, "test", new BasicDBObject("name", "Alex").append("email", "test@example.com"))));
        DBObject testOne = test2.findOne(new BasicDBObject("_id", "test2"));
        System.out.println(testOne);
    }

    @Test
	public void ReferenceCustomId() throws Exception {
        ads.setDecoderFact(new DBDecoderFactory() {
            public DBDecoder create() {
                return new DefaultDBDecoder() {
                    @Override
                    public DBCallback getDBCallback(DBCollection collection) {
                        return new MyDBCallback(collection);
                    }
                };
            }
        });
		CustomId cId = new CustomId();
		cId.type = "banker";

        UsesCustomIdObject child1 = new UsesCustomIdObject(cId, "hello world");

		ReferencesCustomId parent = new ReferencesCustomId();
		parent.id = "testId";
        parent.child = child1;
        parent.children.add(child1);
        parent.childMap.put("1", child1);

		ds.save(child1);
        ds.save(parent);

        ReferencesCustomId fetched = ds.get(ReferencesCustomId.class, parent.id);

        assertNotNull(fetched);

        assertNotNull(fetched.child);
        assertEquals(child1.id, fetched.child.id);
        assertEquals(child1.text, fetched.child.text);

        assertNotNull(fetched.children);
        assertEquals(1, fetched.children.size());
        assertEquals(child1.id, fetched.children.get(0).id);
        assertEquals(child1.text, fetched.children.get(0).text);
    }

}
