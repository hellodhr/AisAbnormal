/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dma.ais.abnormal.stat.db.data;

import com.google.common.primitives.Ints;
import dk.dma.ais.abnormal.util.Categorizer;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.map.hash.TShortIntHashMap;

import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * This is a memory-consumption optimised implementation of FourKeyMap intended to store
 * AIS feature statistics of type CourseOverGroundData for one grid cell.
 *
 */
public class CourseOverGroundData implements FeatureData, FourKeyMap {

    private final TShortIntHashMap data;

    final byte MAX_KEY_1;
    final byte MAX_KEY_2;
    final byte MAX_KEY_3;

    private static final String MEANING_OF_KEY_1 = "shipType";
    private static final String MEANING_OF_KEY_2 = "shipSize";
    private static final String MEANING_OF_KEY_3 = "cogInterval";
    private static final String MEANING_OF_KEY_4 = "statName";

    public static final String STAT_SHIP_COUNT = "shipCount";

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey1() {
        return MEANING_OF_KEY_1;
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey2() {
        return MEANING_OF_KEY_2;
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey3() {
        return MEANING_OF_KEY_3;
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey4() {
        return MEANING_OF_KEY_4;
    }

    public static CourseOverGroundData create() {
        return new CourseOverGroundData(Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1, Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1, Categorizer.NUM_COURSE_OVER_GROUND_CATEGORIES - 1, 1);
    }

    protected CourseOverGroundData(int maxKey1, int maxKey2, int maxKey3, int maxNumKey4) {
        if (maxKey1 <= 0) {
            throw new IllegalArgumentException("maxKey1 <= 0 not supported.");
        }
        if (maxKey2 <= 0) {
            throw new IllegalArgumentException("maxKey2 <= 0 not supported.");
        }
        if (maxKey3 <= 0) {
            throw new IllegalArgumentException("maxKey3 <= 0 not supported.");
        }
        if (maxNumKey4 != 1) {
            throw new IllegalArgumentException("maxNumKey4 != 1 not supported.");
        }

        this.MAX_KEY_1 = (byte) maxKey1;
        this.MAX_KEY_2 = (byte) maxKey2;
        this.MAX_KEY_3 = (byte) maxKey3;

        this.data = new TShortIntHashMap(1);
    }

    @Override
    public void incrementValue(int shipTypeBucket, int shipSizeBucket, int cogBucket, String statisticName) {
        short key = computeMapKey(shipTypeBucket, shipSizeBucket, cogBucket, statisticName);
        if (data.get(key) != data.getNoEntryValue()) {
            data.increment(key);
        } else {
            data.put(key, 1);
            data.compact();
        }
    }

    @Override
    public void setValue(int shipTypeBucket, int shipSizeBucket, int cogBucket, String statisticName, int value) {
        short key = computeMapKey(shipTypeBucket, shipSizeBucket, cogBucket, statisticName);
        data.put(key, value);
        data.compact();
    }

    @Override
    public Integer getValue(int shipTypeBucket, int shipSizeBucket, int cogBucket, String statisticName) {
        short key = computeMapKey(shipTypeBucket, shipSizeBucket, cogBucket, statisticName);
        Integer statisticsValue = data.get(key);
        statisticsValue = statisticsValue == data.getNoEntryValue() ? null : statisticsValue;
        return statisticsValue;
    }

    @Override
    public int getSumFor(String key4) {
        return Ints.asList(data.values()).stream().mapToInt(value -> value).sum();
    }

    @Override
    public String getFeatureName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getFeatureDataType() {
        return FourKeyMap.class.getSimpleName();
    }

    @Override
    public TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> getData() {
        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> root = new TreeMap<>();

        TShortIterator keys = data.keySet().iterator();
        while(keys.hasNext()) {
            short key = keys.next();
            int shipTypeBucket = extractKey1(key);
            int shipSizeBucket = extractKey2(key);
            int cogBucket      = extractKey3(key);

            TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>> level1 = root.get(shipTypeBucket);
            if (level1 == null) {
                level1 = new TreeMap<>();
                root.put(shipTypeBucket, level1);
            }

            TreeMap<Integer, HashMap<String, Integer>> level2 = level1.get(shipSizeBucket);
            if (level2 == null) {
                level2 = new TreeMap<>();
                level1.put(shipSizeBucket, level2);
            }

            HashMap<String, Integer> level3 = level2.get(cogBucket);
            if (level3 == null) {
                level3 = new HashMap<>();
                level2.put(cogBucket, level3);
            }

            Integer value = getValue(shipTypeBucket, shipSizeBucket, cogBucket, STAT_SHIP_COUNT);
            if (value != null) {
                level3.put(STAT_SHIP_COUNT, value);
            }
        }

        return root;
    }

    short computeMapKey(int key1, int key2, int key3, String key4) {
        if (key1 > MAX_KEY_1) {
            throw new IllegalArgumentException("key1 must be 0-" + MAX_KEY_1 + ".");
        }
        if (key2 > MAX_KEY_2) {
            throw new IllegalArgumentException("key2 must be 0-" + MAX_KEY_2 + ".");
        }
        if (key3 > MAX_KEY_3) {
            throw new IllegalArgumentException("key3 must be 0-" + MAX_KEY_3 + ".");
        }
        if (! STAT_SHIP_COUNT.equals(key4)) {
            throw new IllegalArgumentException("key4 '" + key4 + "' is not supported.");
        }

        final int d1 = MAX_KEY_2 + 1;
        final int d2 = MAX_KEY_3 + 1;

        return (short) (key3 + key2*d2 + key1*d1*d2);
    }

    int extractKey1(short key) {
        return (key / ((MAX_KEY_2+1)*(MAX_KEY_3+1)));
    }

    int extractKey2(short key) {
        return (key / (MAX_KEY_3+1)) % (MAX_KEY_2+1);
    }

    int extractKey3(short key) {
        return key % (MAX_KEY_3+1);
    }

    int extractKey4(short key) {
        return 0;
    }

}
