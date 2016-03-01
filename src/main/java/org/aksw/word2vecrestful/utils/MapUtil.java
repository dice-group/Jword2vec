package org.aksw.word2vecrestful.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapUtil {
  public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
    return _sortByValue(map, false);
  }

  public static <K, V extends Comparable<? super V>> Map<K, V> reverseSortByValue(
      final Map<K, V> map) {
    return _sortByValue(map, true);
  }

  private static <K, V extends Comparable<? super V>> Map<K, V> _sortByValue(final Map<K, V> map,
      final boolean reverse) {
    final List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
    Collections.sort(list, (o1, o2) -> {
      if (reverse) {
        return (o2.getValue()).compareTo(o1.getValue());
      } else {
        return (o1.getValue()).compareTo(o2.getValue());
      }
    });
    final Map<K, V> result = new LinkedHashMap<K, V>();
    list.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return result;
  }
}
