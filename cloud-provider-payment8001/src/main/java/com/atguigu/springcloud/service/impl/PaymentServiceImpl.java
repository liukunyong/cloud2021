package com.atguigu.springcloud.service.impl;

import com.atguigu.springcloud.dao.PaymentDao;
import com.atguigu.springcloud.entities.Payment;
import com.atguigu.springcloud.service.PaymentService;
import org.apache.ibatis.annotations.Param;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Tags;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 */
@Service
public class PaymentServiceImpl implements PaymentService
{
    @Resource
    private PaymentDao paymentDao;

    public int create(Payment payment)
    {
        return paymentDao.create(payment);
    }

    @Trace
    @Tag(key="getPaymentById",value = "returnedObj")
    @Tags(@Tag(key="getPaymentById",value = "arg[0]"))
    public Payment getPaymentById(Long id)
    {
        return paymentDao.getPaymentById(id);
    }

    @Override
    public List<Payment> getAll() {
        return paymentDao.getAll();
    }
}

