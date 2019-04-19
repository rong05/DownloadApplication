package com.rong.download;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A cache that holds strong references to a limited number of values. Each time
 * a value is accessed, it is moved to the head of a queue. When a value is
 * added to a full cache, the value at the end of that queue is evicted and may
 * become eligible for garbage collection.
 *
 * <p>If your cached values hold resources that need to be explicitly released,
 * override {@link #entryRemoved}.
 *
 * <p>If a cache miss should be computed on demand for the corresponding keys,
 * override {@link #create}. This simplifies the calling code, allowing it to
 * assume a value will always be returned, even when there's a cache miss.
 *
 * <p>By default, the cache size is measured in the number of entries. Override
 * {@link #sizeOf} to size the cache in different units. For example, this cache
 * is limited to 4MiB of bitmaps:
 * <pre>   {@code
 *   int cacheSize = 4 * 1024 * 1024; // 4MiB
 *   LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
 *       protected int sizeOf(String key, Bitmap value) {
 *           return value.getByteCount();
 *       }
 *   }}</pre>
 *
 * <p>This class is thread-safe. Perform multiple cache operations atomically by
 * synchronizing on the cache: <pre>   {@code
 *   synchronized (cache) {
 *     if (cache.get(key) == null) {
 *         cache.put(key, value);
 *     }
 *   }}</pre>
 *
 * <p>This class does not allow null to be used as a key or value. A return
 * value of null from {@link #get}, {@link #put} or {@link #remove} is
 * unambiguous: the key was not in the cache.
 *
 * <p>This class appeared in Android 3.1 (Honeycomb MR1); it's available as part
 * of <a href="http://developer.android.com/sdk/compatibility-library.html">Android's
 * Support Package</a> for earlier releases.
 */
public class LruCache<K, V> {
    private final HashMap<K, Entry<K, V>> map;//缓存容器，map的键其实就是entry的键

    /**
     * Size of this cache in units. Not necessarily the number of elements.
     */
    private int size;//缓存中的元素长度
    private int maxSize;//缓存的长度

    private int putCount;
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;
    private Entry<K, V> head;//链表的头部
    private Entry<K, V> tail;//链表的尾部

    private final ReadWriteLock rwLock;


    private static class Entry<K, V> {
        Entry<K, V> before;//上一个节点
        Entry<K, V> after;//下一个节点
        final V value;//保存的内容
        final K key;//键

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public V getValue() {
            return value;
        }

        public K getKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry<?, ?> entry = (Entry<?, ?>) o;
            return getKey().equals(entry.getKey());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getKey());
        }
    }


    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new HashMap<>((int) Math.ceil(maxSize / 0.75) + 1, 0.75f);
        this.rwLock = new ReentrantReadWriteLock();
    }

    /**
     * Sets the size of the cache.
     *
     * @param maxSize The new maximum size.
     */
    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        rwLock.writeLock().lock();
        try {
            this.maxSize = maxSize;
        }finally {
            rwLock.writeLock().unlock();
        }
        trimToSize(maxSize);
    }

    // newNode 中新节点，放到双向链表的尾部
    private void linkNodeLast(Entry<K, V> p) {
        // 添加元素之前双向链表尾部节点
        Entry<K, V> last = tail;
        // tail 指向新添加的节点
        tail = p;
        //如果之前 tail 指向 null 那么集合为空新添加的节点 head = tail = p
        if (last == null)
            head = p;
        else {
            // 否则将新节点的 before 引用指向之前当前链表尾部
            p.before = last;
            // 当前链表尾部节点的 after 指向新节点
            last.after = p;
        }
    }

    // newNode 的实现
    private Entry<K, V> newNode(K key, V value) {
        Entry<K, V> p = new Entry<K, V>(key, value);
        // 将 Entry 接在双向链表的尾部
        linkNodeLast(p);
        return p;
    }


    //  从双向链表中删除对应的节点 e 为已经删除的节点
    private void afterNodeRemoval(Entry<K, V> e) {
        Entry<K, V> p = e, b = p.before, a = p.after;
        // 将 p 节点的前后指针引用置为 null 便于内存释放
        p.before = p.after = null;
        // p.before 为 null，表明 p 是头节点
        if (b == null)
            head = a;
        else//否则将 p 的前驱节点连接到 p 的后驱节点
            b.after = a;
        // a 为 null，表明 p 是尾节点
        if (a == null)
            tail = b;
        else //否则将 a 的前驱节点连接到 b
            a.before = b;
    }


    //将被访问节点移动到链表最后
    private void afterNodeAccess(Entry<K, V> e) { // move node to last
        Entry<K, V> last;
        if ((last = tail) != e) {
            Entry<K, V> p = e, b = p.before, a = p.after;
            //访问节点的后驱置为 null
            p.after = null;
            //如访问节点的前驱为 null 则说明 p = head
            if (b == null)
                head = a;
            else
                b.after = a;
            //如果 p 不为尾节点 那么将 a 的前驱设置为 b
            if (a != null)
                a.before = b;
            else
                last = b;

            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;// 将 p 接在双向链表的最后
        }
    }


    /**
     * Returns the value for {@code key} if it exists in the cache or can be
     * created by {@code #create}. If a value was returned, it is moved to the
     * head of the queue. This returns null if a value is not cached and cannot
     * be created.
     */
    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Entry<K, V> mapValue = null;
        rwLock.readLock().lock();
        try {
            mapValue = map.get(key);
            rwLock.readLock().unlock();
            rwLock.writeLock().lock();
            try {
                if (mapValue != null) {
                    afterNodeAccess(mapValue);
                    hitCount++;
                }
                missCount++;
            } finally {
                rwLock.readLock().lock();
                rwLock.writeLock().unlock();
            }
        } finally {
            rwLock.readLock().unlock();
            if (mapValue != null) {
                return mapValue.getValue();
            }
        }

        /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */

        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }
        rwLock.writeLock().lock();
        try {
            createCount++;
            mapValue = newNode(key, createdValue);
            Entry<K, V> previous = map.put(key, mapValue);
            if (previous != null) {
                // There was a conflict so undo that last put
                afterNodeRemoval(mapValue);
                map.put(key, previous);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue.getValue());
            return mapValue.getValue();
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }

    /**
     * Caches {@code value} for {@code key}. The value is moved to the head of
     * the queue.
     *
     * @return the previous value mapped by {@code key}.
     */
    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        Entry<K, V> previous;
        rwLock.writeLock().lock();
        try {
            putCount++;
            size += safeSizeOf(key, value);
            Entry<K, V> node = newNode(key, value);
            previous = map.put(key, node);
            if (previous != null) {
                afterNodeRemoval(previous);
                size -= safeSizeOf(key, value);
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        if (previous != null) {
            entryRemoved(false, key, previous.getValue(), value);
        }

        trimToSize(maxSize);
        return previous != null ? previous.getValue() : null;
    }



    /**
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     *                to evict even 0-sized elements.
     */
    public void trimToSize(int maxSize) {
        while (true) {
            K key;
            V value;
            rwLock.readLock().lock();
            if (size < 0 || (map.isEmpty() && size != 0)) {
                rwLock.readLock().unlock();
                throw new IllegalStateException(getClass().getName()
                        + ".sizeOf() is reporting inconsistent results!");
            }

            if (size <= maxSize) {//检查是否超过最大缓存数
                rwLock.readLock().unlock();
                break;
            }
            Entry<K, V> toEvict = head;//取出头部
            if (toEvict == null) {
                rwLock.readLock().unlock();
                break;
            }

            key = toEvict.getKey();
            value = toEvict.getValue();
            Entry<K, V> previous;
            rwLock.readLock().unlock();//读取锁不可能升级成写入锁
            rwLock.writeLock().lock();
            try {
                previous = map.remove(key);
                if (previous != null) {
                    afterNodeRemoval(previous);//删除节点
                    size -= safeSizeOf(key, value);
                    evictionCount++;
                }
            } finally {
                rwLock.readLock().lock();//写入锁可以降级为读取锁
                rwLock.writeLock().unlock();
            }
            rwLock.readLock().unlock();
            entryRemoved(true, key, value, null);
        }
    }

    /**
     * Removes the entry for {@code key} if it exists.
     *
     * @return the previous value mapped by {@code key}.
     */
    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Entry<K, V> previous;
        rwLock.writeLock().lock();
        try {
            previous = map.remove(key);
            if (previous != null) {
                afterNodeRemoval(previous);
                size -= safeSizeOf(key, previous.getValue());
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        if (previous != null) {
            entryRemoved(false, key, previous.getValue(), null);
        }

        return previous != null ? previous.getValue() : null;
    }


    public final Iterator<Entry<K,V>> iterator(){
        final Set<Entry<K,V>> vSet = new LinkedHashSet<>();
        rwLock.readLock().lock();
        try {
            Entry<K,V> e = head;
            while (e != tail.after){
                vSet.add(e);
                Entry<K,V> a = e.after;
                e = a;
            }
        }finally {
            rwLock.readLock().unlock();
        }

        return vSet.iterator();
    }
    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * {@link #remove}, or replaced by a call to {@link #put}. The default
     * implementation does nothing.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted  true if the entry is being removed to make space, false
     *                 if the removal was caused by a {@link #put} or {@link #remove}.
     * @param newValue the new value for {@code key}, if it exists. If non-null,
     *                 this removal was caused by a {@link #put}. Otherwise it was caused by
     *                 an eviction or a {@link #remove}.
     */
    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
    }

    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * <p>If a value for {@code key} exists in the cache when this method
     * returns, the created value will be released with {@link #entryRemoved}
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls {@link #put} while another is creating a value for the same
     * key.
     */
    protected V create(K key) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    /**
     * Returns the size of the entry for {@code key} and {@code value} in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     * <p>An entry's size must not change while it is in the cache.
     */
    protected int sizeOf(K key, V value) {
        return 1;
    }

    /**
     * Clear the cache, calling {@link #entryRemoved} on each removed entry.
     */
    public final void evictAll() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    public  final int size() {
        int size = -1;
        rwLock.readLock().lock();
        try {
            size = this.size;
        }finally {
            rwLock.readLock().unlock();
        }
        return size;
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    public  final int maxSize() {
        int maxSize = -1;
        rwLock.readLock().lock();
        try {
            maxSize = this.maxSize;
        }finally {
            rwLock.readLock().unlock();
        }
        return maxSize;
    }

    /**
     * Returns the number of times {@link #get} returned a value that was
     * already present in the cache.
     */
    public final int hitCount() {
        int hitCount = -1;
        rwLock.readLock().lock();
        try {
            hitCount = this.hitCount;
        }finally {
            rwLock.readLock().unlock();
        }
        return hitCount;
    }

    /**
     * Returns the number of times {@link #get} returned null or required a new
     * value to be created.
     */
    public  final int missCount() {
        int missCount = -1;
        rwLock.readLock().lock();
        try {
            missCount = this.missCount;
        }finally {
            rwLock.readLock().unlock();
        }
        return missCount;
    }

    /**
     * Returns the number of times {@link #create(Object)} returned a value.
     */
    public final int createCount() {
        int createCount = -1;
        rwLock.readLock().lock();
        try {
            createCount = this.createCount;
        }finally {
            rwLock.readLock().unlock();
        }
        return createCount;
    }

    /**
     * Returns the number of times {@link #put} was called.
     */
    public final int putCount() {
        int putCount = -1;
        rwLock.readLock().lock();
        try {
            putCount = this.putCount;
        }finally {
            rwLock.readLock().unlock();
        }
        return putCount;
    }

    /**
     * Returns the number of values that have been evicted.
     */
    public final int evictionCount() {
        int evictionCount = -1;
        rwLock.readLock().lock();
        try {
            evictionCount = this.evictionCount;
        }finally {
            rwLock.readLock().unlock();
        }
        return evictionCount;
    }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    /** public synchronized final Map<K, Entry<K, V>> snapshot() {
        return new HashMap<K, Entry<K, V>>(map);
    }*/

    @Override
    public final String toString() {
        String s = null;
        rwLock.readLock().lock();
        try {
            int accesses = hitCount + missCount;
            int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
            s = String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]",
                    maxSize, hitCount, missCount, hitPercent);
        }finally {
            rwLock.readLock().unlock();
        }
        return s;
    }

    public final String toAllValueString(){
        final StringBuffer SB = new StringBuffer();
        SB.append("[");
        Iterator<Entry<K,V>> iterator = iterator();
        while (iterator.hasNext()){
            final Entry<K,V> e = iterator.next();
            if(e != null){
                SB.append("(");
                SB.append(e.getKey().toString());
                SB.append(",");
                SB.append(e.getValue().toString());
                SB.append("),");
            }
        }
        SB.append("]");
        return SB.toString();
    }
}

