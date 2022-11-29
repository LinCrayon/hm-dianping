package com.hmdp.utils;

/**
 * @Author: linshengqian  2022/11/23  13:53
 * @Description:
 */
public interface ILock {

    boolean tryLock(long timeoutSec);

    void unlock();
}
