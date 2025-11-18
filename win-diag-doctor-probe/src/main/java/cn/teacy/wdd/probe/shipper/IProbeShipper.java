package cn.teacy.wdd.probe.shipper;

import cn.teacy.wdd.protocol.WsMessageContext;

/**
 * 日志数据发送器接口
 * 负责将探针收集的数据运输回服务器
 */
public interface IProbeShipper {

    /**
     * 发送日志数据
     *
     * @param taskId   任务ID
     * @param queryContext 日志查询上下文，包含查询请求和日志条目列表
     * @return 发送是否成功
     */
    boolean ship(String taskId, WsMessageContext queryContext);

}
