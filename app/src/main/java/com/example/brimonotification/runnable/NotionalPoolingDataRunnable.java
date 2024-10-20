package com.example.brimonotification.runnable;

import com.example.brimonotification.bean.NotionalPoolingBean;
import com.example.brimonotification.service.NotionalPoolingAccessibilityService;

import lombok.Data;

//获取归集数据
@Data
public class NotionalPoolingDataRunnable implements Runnable {
    private final NotionalPoolingAccessibilityService service;
    private final Object lock = new Object();

    @Override
    public void run() {
        if (service.getPoolingBean() == null) return;
        synchronized (lock) {
            NotionalPoolingBean poolingBean = new NotionalPoolingBean();
            poolingBean.setAccount("119301023317509");
            poolingBean.setBank("BRI");
            poolingBean.setPayerName("juwendi");
            poolingBean.setAccount("10000");
            service.setPoolingBean(poolingBean);
        }
    }
}
