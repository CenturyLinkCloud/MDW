package com.centurylink.mdw.common.service;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * Standard parameters:
 * <ul>
 *   <li>count - Count only (no data retrieval)</li>
 *   <li>find - Find string</li>
 *   <li>start - Start index (for pagination)</li>
 *   <li>max - Max rows (for pagination)</li>
 *   <li>sort - sort field</li>
 *   <li>descending - sort descending</li>
 * </ul>
 * @author donaldoakes
 *
 */
public class Query {
    public static final int DEFAULT_MAX = 50;
    public static final int MAX_ALL = -1;

    public Query() {
    }

    public Query(String path) {
        this.path = path;
    }

    public Query(String path, Map<String,String> parameters) {

        this.path = path;

        String countParam = parameters.get("count");
        if (countParam != null)
            setCount(Boolean.parseBoolean(countParam));

        setFind(parameters.get("find"));

        String startParam = parameters.get("start");
        if (startParam != null)
            setStart(Integer.parseInt(startParam));

        String maxParam = parameters.get("max");
        if (maxParam != null)
            setMax(Integer.parseInt(maxParam));

        setSort(parameters.get("sort"));

        String descendingParam = parameters.get("descending");
        if (descendingParam != null)
            setDescending(Boolean.parseBoolean(descendingParam));

        for (String key : parameters.keySet()) {
            if (!"count".equals(key) && !"find".equals(key) && !"start".equals(key) && !"max".equals(key)
                    && !"sort".equals(key) && !"descending".equals(key) && !"ascending".equals(key) && !"app".equals(key)
                    && !"DownloadFormat".equals(key))
                setFilter(key, parameters.get(key));
        }
    }

    private boolean count; // count only -- no retrieval
    public boolean isCount() { return count; }
    public void setCount(boolean count) { this.count = true; }

    private String find;
    public String getFind() { return find; }
    public void setFind(String find) { this.find = find;}

    private int start = 0;
    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    private int max = DEFAULT_MAX;
    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }

    private String sort;
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    private boolean descending;
    public boolean isDescending() { return descending; }
    public void setDescending(boolean descending) { this.descending = descending; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    /**
     * All parameters are filters except count, find, start, max, sort, descending, ascending and app
     */
    private Map<String,String> filters = new HashMap<>();
    public Map<String,String> getFilters() { return filters; }
    public void setFilters(Map<String,String> filters) { this.filters = filters; }
    public boolean hasFilters() { return !filters.isEmpty(); }

    public Query setFilter(String key, String value) {
        filters.put(key, value);
        return this;
    }
    public String getFilter(String key) {
        return filters.get(key);
    }
    public String getFilter(String key, String defaultValue) {
        String v = getFilter(key);
        return v == null ? defaultValue : v;
    }

    /**
     * Empty list returns null;
     */
    public String[] getArrayFilter(String key) {
        String value = filters.get(key);
        if (value == null)
            return null;
        String[] array = new String[0];
        if (value.startsWith("[")) {
            if (value.length() > 2)
                array = value.substring(1, value.length() - 1).split(",");
        }
        else if (value.length() > 1) {
            array = value.split(",");
        }
        for (int i = 0; i < array.length; i++) {
            String item = array[i];
            if ((item.startsWith("\"") && item.endsWith("\"")) || (item.startsWith("'") && item.endsWith("'")) && item.length() > 1)
                array[i] = item.substring(1, item.length() - 2);
        }
        return array;
    }
    public Query setArrayFilter(String key, String[] array) {
        if (array == null || array.length == 0) {
            filters.remove(key);
        }
        else {
            String value = "[";
            for (int i = 0; i < array.length; i++) {
                value += array[i];
                if (i < array.length - 1)
                    value += ",";
            }
            value += "]";
            filters.put(key, value);
        }
        return this;
    }

    /**
     * Empty map returns null, as does invalid format.
     */
    public Map<String,String> getMapFilter(String name) {
        String value = filters.get(name);
        if (value == null)
            return null;
        Map<String,String> map = new LinkedHashMap<>();
        if (value.startsWith("{") && value.endsWith("}")) {
            for (String entry : value.substring(1, value.length() - 1).split(",")) {
                int eq = entry.indexOf('=');
                if (eq > 0 && eq < entry.length() - 1) {
                    String key = entry.substring(0, eq).trim();
                    String val = entry.substring(eq + 1);
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'")) && val.length() > 1) {
                        val = val.substring(1, val.length() - 1);
                    }
                    else {
                        val = val.trim();
                    }
                    map.put(key, val);
                }
            }
        }
        return map.isEmpty() ? null : map;
    }

    public Long[] getLongArrayFilter(String key) {
        String[] strArr = getArrayFilter(key);
        if (strArr == null)
            return null;
        Long[] longArr = new Long[strArr.length];
        for (int i = 0; i < strArr.length; i++)
            longArr[i] = Long.parseLong(strArr[i]);
        return longArr;
    }

    public boolean getBooleanFilter(String key) {
        String value = filters.get(key);
        return "true".equalsIgnoreCase(value);
    }
    public Query setFilter(String key, boolean value) {
        setFilter(key, String.valueOf(value));
        return this;
    }

    public int getIntFilter(String key) {
        String value = filters.get(key);
        return value == null ? -1 : Integer.parseInt(value);
    }
    public Query setFilter(String key, int value) {
        setFilter(key, String.valueOf(value));
        return this;
    }

    public long getLongFilter(String key) {
        String value = filters.get(key);
        return value == null ? -1 : Long.parseLong(value);
    }
    public Query setFilter(String key, long value) {
        setFilter(key, String.valueOf(value));
        return this;
    }

    public Date getDateFilter(String key) throws ParseException {
        return getDate(filters.get(key));
    }

    public Instant getInstantFilter(String key) {
        String value = filters.get(key);
        return value == null ? null : Instant.parse(value);
    }

    public enum Timespan {
        Day ((long)24*3600*1000),
        Week (Day.millis * 7),
        Month (Day.millis * 30);

        private final long millis;
        public long millis() { return millis; }

        Timespan(long millis) {
            this.millis = millis;
        }
    }

    public Timespan getTimespanFilter(String key) {
        String span = getFilter(key);
        if (span == null) {
            return Timespan.Week;
        }
        else {
            try {
                return Timespan.valueOf(span);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    public Query setFilter(String key, Date date) {
        String value = getString(date);
        if (value == null)
            filters.remove(key);
        else
            filters.put(key, value);
        return this;
    }

    protected static DateFormat getDateTimeFormat() {
        return new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    }

    protected static DateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy-MMM-dd"); // does not require url-encoding
    }

    public static Date getDate(String str) throws ParseException {
        if (str == null)
            return null;
        else if (str.length() > 11)
            return getDateTimeFormat().parse(str);
        else
            return getDateFormat().parse(str);
    }

    public static String getString(Date date) {
        if (date == null)
            return null;
        else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0 && c.get(Calendar.SECOND) == 0)
                return getDateFormat().format(date);
            else
                return getDateTimeFormat().format(date);
        }
    }

    public static String getString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /**
     * Builds a path/query string for an endpoint.
     * Don't change this without extensive regression testing.
     */
    @SuppressWarnings("deprecation")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (filters != null) {
            for (String name : filters.keySet()) {
                if (sb.length() > 0)
                    sb.append("&");
                sb.append(name).append("=").append(URLEncoder.encode(filters.get(name)));
            }
            if (sb.length() > 0)
                sb.append("&");
        }
        if (count) {
            sb.append("count=true");
            if (sb.length() > 0)
                sb.append("&");
        }
        if (find != null) {
            sb.append("find=" + find);
            if (sb.length() > 0)
                sb.append("&");
        }
        if (start > 0) {
            sb.append("start=" + start);
            if (sb.length() > 0)
                sb.append("&");
        }
        if (max != DEFAULT_MAX) {
            sb.append("max=" + max);
            if (sb.length() > 0)
                sb.append("&");
        }
        if (sort != null) {
            sb.append("sort=" + sort);
            if (sb.length() > 0)
                sb.append("&");
        }
        if (descending) {
            sb.append("descending=true");
            if (sb.length() > 0)
                sb.append("&");
        }

        String query = sb.toString();
        if (query.endsWith("&"))
            query = query.substring(0, query.length() - 1);

        if (path != null)
            return query.length() > 0 ? path + "?" + query : path;
        else
            return query;
    }
}