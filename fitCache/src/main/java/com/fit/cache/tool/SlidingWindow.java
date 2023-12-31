package com.fit.cache.tool;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fit.cache.keylistener.KeyListener;

/**
 * 滑动窗口。该窗口同样的key都是单线程计算。
 *
 * @author songhao
 */
public class SlidingWindow {

    private static final Logger LOG = LoggerFactory.getLogger(KeyListener.class);
    private static final String TITLE = "SlidingWindow";

    /**
     * 循环队列，就是装多个窗口用，该数量是windowSize的2倍
     */
    private AtomicLong[] timeSlices;
    /**
     * 队列的总长度
     */
    private int timeSliceSize;
    /**
     * 每个时间片的时长，以毫秒为单位
     */
    private int timeMillisPerSlice;
    /**
     * 共有多少个时间片（即窗口长度）
     */
    private int windowSize;
    /**
     * 在一个完整窗口期内允许通过的最大阈值
     */
    private int threshold;
    /**
     * 该滑窗的起始创建时间，也就是第一个数据
     */
    private long beginTimestamp;
    /**
     * 最后一个数据的时间戳
     */
    private long lastAddTimestamp;

    public SlidingWindow(int duration, int threshold) {
        // 超过10分钟的按10分钟
        if (duration > 600) {
            duration = 600;
        }
        // 要求5秒内探测出来的，
        if (duration <= 5) {
            this.windowSize = 5;
            this.timeMillisPerSlice = duration * 200;
        } else {
            this.windowSize = 10;
            this.timeMillisPerSlice = duration * 100;
        }
        this.threshold = threshold;
        // 保证存储在至少两个window
        this.timeSliceSize = windowSize * 2;

        reset();
    }

    public SlidingWindow(int timeMillisPerSlice, int windowSize, int threshold) {
        this.timeMillisPerSlice = timeMillisPerSlice;
        this.windowSize = windowSize;
        this.threshold = threshold;
        // 保证存储在至少两个window
        this.timeSliceSize = windowSize * 2;

        reset();
    }

    /**
     * 初始化
     */
    private void reset() {
        LOG.info(TITLE, "reset");
        beginTimestamp = System.currentTimeMillis();
        // 窗口个数
        AtomicLong[] localTimeSlices = new AtomicLong[timeSliceSize];
        for (int i = 0; i < timeSliceSize; i++) {
            localTimeSlices[i] = new AtomicLong(0);
        }
        timeSlices = localTimeSlices;
    }

    /**
     * 计算当前所在的时间片的位置
     */
    private int locationIndex() {
        long now = System.currentTimeMillis();
        // 如果当前的key已经超出一整个时间片了，那么就直接初始化就行了，不用去计算了
        if (now - lastAddTimestamp > timeMillisPerSlice * windowSize) {
            reset();
        }

        int index = (int)(((now - beginTimestamp) / timeMillisPerSlice) % timeSliceSize);
        if (index < 0) {
            return 0;
        }
        LOG.info(TITLE, "index:{}", index);
        return index;
    }

    /**
     * 增加count个数量
     */
    public synchronized boolean addCount(int count) {
        // 当前自己所在的位置，是哪个小时间窗
        int index = locationIndex();
        // 然后清空自己前面windowSize到2*windowSize之间的数据格的数据
        // 譬如1秒分4个窗口，那么数组共计8个窗口
        // 当前index为5时，就清空6、7、8、1。然后把2、3、4、5的加起来就是该窗口内的总和
        clearFromIndex(index);

        int sum = 0;
        // 在当前时间片里继续+1
        sum += timeSlices[index].addAndGet(count);
        // 加上前面几个时间片
        for (int i = 1; i < windowSize; i++) {
            sum += timeSlices[(index - i + timeSliceSize) % timeSliceSize].get();
        }

        LOG.info(TITLE, "sum:{}", sum);
        lastAddTimestamp = System.currentTimeMillis();
        return sum >= threshold;
    }

    public int getCount() {
        int sum = 0;
        // 加上前面几个时间片
        for (int i = 1; i < windowSize; i++) {
            sum += timeSlices[i].get();
        }
        return sum;
    }

    private void clearFromIndex(int index) {
        for (int i = 1; i <= windowSize; i++) {
            LOG.info(TITLE, "clear i:{}", i);
            int j = index + i;
            if (j >= windowSize * 2) {
                j -= windowSize * 2;
            }
            timeSlices[j].set(0);
        }
    }

}
