package org.javaz.util.test;

import junit.framework.Assert;
import org.javaz.util.BinarySearch;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

/**
 */
public class BinarySearchTest {

    @Ignore
    @Test
    public void testComplexSearch() {
        int total = 20;
        ArrayList objs = new ArrayList();
        ArrayList objs2 = new ArrayList();
        for(int i =0 ; i < total; i++) {
            Object[] objects = genDataLong(8, 3, 100000, 1, 15000);
//            Object[] objects = genDataInt(8, 4, 125000, 3, 5000);
            objs.add(objects);
            objs2.add(convertToHashSets(objects));
        }
        //warm-up
        for(int x =0; x < 10; x++) {
            for (Iterator iterator = objs.iterator(); iterator.hasNext(); ) {
                Object[] objects = (Object[]) iterator.next();
                List result = BinarySearch.complexMultiSearch((long[][][]) objects[0], (long[][]) objects[1]);
            }
            for (Iterator iterator = objs2.iterator(); iterator.hasNext(); ) {
                Object[] objects = (Object[]) iterator.next();
                List result = simpleHashSetChecks((HashSet[][]) objects[0], (HashSet) objects[1]);
            }
        }

        int totalFound = 0;
        long nanoTime = System.nanoTime();
        for (Iterator iterator = objs.iterator(); iterator.hasNext(); ) {
            Object[] objects = (Object[]) iterator.next();
            List result = BinarySearch.complexMultiSearch((long[][][]) objects[0], (long[][]) objects[1]);
            totalFound += result.size();
        }
        long nanoTime2 = System.nanoTime();
        System.out.println("totalFound = " + totalFound);
        System.out.println("per 1 ms = " + (nanoTime2 - nanoTime)/1000000.0/total);
        System.out.println("total spent ms = " + (nanoTime2 - nanoTime)/1000000.0);

        totalFound = 0;
        nanoTime = System.nanoTime();
        for (Iterator iterator = objs2.iterator(); iterator.hasNext(); ) {
            Object[] objects = (Object[]) iterator.next();
            List result = simpleHashSetChecks((HashSet[][]) objects[0], (HashSet) objects[1]);
            totalFound += result.size();
        }
        nanoTime2 = System.nanoTime();
        System.out.println("totalFound = " + totalFound);
        System.out.println("per 1 ms = " + (nanoTime2 - nanoTime)/1000000.0/total);
        System.out.println("total spent ms = " + (nanoTime2 - nanoTime)/1000000.0);
    }

    private List simpleHashSetChecks(HashSet[][] data, HashSet negative) {
        List<Long> retValue = new LinkedList<Long>();
        HashSet first = data[0][0];
        for (Iterator iterator = first.iterator(); iterator.hasNext(); ) {
            Long value = (Long) iterator.next();
            if(negative.contains(value)) {
                continue;
            }

            boolean anyFailed = false;
            for (int i = 1; !anyFailed && i < data.length; i++) {
                HashSet[] hashSets = data[i];
                boolean anyFound = false;
                for (int j = 0; !anyFound && j < hashSets.length; j++) {
                    anyFound = hashSets[j].contains(value);
                }
                if(!anyFound) {
                    anyFailed = true;
                }
            }
            if(!anyFailed) {
                retValue.add(value);
            }
        }
        return retValue;
    }

    private Object convertToHashSets(Object[] objects) {
        long[][][] data = (long[][][]) objects[0];
        long[][] negative = (long[][]) objects[1];
        HashSet[][] sets = new HashSet[data.length][];
        HashSet negset = new HashSet();
        for(int i = 0; i < data.length; i++) {
            sets[i] = new HashSet[data[i].length];
            for(int j = 0; j < data[i].length; j++) {
                sets[i][j] = new HashSet();
                for(int k = 0; k < data[i][j].length; k++) {
                    sets[i][j].add(data[i][j][k]);
                }
            }
        }

        for(int i = 0; i < negative.length; i++) {
            for(int j = 0; j < negative[i].length; j++) {
                negset.add(negative[i][j]);
            }
        }
        return new Object[] {sets, negset} ;
    }

    private Object[] genDataInt(int dims, int arrs, int items, int negs, int negsCount) {
        int from = 1;
        int to =   3*items;
        Random random = new Random();
        int[][][] data = new int[dims][][];
        for(int i = 0; i < data.length; i++) {
            int size = (i ==0 ? 1 : arrs);
            data[i] = new int[size][];
            for(int j = 0; j < data[i].length; j++) {

                HashSet set = new HashSet();
                for(int k = 0; k < items; k++) {
                    set.add(random.nextInt(to) + from);
                }
                data[i][j] = new int[set.size()];
                ArrayList list = new ArrayList(set);
                Collections.sort(list);
                for (int k = 0; k < data[i][j].length; k++) {
                    data[i][j][k] = (Integer) list.get(k);
                }
            }
        }
        int[][] negative = new int[negs][];
        for(int i = 0; i < negs; i++) {

            HashSet set = new HashSet();
            for(int k = 0; k < items; k++) {
                set.add(random.nextInt(to) + from);
            }
            negative[i] = new int[set.size()];
            ArrayList list = new ArrayList(set);
            Collections.sort(list);
            for (int k = 0; k < negative[i].length; k++) {
                negative[i][k] = (Integer) list.get(k);
            }
        }

        return new Object[]{data, negative};
    }

    private Object[] genDataLong(int dims, int arrs, int items, int negs, int negsCount) {
        int from = 1;
        int to =   3*items;
        Random random = new Random();
        long[][][] data = new long[dims][][];
        for(int i = 0; i < data.length; i++) {
            int size = (i ==0 ? 1 : arrs);
            data[i] = new long[size][];
            for(int j = 0; j < data[i].length; j++) {

                HashSet set = new HashSet();
                for(int k = 0; k < items; k++) {
                    set.add(random.nextInt(to) + from);
                }
                data[i][j] = new long[set.size()];
                ArrayList list = new ArrayList(set);
                Collections.sort(list);
                for (int k = 0; k < data[i][j].length; k++) {
                    data[i][j][k] = ((Integer) list.get(k)).longValue();
                }
            }
        }
        long[][] negative = new long[negs][];
        for(int i = 0; i < negs; i++) {

            HashSet set = new HashSet();
            for(int k = 0; k < items; k++) {
                set.add(random.nextInt(to) + from);
            }
            negative[i] = new long[set.size()];
            ArrayList list = new ArrayList(set);
            Collections.sort(list);
            for (int k = 0; k < negative[i].length; k++) {
                negative[i][k] = ((Integer) list.get(k)).longValue();
            }
        }

        return new Object[]{data, negative};
    }

    @Test
    public void testInclusiveSearch() {
        testIncl(new int[]{1, 3, 5, 7, 9, 11}, 3, 5, 1, 2);
        testIncl(new int[]{1, 3, 5, 7, 9, 11}, 3, 3, 1, 1);
        testIncl(new int[]{1, 3, 5, 7, 9, 11}, 2, 2, -1, -1);

        testExcl(new int[]{1, 3, 5, 7, 9, 11}, 3, 5, -1, -1);
        testExcl(new int[]{1, 3, 5, 7, 9, 11}, 3, 7, 2, 2);
        testExcl(new int[]{1, 3, 5, 7, 9, 11}, 2, 4, 1, 1);
    }

    private void testIncl(int[] ints, int from, int to, int idx, int idx2) {
        int[] range = BinarySearch.binaryRangeSearchInclusive(ints, from, to);
        Assert.assertEquals(range[0], idx);
        Assert.assertEquals(range[1], idx2);
    }

    private void testExcl(int[] ints, int from, int to, int idx, int idx2) {
        int[] range = BinarySearch.binaryRangeSearchExclusive(ints, from, to);
        Assert.assertEquals(range[0], idx);
        Assert.assertEquals(range[1], idx2);
    }
}
