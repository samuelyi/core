package com.dotcms.concurrent.lock;

import com.dotcms.concurrent.DotConcurrentException;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Strip lock Implementation - backed-up by ConcurrentHashMap
 */
public class StripedLockImpl<K> implements DotKeyLockManager<K> {

    static final int DEFAULT_STRIPES = 100;

    static final int DEFAULT_TIME = 1;

    static final TimeUnit DEFAULT_TU = TimeUnit.MINUTES;

    private final TimeUnit unit;

    private final long time;

    //This is a collection like struct that holds references of the Lock implementation of choice.
    //It'll allocate locks based upon a key.
    private final ConcurrentMap<K,ReentrantLock> locks = new ConcurrentHashMap<>(100);

    /**
     * Constructor 2 takes the number of strips to be allocated + a time specification to instruct
     * the try-lock methods This will instantiate the Collection of Stripes establishing an initial
     * size. The collection will hold null references until a lock instance is required. The
     * References are WeakReferences that will be collected once the entry is no longer in use.
     */
    StripedLockImpl(final int stripes, final long time, final TimeUnit unit) {
        this.time = time;
        this.unit = unit;
    }

    /**
     * @inheritDoc
     */
    @Override
    public <R> R tryLock(final K key, final ReturnableDelegate<R> callback) throws Throwable {
        return this.tryLock(key, callback, this.time, this.unit);
    }


    /**
     * @inheritDoc
     */
    @Override
    public void tryLock(final K key, final VoidDelegate callback) throws Throwable {
        tryLock(key, callback, this.time, this.unit);
    }

    /**
     * Internal try lock impl. With Returnable Delegate
     * These methods are hidden from the public interface
     * @param key
     * @param callback
     * @param time
     * @param unit
     * @param <R>
     * @return
     * @throws Throwable
     */
    <R> R tryLock(final K key, final ReturnableDelegate<R> callback, final long time,
            final TimeUnit unit) throws Throwable {

        if(isLockAlreadyHeld(Thread.currentThread())){
            Logger.info(StripedLockImpl.class,
                    " Only one lock can be held by thread  "+ Thread.currentThread() );
            return callback.execute();
        }

        final ReentrantLock lock = locks.computeIfAbsent(key, newLock -> new ReentrantLock());

        if (lock.isHeldByCurrentThread()) {
            Logger.info(StripedLockImpl.class, "Already held by current thread we can still proceed.");
            return callback.execute();
        } else {
             if (!lock.tryLock(time, unit)) {

                 final Thread owner = getOwnerThread(lock);
                 Logger.info(StripedLockImpl.class, " already locked we might get an exception! " + key + " owner= " + owner + " state= "+owner.getState() );
                 Logger.info(StripedLockImpl.class,("Owner Thread stacktrace: " + ExceptionUtil
                         .getStackTraceAsString(60, owner.getStackTrace()))
                 );
                 throw new DotConcurrentException(
                        String.format("Unable to acquire Lock on key `%s` and thread `%s`.", key,
                                Thread.currentThread().getName())
                 );
             }
            inspectLocks();
            Logger.info(StripedLockImpl.class, " Key `" + key + "` locked.");
            try {
                return callback.execute();
            } finally {
                tryUnlock(key, lock);
                sanityCheck();
            }
        }
    }



    private void tryUnlock(K key, final ReentrantLock lock){
       try{
           lock.unlock();
           Logger.info(StripedLockImpl.class, " Key `" + key + "` unlocked.");
       }finally {
          if(!lock.isLocked()){
              locks.remove(key);
              Logger.info(StripedLockImpl.class, " Key `" + key + "` removed.");
           }
       }
    }

    /**
     * Internal try lock impl. With Void Delegate
     * These methods are hidden from the public interface
     * @param key
     * @param callback
     * @param time
     * @param unit
     * @throws Throwable
     */
    void tryLock(final K key, final VoidDelegate callback, final long time, final TimeUnit unit)
            throws Throwable {
/*
        final ReentrantLock lock = (ReentrantLock) lockStripes.get(key);
        Logger.info(StripedLockImpl.class, () -> "Void delegate key:" + key);
        if (lock.isHeldByCurrentThread()) {
            Logger.info(StripedLockImpl.class, () -> "Already held by:" + key);
            callback.execute();
        } else {
            if (!lock.tryLock(time, unit)) {
                throw new DotConcurrentException(
                        String.format("Unable to acquire Lock on key `%s` and thread `%s`. ", key,
                                Thread.currentThread().getName())
                );
            }
            try {
                callback.execute();
            } finally {
                Logger.info(StripedLockImpl.class, () -> " Key `" + key + "` unlocked.");
                lock.unlock();
            }
        }
        */
    }

    private synchronized Thread getOwnerThread(final Lock lock) {
        try {
            Method method = lock.getClass().getDeclaredMethod("getOwner");
            if(!method.isAccessible()){
                method.setAccessible(true);
            }
            return Thread.class.cast(method.invoke(lock));
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    private synchronized boolean isLockAlreadyHeld(final Thread thread){
        for(final ReentrantLock lock: locks.values()){
            if(thread == getOwnerThread(lock)){
                return true;
            }
        }
        return false;
    }

    private void inspectLocks(){
       inspectLocks(locks);
    }

    private void inspectLocks(final ConcurrentMap<K,ReentrantLock> locks){
        Logger.info(StripedLockImpl.class, () ->  "begin lock inspection:");
        locks.forEach((k, reentrantLock) -> {
            Logger.info(StripedLockImpl.class, () ->   k + " : " +reentrantLock);
        });
        Logger.info(StripedLockImpl.class, () ->  "end lock inspection");
    }

    private void sanityCheck(){
        assert(locks.values().stream().noneMatch(reentrantLock -> !reentrantLock.isLocked()));
    }

}
