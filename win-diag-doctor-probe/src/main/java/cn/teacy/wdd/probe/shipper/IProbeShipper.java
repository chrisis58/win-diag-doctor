package cn.teacy.wdd.probe.shipper;

import cn.teacy.wdd.common.entity.TaskExecutionResult;
import cn.teacy.wdd.common.enums.ExecutionResultEndpoint;

/**
 * 任务结果数据发送器接口
 * 负责将探针收集的数据运输回服务器
 */
public interface IProbeShipper {

    /**
     * 发送日志数据
     *
     * @param endpoint 发送端点
     * @param result   任务执行结果
     * @return 发送是否成功
     */
    <T> boolean ship(ExecutionResultEndpoint endpoint, TaskExecutionResult<T> result);

}
