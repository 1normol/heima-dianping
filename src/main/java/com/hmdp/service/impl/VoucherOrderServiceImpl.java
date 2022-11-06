package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.*;

import static com.hmdp.utils.SystemConstants.REDIS_SHOP_SECKILL_USER;
import static com.hmdp.utils.SystemConstants.REDIS_SHOP_SECKILL_VOUCHER;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    BlockingQueue<VoucherOrder> blockingQueue = new ArrayBlockingQueue(1024*1024);
    ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();


    private class VoucherOrderHandler implements Runnable {
      @Override
      public void run() {
        while (true){
            try {
                VoucherOrder order = blockingQueue.take();
                createOrder(order);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
      }
  }

    @PostConstruct
    private void init(){
        threadPoolExecutor.submit(new VoucherOrderHandler());
        System.out.println("开始执行线程池任务");
    }

    private void createOrder(VoucherOrder order) {
        seckillVoucherService.update()
                .setSql("stock = stock-1")
                .gt("stock",0)
                .eq("voucher_id",order.getVoucherId())
                .update();
        save(order);
        System.out.println("订单信息:"+order);
    }


    @Override
    public Result doSeckill(Long voucherId) {

        Long userId = UserHolder.getUser().getId();

        Long result = (Long) redisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId, userId);
        if (result != 0){
          //  System.out.println(result==1 ? "库存不足" : "不可重复下单");
          return  Result.fail(result==1 ? "库存不足" : "不可重复下单");
        }
        //为0 ，有购买资格，把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        // 放入阻塞队列
        blockingQueue.add(voucherOrder);

        return Result.ok(0);
    }
}
