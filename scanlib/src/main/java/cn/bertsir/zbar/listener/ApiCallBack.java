package cn.bertsir.zbar.listener;

/**
 * @author: ShiLiang
 * @describe: 扫码回调
 */
public interface ApiCallBack<T> {
    void success(T t);
    void failed();
    void error(Exception e);
}
