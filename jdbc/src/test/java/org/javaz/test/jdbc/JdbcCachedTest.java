package org.javaz.test.jdbc;

import org.hsqldb.server.Server;
import org.javaz.jdbc.queues.GenericDbUpdater;
import org.javaz.jdbc.queues.SqlRecordsFetcher;
import org.javaz.jdbc.replicate.ReplicateTables;
import org.javaz.jdbc.util.*;
import org.javaz.queues.iface.RecordsRotatorI;
import org.javaz.queues.impl.RotatorsHolder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

/**
 *
 */
public class JdbcCachedTest
{
    public static JdbcHelperI test = null;
    public static JdbcHelperI test2 = null;
    public static String address = null;
    public static String address2 = null;
    public static ConnectionProviderI provider = null;
    public static int testPort = 31234;

    @BeforeClass
    public static void testPrepare() throws Exception
    {
        new UnsafeSqlHelper();
        new JdbcCachedHelper();
        address = "jdbc:hsqldb:hsql://localhost:" + testPort + "/mydb1;username=SA";
        address2 = "jdbc:hsqldb:hsql://localhost:" + testPort + "/mydb2;username=SA";

//        address = "jdbc:hsqldb:mem:test1;username=SA";

        test = JdbcCachedHelper.getInstance(address);
        test2 = JdbcCachedHelper.getInstance(address2);
        Assert.assertEquals(address, test.getJdbcAddress());
        provider = test.getProvider();
        test.setProvider(provider);
        long ttl = test.getListRecordsTtl();
        test.setListRecordsTtl(ttl);

        Server server = new Server();
        server.setAddress("localhost");
        server.setDatabaseName(0, "mydb1");
        server.setDatabaseName(1, "mydb2");
        File tempFile1 = File.createTempFile("jdbc-junit-test1", "hsqldb");
        tempFile1.deleteOnExit();
        server.setDatabasePath(0, tempFile1.getCanonicalPath());
        File tempFile2 = File.createTempFile("jdbc-junit-test2", "hsqldb");
        tempFile2.deleteOnExit();
        server.setDatabasePath(1, tempFile2.getCanonicalPath());
        server.setPort(testPort);
        server.setTrace(true);
        server.setLogWriter(new PrintWriter(System.out));
        server.start();
        try
        {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testDumper() throws Exception
    {
        test.runUpdate("drop table test", null);

        test.runUpdate("create table test (id integer, name varchar(250))", null);
        test.runUpdate("insert into test values (1,'a'),(2,'b'),(3,'c')", null);

        String tableInserts = TableDumper.getTableInserts("test", "id", test, TableDumper.DB_SQL);
        String[] split = tableInserts.split("\n");
        Assert.assertEquals(split[1], "('1', 'a'),");

        tableInserts = TableDumper.getTableInserts("test", "name", test, TableDumper.DB_SQL);
        split = tableInserts.split("\n");
        Assert.assertEquals(split[1], "('1', 'a'),");

    }

    @Test
    public void testReplicateBlob() throws Exception
    {
        test.runUpdate("drop table blobtest", null);
        test.runUpdate("create table blobtest (id integer, bb blob)", null);
        test.runUpdate("insert into blobtest values (1, '0xAAAAFFFFFFFFFFF8787878787a77777777')", null);


        test2.runUpdate("drop table blobtest2", null);
        test2.runUpdate("create table blobtest2 (id integer, bb blob)", null);

        ReplicateTables replicator = new ReplicateTables();
        replicator.init(null);
        replicator.dbFrom = address;
        replicator.dbTo = address2;
        replicator.dbToType = "hsqldb";
        HashMap<String, String> tableInfo = new HashMap<String, String>();
        tableInfo.put("name", "blobtest");
        tableInfo.put("name2", "blobtest2");
        tableInfo.put("where1", "");
        tableInfo.put("where2", "");
        replicator.tables.add(tableInfo);

        replicator.runReplicate();


        List recordList = test.getRecordList("select * from  blobtest", null, false);
        Assert.assertEquals(recordList.size(), 1);
        Object bb = ((Map) recordList.iterator().next()).get("bb");

        recordList = test2.getRecordList("select * from  blobtest2", null, false);
        Assert.assertEquals(recordList.size(), 1);
        Object bb2 = ((Map) recordList.iterator().next()).get("bb");

        Assert.assertArrayEquals((byte[]) bb, (byte[]) bb2);
        test.runUpdate("drop table blobtest", null);
        test2.runUpdate("drop table blobtest2", null);
    }

    @Test
    public void testCache() throws Exception
    {
        test.runUpdate("drop table test", null);

        test.runUpdate("create table test (id integer, name varchar(250))", null);
        test.runUpdate("insert into test values (1,'a'),(2,'b'),(3,'c')", null);

        HashMap params = new HashMap();
        UnsafeSqlHelper.addArrayParameters(params, new Object[]{1, 2});
        ArrayList id3 = new ArrayList();
        id3.add(3);
        UnsafeSqlHelper.addArrayParameters(params, id3);

        List list =
                test.getRecordList("select * from test where id in (" + UnsafeSqlHelper.repeatQuestionMark(params.size()) + ")", params);
        Assert.assertEquals(list.size(), 3);

        ArrayList updates = new ArrayList();
        updates.add(new Object[]{"insert into test values (101,'a')", null});
        updates.add(new Object[]{"insert into test values (102,'b')", null});
        updates.add(new Object[]{"insert into test values (103,'c')", null});

        test.runMassUpdate(updates);

        list = test.getRecordList("select * from test", null, false);
        Assert.assertEquals(list.size(), 6);

        test.runUpdate("drop table test", null);

        list = test.getRecordList("select * from test", null, false);
        Assert.assertEquals(list.size(), 0);

        List list2 =
                test.getRecordList("select * from test where id in (" + UnsafeSqlHelper.repeatQuestionMark(params.size()) + ")", params);
        Assert.assertEquals(list2.size(), 3);
    }

    @Test
    public void testMassUpdate() throws Exception
    {
        test.runUpdate("drop table test2", null);

        test.runUpdate("create table test2 (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, dt timestamp)", null);
        long millis = System.currentTimeMillis();
        HashMap params = new HashMap();
        params.clear();
        params.put(1, new Date(millis));

        ArrayList updates = new ArrayList();
        updates.clear();
        updates.add(new Object[]{"insert into test2 (dt) values (?)", params});
        updates.add(new Object[]{"insert into test2 (dt) values (?)", params});
        updates.add(new Object[]{"insert into test2 (dt) values (?)", params});

        ArrayList<List> massUpdate = test.runMassUpdate(updates);
        Assert.assertEquals(massUpdate.size(), 3);
        Assert.assertEquals(((List) massUpdate.get(0)).size(), 2);

        ArrayList metadata = UnsafeSqlHelper.runSqlUnsafe(provider, address,
                "select id from test2", JdbcConstants.ACTION_COMPLEX_LIST_METADATA, null);
        Assert.assertEquals(metadata.size(), 4);
        Assert.assertTrue(((List) metadata.get(0)).get(0).getClass().getName().contains("String"));

        ArrayList nometadata = UnsafeSqlHelper.runSqlUnsafe(provider, address,
                "select id from test2", JdbcConstants.ACTION_COMPLEX_LIST_NO_METADATA, null);
        Assert.assertEquals(nometadata.size(), 3);
        Assert.assertTrue(((List) nometadata.get(0)).get(0) instanceof Number);

        ArrayList ids = UnsafeSqlHelper.runSqlUnsafe(provider, address,
                "select id from test2", JdbcConstants.ACTION_LIST_FIRST_OBJECTS, null);

        Assert.assertTrue(ids.get(0) instanceof Number);
        test.runUpdate("drop table test2", null);
    }

    @Test
    public void testGenericUpdater() throws Exception
    {
        test.runUpdate("drop table test3", null);

        test.runUpdate("create table test3 (id integer, name varchar(250))", null);
        test.runUpdate("insert into test3 values (1,'a'),(2,'b'),(3,'c')", null);
        String query = "update test3 set name='x' where id";
        GenericDbUpdater dbUpdater = GenericDbUpdater.getInstance(query, address);
        Assert.assertEquals(dbUpdater.getQuery(), query);
        Assert.assertEquals(dbUpdater.getDb(), address);
        dbUpdater.addToQueue(1);
        Thread.sleep(GenericDbUpdater.LONG_SEND_PERIOD);

        List list = test.getRecordList("select * from test3 where name='x'", null, false);
        Assert.assertEquals(list.size(), 1);

        ArrayList collection = new ArrayList();
        collection.add(2);
        collection.add(3);

        dbUpdater.addToQueueAll(collection);
        int tries = 3;

        list = test.getRecordList("select * from test3 where name='x'", null, false);
        while (tries-- > 0 && list.size() != 3)
        {
            Thread.sleep(GenericDbUpdater.LONG_SEND_PERIOD);
            list = test.getRecordList("select * from test3 where name='x'", null, false);
        }
        Assert.assertEquals(list.size(), 3);
        test.runUpdate("drop table test3", null);
    }

    @Test
    public void testSqlFetcher() throws Exception
    {
        test.runUpdate("drop table test4", null);

        test.runUpdate("create table test4 (idx integer, name varchar(250))", null);
        test.runUpdate("insert into test4 values (1,'a'),(2,'b'),(300000,'c')", null);

        SqlRecordsFetcher nullFetcher = new SqlRecordsFetcher(null,
                "idx, name", "test4", "idx > 0");
        RecordsRotatorI rotaterNull = RotatorsHolder.getRotater(nullFetcher);

        SqlRecordsFetcher recordsFetcher = new SqlRecordsFetcher(address,
                "idx, name", "test4", "idx > 0");
        recordsFetcher.setIdColumn("idx");
        recordsFetcher.setSelectType(JdbcConstants.ACTION_MAP_RESULTS_SET);
        System.out.println("recordsFetcher.getDescriptiveName() = " + recordsFetcher.getDescriptiveName());
        recordsFetcher.setFieldsClause(recordsFetcher.getFieldsClause());
        recordsFetcher.setWhereClause(recordsFetcher.getWhereClause());
        recordsFetcher.setProviderI(recordsFetcher.getProviderI());
        RecordsRotatorI rotater = RotatorsHolder.getRotater(recordsFetcher);

        SqlRecordsFetcher recordsFetcher2 = new SqlRecordsFetcher(address,
                "idx, name", "test4", "idx > 0");
        Assert.assertEquals(recordsFetcher2.getIdColumn(), "id");
        Assert.assertEquals(recordsFetcher2.getSelectType(), JdbcConstants.ACTION_MAP_RESULTS_SET);

        recordsFetcher2.setIdColumn("idx");
        Assert.assertTrue(recordsFetcher.equals(recordsFetcher2));
        recordsFetcher2.setSelectType(JdbcConstants.ACTION_COMPLEX_LIST_NO_METADATA);
        Assert.assertFalse(recordsFetcher.equals(recordsFetcher2));
        RecordsRotatorI rotater2 = RotatorsHolder.getRotater(recordsFetcher2);

        int totalSteps = 100;
        int steps = totalSteps;
        while (rotater.getCurrentQueueSize() < 3 && steps-- > 0)
        {
            Thread.sleep(rotater.getFetchDelay() / totalSteps);
        }
        //after this sleeps, rotaters MUST fetch all data, since the reported count less than minSize

        Collection elements = rotater.getManyElements(100);
        Assert.assertEquals(elements.size(), 3);

        Collection elements2 = rotater2.getManyElements(100);
        Assert.assertEquals(elements2.size(), 3);
        Assert.assertTrue(elements.iterator().next() instanceof Map);
        Assert.assertTrue(elements2.iterator().next() instanceof List);

        test.runUpdate("drop table test4", null);
    }

    @Test
    public void testReplicator() throws Exception
    {
        test.runUpdate("drop table test5", null);
        test2.runUpdate("drop table test6", null);

        test.runUpdate("create table test5 (id integer, name varchar(250), dt timestamp)", null);
        test2.runUpdate("create table test6 (id integer, name varchar(250), dt timestamp)", null);

        test.runUpdate("drop table test8", null);
        test2.runUpdate("drop table test8", null);

        test.runUpdate("create table test8 (id integer, name varchar(250), dt timestamp)", null);
        test2.runUpdate("create table test8 (id integer, name varchar(250), dt varchar(45), moreone integer)", null);

        test.runUpdate("insert into test5 values (0,'X', NULL), (1, 'a', '2013-01-01 00:06:00.101'), (2, NULL, '2011-01-01 00:06:00.103'),(300000,'c', '2011-01-01 00:06:00')", null);
        test2.runUpdate("insert into test6 values (0,'Y', NULL), (1, 'not A', '2013-01-01 00:06:00.103'), (2, NULL, '2013-01-01 00:06:00.103'),(40,'b', '2013-01-01 00:06:00')", null);

        List list = test.getRecordList("select * from test5 order by id", null, false);
        Assert.assertEquals(list.size(), 4);

        List list2 = test2.getRecordList("select * from test6", null, false);
        Assert.assertEquals(list2.size(), 4);

        ReplicateTables replicator = new ReplicateTables();
        replicator.init(null);
        replicator.dbFrom = address;
        replicator.dbTo = address2;
        replicator.dbToType = "hsqldb";
        HashMap<String, String> tableInfo = new HashMap<String, String>();
        tableInfo.put("name", "test5");
        tableInfo.put("name2", "test6");
        tableInfo.put("where1", " AND id > 0 ");
        tableInfo.put("where2", " AND id > 0 ");

        replicator.tables.add(tableInfo);

        replicator.runReplicate();

        list2 = test2.getRecordList("select * from test6 order by id", null, false);
        Assert.assertEquals(list2.size(), 4);

        for (int i = 0; i < list.size(); i++)
        {
            Map m1 = (Map) list.get(i);
            Map m2 = (Map) list2.get(i);
            Object id = m1.get("ID");
            if(id == null)
            {
                id = m1.get("id");
            }
            if (id.equals(0))
            {
                Assert.assertNotSame(m1, m2);
            }
            else
            {
                Assert.assertEquals(m1, m2);
            }
        }

        test2.runUpdate("delete from test6", null);
        replicator.runReplicate();

        list2 = test2.getRecordList("select * from test6 order by id", null, false);
        Assert.assertEquals(list2.size(), 3);

        test.runUpdate("drop table test5", null);
        test2.runUpdate("drop table test6", null);


        tableInfo = new HashMap<String, String>();
        tableInfo.put("name", "test8");
        tableInfo.put("name2", "test8");
        tableInfo.put("where1", "");
        tableInfo.put("where2", "");

        replicator.tables.clear();
        replicator.tables.add(tableInfo);

        replicator.clearLog();
        replicator.runReplicate();
        String log = replicator.getLog();

        Assert.assertTrue(log.contains("ERROR: Meta data"));

        test.runUpdate("drop table test8", null);
        test2.runUpdate("drop table test8", null);
    }
}
