package YAVC.Utils;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet<K, V> {
	private final ConcurrentHashMap<K, V> map;

	public ConcurrentHashSet() {
		map = new ConcurrentHashMap<>();
	}

	public boolean add(K element, V val) {
		return map.putIfAbsent(element, val) == null;
	}

	public boolean remove(K element) {
		return map.remove(element) != null;
	}

	public boolean contains(K element) {
		return map.containsKey(element);
	}

	public int size() {
		return map.size();
	}

	public HashSet<K> convertToHashSet() {
		HashSet<K> hashSet = new HashSet<K>(this.map.keySet());
		return hashSet;
	}
}
