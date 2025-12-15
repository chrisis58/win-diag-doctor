本工具用于查询 Windows 事件日志。接受一个 LogQueryRequest 对象作为输入，返回符合查询条件的 Windows 事件日志条目列表。
你可以使用此工具来获取特定日志名称、级别和时间范围内的事件日志，以帮助诊断和分析 Windows 系统中的问题。

## 核心解析规则
1. 元数据 (头部):
   - hasMore: 若为 true，表示当前时间范围内的日志数量超过了返回限制 (被截断)。可以增大 'maxEvents' 参数再次查询，以获取遗漏的日志。
   - userContext (权限):
     - isAdmin: **读取 Security (安全) 日志必须为 true**。若为 false 且查询结果为空，务必建议用户以管理员身份运行。
     - isReader: 若为 true，表示用户属于 Event Log Readers 组，可读取除 Security 外的系统日志。
2. 日志条目 (entries):
   - Id: 故障检索核心关键字 (如 10016)。
   - Message: 重点提取其中的错误码 (HResult) 及 DCOM 组件的 CLSID/APPID。