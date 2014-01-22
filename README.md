javaz
=====

Set of mini utils, classes, routines and other useful things, all buildable as JAR and available as maven artifacats.

Maven dependencies:
==================

    <dependency>
        <groupId>org.javaz</groupId>
        <artifactId>javaz-jdbc</artifactId>
        <version>1.0</version>
        <type>pom</type>
    </dependency>

Available artifactIds:
----------------------
javaz-cache, javaz-jdbc, javaz-queues, javaz-servlet, javaz-uml, javaz-util


Sub Projects:
-------------

* cache - Simple and straightforward implementation of expirable Cache.

* jdbc - Handy tool to access JDBC/JNDI databases, for replicating data between different DBs

* queues - Helpers to make some batching/queues, both abstract and implementation part

* servlet - Servlet, XMLRPC and SSL stuff

* uml - tools to convert Violet UML editor class diagrams into objects, and render objects to any script files usung freemarker.

* util - Small useful utils, not belong to any significant project

* xml - Useful things for xml

Sub Projects cross-dependencies:
--------------------------------

* jdbc/queues - Queues interface implemented using JDBC tools; Threaded generic mass-updater;

Usage examples
==============

Cache
-----
        CacheImpl cache = new CacheImpl();
        cache.setTimeToLive(3600*1000L);
        cache.put("any key, String wold be fine", new Double(7.0));
        Double seven = cache.get("any key, String wold be fine");

That's it. Cache auto cleared during active requests. If you want more aggressive purging - just call something
like this in Thread:
        cache.clearExpired();


JDBC
----
        String address = "jdbc:hsqldb:hsql://localhost:1600/mydb1;username=SA";
        JdbcHelperI db = JdbcCachedHelper.getInstance(address);

        // When you using pool, or your connections have to be created in some obscure way, you should
        // implement own ConnectionProviderFactory and use it.
        ConnectionProviderFactory ownFactory = ...
        String address2 = "custom-address";
        JdbcHelperI dbCustom = JdbcCachedHelper.getInstance(address2, ownFactory);

        // simple query execution
        db.runUpdate("create table test (id integer, name varchar(250))", null);

        // simple query execution with parameter
        HashMap map = new HashMap();

        // SQL standards - parameters starts from 1
        map.put(1, "No injections ' allowed \" at all'; &quot; --");
        db.runUpdate("insert into test (name) values (?)", map);

        // no cache allowed in this query
        List list = test.getRecordList("select * from test", null, false);

        // cache is used here.
        List list = test.getRecordList("select * from test", null);

        // run mass update in single call - in single transaction/connection
        ArrayList updates = new ArrayList();
        updates.add(new Object[]{"insert into test values (101,'a')", null});
        updates.add(new Object[]{"insert into test values (102,'b')", null});
        updates.add(new Object[]{"insert into test values (103,'c')", null});

        db.runMassUpdate(updates);

        // Asynchronous queries
        // For example, you in one thread very efficiently finding what should be updated.
        // To make updates happen in separate Thread, is such simple as:

        GenericDbUpdater dbUpdater =
            GenericDbUpdater.getInstance("update test set name='x' where id", address);
        dbUpdater.addToQueueAll(millionIdsCollection);
        // And updates will go Batched by GenericDbUpdater.MAX_OBJECTS_PER_UPDATE and in separate Thread.



        // POC util
        ReplicateTables replicator = new ReplicateTables();
        // initial values
        replicator.dbFrom = address;
        replicator.dbTo = address2;
        replicator.dbToType = "hsqldb";
        HashMap<String, String> tableInfo = new HashMap<String, String>();
        tableInfo.put("name", "test5");
        tableInfo.put("name2", "test6");
        tableInfo.put("where1", " AND id > 0 ");
        tableInfo.put("where2", " AND id > 0 ");
        replicator.tables.add(tableInfo);

        // Here will happen magic - all data from dbFrom.test5 table will go to dbTo.test6
        // From and To any JDBC-compliant Database URLs.
        // Conditions of success - meta data and sizes of columns should match (pretty obvious)
        replicator.runReplicate();


* why caching some results? There happens heavy queries with relatively slow changing data;

 When you read some rows into memory, objects will stay there until GC, right? So, storing them actually
 not bad idea, at least for some short time.

Queue
-----

        // You have very large table (~millions of records), or other datasource;
        // and you need get records from there by some condition;
        // For example to make update of expired data.
        // Synchronous select is bad, since any significant client traffic will kill your DB
        // To avoid this, there are RecordsRotaterI, which get records from underlying
        // RecordsFetcherI in separate thread, by reasonable chunks.

        SqlRecordsFetcher fetcher = new SqlRecordsFetcher(address,
                "idx, name", // which columns are returned as DATA
                "test4", // from clause, can contains tables, left joins, etc
                "idx > 0"); // condition

        //If your PK not called "id"
        fetcher.setIdColumn("idx");
        //How each records data will be returned, in Maps or ArrayList
        fetcher.setSelectType(JdbcConstants.ACTION_MAP_RESULTS_SET);

        //get and launch in background Thread rotator itself
        RecordsRotatorI rotater = RotatorsHolder.getRotater(fetcher);

        // if data is read, then here will be some results.
        // No matter how many clients called this method, load will be low
        // Since querying ready objects pool are separated from part which fills it from DB (Or any
        // other source - RPC, WS)
        Collection elements = rotater.getManyElements(1000);

        // If you implementing accepter of data, it's nice to have it asynchronous too.
        // For example, some clients are pushing to your server data, and load is different in time.
        // That's simple:

        // Create and launch Sender:
        sender = new SimplePartialSender(yourFeederDataSaver);
        //Any size
        sender.setChunkSize(25);
        //If your logic allows - wait some time to push more data
        sender.setWaitDtelayForMinimalSize(1000);

        // When data came, just puch it like this:
        sender.addToQueueAll(manyObjects);

        //That's it. When sender will be ready, it will call your implemented method in FeedI:
        // yourFeederDataSaver.sendData(Collection nextChunkOfRecords) throws Exception;

Uml
---

        // VioletParser used to parse beans from violet file or from json
        HashMap fromViolet = new VioletParser().parseVioletClass("path");

        // VioletParser used to parse beans from violet file or from json
        HashMap fromJson = new VioletParser().parseFromJson("path");

        // Can be used from commandline
        java -cp ${jar} org.javaz.uml.VioletParser in.class.violet out-ver${ver}.json

        // RenderFtl used to take beans (or each bean) and render some templates
        RenderFtl renderFtl = new RenderFtl(oldModel, newModel);
        renderFtl.setParseType(RENDER_DIFFERENCE);
        renderFtl.setTemplate("update-db");

        // This can be used from command-line
        // This takes model from file model7.json and renders template create-mysql.ftl
        java -cp ${jar} org.javaz.uml.RenderFtl model7.json create-mysql 1 -DtemplatePath=/path/tpl


Util
----
        // return time in such format YYYYDDDPP
        // where YYYY - year
        // DDD - day in year
        // PP - percents of time; e.g. 12:00 = 50; 18:00 = 75
        Integer day = DayUtil.getIntegerTime();

        // @return day from beginning of 2011 Year
        //         It's NOT the same as ((extract(year from NOW()) - 2011)*365 +
        //         extract(doy from NOW())) in database;
        // As leap years and DST counting.
        Integer daysFrom2011 = DayUtil.getDayShort();

        String toBrowser = JsonUtil.convertToJS(anyHashMapOrArrayOrAnything);

        filePropertyUtil = UpdateableFilePropertyUtil.getInstance(file);
        //get property from file;
        String original = filePropertyUtil.getProperty(key);
        // imagine that file is somehow changed;

        //get property from file;
        String updated = filePropertyUtil.getProperty(key);

        // Comparators - dynamic comparators, which can treat any Collections, elements can be any
        // level of complexity and any level of nesting

        // Create comparator which will sort Collection of Map, by getting from each map value
        // by key "a", splitting this value by "\t" and comparing second part of split, treating
        // it as Long.
        GenericDeepComparator deepComparator = new GenericDeepComparator();
        MapValueProducer producer1 = new MapValueProducer("a");
        SplitStringProducer nested1 = new SplitStringProducer("\t", 1);
        nested1.setNested(new LongFromStringProducer());
        producer1.setNested(nested1);
        deepComparator.setProducerI(producer1);

        // Create comparator which will sort Collection of Map by getting from each map value
        // by key "a"
        GenericDeepComparator deepComparator = new GenericDeepComparator();
        MapValueProducer producer1 = new MapValueProducer("a");
        deepComparator.setProducerI(producer1);

        // ObjectDifference used to find difference between objects.
        // Mostly useful to make difference between Map(s)
        HashMap inANotInB = ObjectDifference.getInANotInB(a, b);
        HashMap inAAndInBEquals = ObjectDifference.getInAAndInBEquals(a, b);
        HashMap inAAndInBNotEquals = ObjectDifference.getInAAndInBNotEquals(a, b);


XML
-----
        // If you are dealing with different XML and they are big to be processed via DOM,
        // you end up with SAX.
        // XpathSaxHandler could transform XML into set of HashMaps/ArrayLists/Strings the way you
        // specify by simple rules.
        XpathSaxHandler dh = new XpathSaxHandler();

        // Specify what objects you want to treat as highest level objects
        dh.addHashToHashFillingRule("tag1", XpathSaxHandler.RESULTS);

        // Specify other objects you need to parse, and where this objects will be put,
        // in this case all node2 objects will be put into their parent Map tag1, into
        // property "key1", tag1.get("key1") == ArrayList
        dh.addHashToHashFillingRule("node2", "tag1@key1,list");

        // node3 which should be nested into Node2 structure put in both tag1 & tag2.
        dh.addHashToHashFillingRule("node3", "tag2@n3,list");
        dh.addHashToHashFillingRule("node3", "tag1@alln3,list");

        // Okey, now let's tell parser where to create new Objects
        dh.addNewObjectRule("/start", "tag1");
        dh.addNewObjectRule("/start/some/node2", "node2");
        dh.addNewObjectRule("/start/some/node2/deeper/deeper", "node3");

        // And let's specify which attributes we actually need to be extracted:

        // You see that we extract id for tag1 not from /start/, but from any child tree attribute
        dh.addObjectFillingRule("/start/some@id", "tag1@id");

        // Extract id for node2
        dh.addObjectFillingRule("/start/some/node2@id", "node2@id");
        // Extract all requred texts and put them into ArrayList into node2
        dh.addObjectFillingRule("/start/some/node2/desc@text", "node2@alltextes,list");

        // As for node3, we interested only in one attribute.
        dh.addObjectFillingRule("/start/some/node2/deeper/deeper@refid", "node3@id");

        // And, we need extract text content from tag 'text' into ArrayList of texts.
        // Note that even empty Strings are proceeded.
        dh.addObjectFillingRule("/start/some/node2/deeper/deeper/text", "node3@texts,list");

        // Parse magic
        parser.parse(inputStream, dh);

        // And at last, get our data.
        ArrayList objects = dh.getResults();


