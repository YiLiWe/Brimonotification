package com.example.brimonotification.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.NumberPicker;

import com.example.brimonotification.bean.NotionalPoolingBean;
import com.example.brimonotification.runnable.NotionalPoolingDataRunnable;
import com.example.brimonotification.utils.NotionalPoolingSharedPreferencesUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自动归集
 */
@Data
public class NotionalPoolingAccessibilityService extends AccessibilityService implements OnNotionalPoolingListener {
    private static final String TAG = "NotionalPoolingAccessibilityService";
    private final String pass = "Tang443356@";//登录密码
    private String amount = "1000";//余额
    private NotionalPoolingBean poolingBean = null;//转账信息
    private boolean isRun = true; // Ensures thread-safe access
    private final Timer timer = new Timer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AccessibilityNodeInfo nodeInfo = null;

    private final long NotionalPoolingTimeMAX = 1000;//获取归集数据集间隔
    private final long POST_DELAY_MS = 20000, GESTURE_DURATION_MS = 1000; // Delay for posting logs

    private NotionalPoolingSharedPreferencesUtil sharedPreferencesUtil;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "服务启动");
        initData();
        startData();
    }

    private void initData() {
        sharedPreferencesUtil = new NotionalPoolingSharedPreferencesUtil(this);
        poolingBean = sharedPreferencesUtil.getObject("bean", NotionalPoolingBean.class);
    }

    private void print(String msg) {
        Log.d(TAG, msg);
    }

    private void NotionalPooling() {
        if (!isRun) return;
        if (poolingBean == null && !amount.equals("0")) {//判断数据，金额不为0，才执行
            print("请求归集数据");
            new Thread(new NotionalPoolingDataRunnable(this)).start();
        } else {
            print("归集数据不为空");
        }
    }

    private void startData() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                simulateSwipeUp();
            }
        }, POST_DELAY_MS);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                NotionalPooling();
            }
        }, NotionalPoolingTimeMAX);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                CharSequence sequence = event.getClassName();
                if (sequence != null) {
                    boolean is = nodeInfo == null;
                    Log.i(TAG, "className =" + sequence + "|界面信息是否为空:" + is);
                }
            }
            if (nodeInfo != null) {
                handleLogin(nodeInfo);
                handleAmount(nodeInfo);
                handleTransfer(nodeInfo);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void start() {
        if (this.nodeInfo == null) return;
        handleLogin(this.nodeInfo);
        handleAmount(this.nodeInfo);
        handleTransfer(this.nodeInfo);
    }

    /**
     * 处理转账转账
     *
     * @param nodeInfo
     */
    private void handleTransfer(AccessibilityNodeInfo nodeInfo) {
        if (poolingBean == null) {
            print("数据为空");
            return;
        } else {
            print("运行x");
        }
        ClickTransfer(nodeInfo);//首页
        ClickTambahPenerimaBaru(nodeInfo);
        if (!nodeInfo.findAccessibilityNodeInfosByText("Penerima Baru").isEmpty()) {//输入账号信息
            ClickSelectBank(nodeInfo);
        } else if (!nodeInfo.findAccessibilityNodeInfosByText("Masukkan Nominal").isEmpty()) {//输入金额
            editNominal(nodeInfo);
        }
    }

    /**
     * 输入金额
     *
     * @param nodeInfo
     */
    private void editNominal(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/et_nominal");
        for (AccessibilityNodeInfo bank : nodeInfos) {
            if (bank.getText() == null) continue;
            String text = bank.getText().toString();
            if (!text.equals(poolingBean.getAmount())) {//输入金额
                editText(bank, poolingBean.getAmount());
            } else {//确认转账
                ClickLanjut(nodeInfo);
            }
        }
    }

    /**
     * 选择银行
     *
     * @param nodeInfo
     */
    private void ClickSelectBank(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/et_bank");
        for (AccessibilityNodeInfo bank : nodeInfos) {
            if (bank.getText() == null) continue;
            String text = bank.getText().toString();
            if (!text.contains(poolingBean.getBank())) {//不存在则点击选择
                bank.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {//输入银行卡
                editAccount(nodeInfo);
            }
        }
        //上面点击，以后输入银行
        editBank(nodeInfo);
    }

    /**
     * 输入银行卡
     *
     * @param nodeInfo
     */
    private void editAccount(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/et_norek");
        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            if (nodeInfo1.getText() == null) continue;
            String text = nodeInfo1.getText().toString();
            if (!text.equals(poolingBean.getAccount())) {//未输入银行号
                editText(nodeInfo1, poolingBean.getAccount());
            } else {//以输入银行卡
                ClickLanjut(nodeInfo);
            }
        }
    }

    /**
     * 确认信息
     *
     * @param nodeInfo
     */
    private void ClickLanjut(AccessibilityNodeInfo nodeInfo) {
        ClickNodeInfo(nodeInfo, "id.co.bri.brimo:id/btn_lanjut");
    }

    /**
     * 输入银行
     *
     * @param nodeInfo
     */
    private void editBank(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> searchView = nodeInfo.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/searchView");
        for (AccessibilityNodeInfo search : searchView) {
            if (search.getText() == null) continue;
            String text = search.getText().toString();
            if (text.contains(poolingBean.getBank())) {//存在则选择
                optionBank(nodeInfo);
            } else {
                editText(search, poolingBean.getBank());
            }
        }
    }

    /**
     * 选择框，选择
     *
     * @param nodeInfo
     */
    private void optionBank(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> option_bank = nodeInfo.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/tv_option_name");
        for (AccessibilityNodeInfo bank : option_bank) {
            if (bank.getText() == null) continue;
            String text = bank.getText().toString();
            if (text.contains(poolingBean.getBank())) {//不存在则点击选择
                boolean is = bank.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                print(String.format("选择银行:%s|选择状态:%s", text, is));
            }
        }
    }

    /**
     * 点击进入转账
     *
     * @param nodeInfo
     */
    private void ClickTambahPenerimaBaru(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByText("Tambah Penerima Baru");
        print("数量:" + nodeInfos.size());
        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            boolean is = nodeInfo1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            print("点击进入转换:" + is);
        }
    }

    /**
     * 点击转账
     *
     * @param nodeInfo
     */
    private void ClickTransfer(AccessibilityNodeInfo nodeInfo) {
        print("获取转账点击");
        List<AccessibilityNodeInfo> transfers = nodeInfo.findAccessibilityNodeInfosByText("Transfer");
        for (AccessibilityNodeInfo transfer : transfers) {
            if (transfer.getViewIdResourceName() == null) continue;
            print(transfer.getViewIdResourceName());
            if (transfer.getViewIdResourceName().equals("id.co.bri.brimo:id/namaMenu")) {
                AccessibilityNodeInfo parent = transfer.getParent();
                boolean is = parent.getParent().getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                print("首页按钮点击事件:" + is);
            }
        }
    }


    /**
     * 获取余额
     *
     * @param nodeInfo
     */
    private void handleAmount(AccessibilityNodeInfo nodeInfo) {
        String text = getText(nodeInfo, "id.co.bri.brimo:id/total_saldo_ib");
        if (text == null) return;
        if (text.startsWith("Rp")) {
            amount = getInteger(text);
        }
    }

    /**
     * 处理登录
     *
     * @param nodeInfo
     */
    private void handleLogin(AccessibilityNodeInfo nodeInfo) {
        //点击首页登录
        ClickNodeInfo(nodeInfo, "id.co.bri.brimo:id/btn_login");
        if (EditNodeInfo(nodeInfo, "id.co.bri.brimo:id/et_password", pass)) {
            ClickNodeInfo(nodeInfo, "id.co.bri.brimo:id/button_login");
        }
    }

    /**
     * 获取文本
     *
     * @param nodeInfo
     * @param id
     * @return
     */
    private String getText(AccessibilityNodeInfo nodeInfo, String id) {
        List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByViewId(id);
        for (AccessibilityNodeInfo pass : nodeInfos) {
            if (pass.getText() == null) continue;
            return pass.getText().toString();
        }
        return null;
    }

    /**
     * 提取纯数字
     *
     * @param input
     * @return
     */
    private String getInteger(String input) {
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(input);
        StringBuilder digitsOnly = new StringBuilder();
        while (matcher.find()) {
            // 获取匹配的数字字符
            digitsOnly.append(matcher.group());
        }
        return digitsOnly.toString();
    }

    /**
     * 输入内容
     *
     * @param nodeInfo
     * @param id
     * @param text
     */
    private boolean EditNodeInfo(AccessibilityNodeInfo nodeInfo, String id, String text) {
        List<AccessibilityNodeInfo> Passwords = nodeInfo.findAccessibilityNodeInfosByViewId(id);
        for (AccessibilityNodeInfo pass : Passwords) {
            if (pass.getText() == null) continue;
            String string = pass.getText().toString();
            if (!string.equals(text)) {
                editText(pass, text);
                print(String.format("输入登录密码:%s|ID:%s", text, id));
                return false;
            } else {
                print("输入状态:" + string);
                return true;
            }
        }
        return false;
    }

    /**
     * 模拟点击
     *
     * @param nodeInfo
     * @param id
     */
    private void ClickNodeInfo(AccessibilityNodeInfo nodeInfo, String id) {
        List<AccessibilityNodeInfo> logins = nodeInfo.findAccessibilityNodeInfosByViewId(id);
        for (AccessibilityNodeInfo login : logins) {
            boolean is = login.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            print(String.format("点击ID:%s|点击状态:%s", id, is));
        }
    }

    /**
     * 输入文字
     *
     * @param nodeInfo
     * @param msg
     */
    private void editText(AccessibilityNodeInfo nodeInfo, String msg) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, msg);
        boolean is = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        print(String.format("输入信息:%s|输入状态:%s", msg, is));
    }

    private void SwipeUp() {
        Path path = new Path();
        path.moveTo(500, 1000);
        path.lineTo(500, 1500);
        GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(path, 0, GESTURE_DURATION_MS);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(strokeDescription);
        dispatchGesture(builder.build(), null, null);
    }

    private void simulateSwipeUp() {
        if (poolingBean == null) {
            handler.post(this::SwipeUp);
            print("模拟滑动");
        } else {
            print("停止滑动");
        }
        if (isRun) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    simulateSwipeUp();
                }
            }, POST_DELAY_MS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRun = false;
        poolingBean = null;
        amount = "0";
        Log.d("详细", "关闭");
    }


    @Override
    public void onInterrupt() {
    }

    @Override
    public void onEntity(NotionalPoolingBean bean) {
        sharedPreferencesUtil.savaObject("bean", bean);
        this.poolingBean = bean;
    }
}
