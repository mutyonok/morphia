package com.mongodb;

import org.bson.BSONObject;
import org.bson.BasicBSONCallback;
import org.bson.types.ObjectId;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyDBCallback extends BasicBSONCallback implements DBCallback{
    public MyDBCallback( DBCollection coll ){
        _collection = coll;
        _db = _collection == null ? null : _collection.getDB();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void gotDBRef( String name , String ns , ObjectId id ){
        if ( id.equals( Bytes.COLLECTION_REF_ID ) )
            cur().put( name , _collection );
        else
            cur().put( name , new DBPointer( (DBObject)cur() , name , _db , ns , id ) );
    }

    @Override
    public void objectStart(boolean array, String name){
        names.addLast(name);
        super.objectStart( array , name );
    }

    /**
     * @return
     * @throws MongoException
     */
    @Override
    public BSONObject create(){
        return _create( null );
    }

    /**
     * @param array
     * @param path
     * @return
     * @throws MongoException
     */
    @Override
    public BSONObject create( boolean array , List<String> path ){
        if ( array )
            return new BasicDBList();
        return _create( path );
    }

    private DBObject _create( List<String> path ){

        Class c = null;

        if ( _collection != null && _collection._objectClass != null){
            if ( path == null || path.size() == 0 ){
                c = _collection._objectClass;
            }
            else {
                StringBuilder buf = new StringBuilder();
                for ( int i=0; i<path.size(); i++ ){
                    if ( i > 0 )
                        buf.append(".");
                    buf.append( path.get(i) );
                }
                c = _collection.getInternalClass( buf.toString() );
            }

        }

        if ( c != null ){
            try {
                return (DBObject)c.newInstance();
            }
            catch ( InstantiationException ie ){
                LOGGER.log( Level.FINE , "can't create a: " + c , ie );
                throw new MongoInternalException( "can't instantiate a : " + c , ie );
            }
            catch ( IllegalAccessException iae ){
                LOGGER.log( Level.FINE , "can't create a: " + c , iae );
                throw new MongoInternalException( "can't instantiate a : " + c , iae );
            }
        }

        return new BasicDBObject();
    }

    DBObject dbget(){
        return (DBObject)get();
    }

    @Override
    public void reset(){
        names = new LinkedList<String>();
        super.reset();
    }

    LinkedList<String> names = new LinkedList<String>();
    final DBCollection _collection;
    final DB _db;
    static final Logger LOGGER = Logger.getLogger( "com.mongo.DECODING" );

    @Override
    public Object objectDone() {
        BSONObject o = (BSONObject)super.objectDone();
        String name = null;
        if (names.size() > 0) name = names.removeLast();
        System.out.println(name);
        if ( name != null &&
                ! ( o instanceof List ) &&
                o.containsField( "$ref" ) &&
                o.containsField( "$id" ) ){
            return cur().put( name , new DBRef( _db, o ) );
        }

        return o;

    }

}