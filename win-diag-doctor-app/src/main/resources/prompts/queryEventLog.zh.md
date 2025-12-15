# Windows 事件日志查询工具

## 参数说明
1. `logName` (String, 必填):
    - 目标日志名称。
    - 常用值: `Application` (应用程序), `System` (系统), `Security` (安全), `Setup` (安装)。
    - 注意: 查询 `Security` 日志通常需要管理员权限。

2. `levels` (List<Enum>, 选填):
    - 筛选日志级别。
    - **App/System**: 仅查 `Error`, `Warning`, `Critical`，除非必要请忽略 `Information` 以降噪。
    - **Security**: 必须包含 `Information`，因为关键审核事件(如登录失败、提权)均属此级别。

3. `startHoursAgo / endHoursAgo` (Integer, 选填):
    - **重要**: 必须传递**正整数**。
    - `startHoursAgo`: 查询的**起始点**（多少小时前）。例如 `24` 表示从 24 小时前开始。
    - `endHoursAgo`: 查询的**结束点**（多少小时前）。例如 `1` 表示截止到 1 小时前。
    - 逻辑: `startHoursAgo` 的值通常应**大于** `endHoursAgo`。
    - 示例: 查询“昨天”的日志，可设 `startHoursAgo=48, endHoursAgo=24`。

4. `maxEvents` (int, 选填):
    - 限制返回条数，默认 10。

## 结果解析
1. 元数据:
   - `hasMore`: 若为 true，表示当前时间范围内的日志数量超过了返回限制。可以增大 `maxEvents` 参数再次查询，以获取遗漏的日志。
   - `userContext`:
     - `isAdmin`: **读取 Security 日志必须为 true**。若查 Security 且此值为 false，务必建议用户以管理员身份运行探针。
     - `isReader`: 若为 true，表示用户属于 Event Log Readers 组，可读取除 Security 外的系统日志。
2. 日志条目 (entries):
   - `Id`: 故障检索核心关键字 (如 10016)。
   - `Message`: 重点提取其中的错误码 (HResult) 及 DCOM 组件的 CLSID/APPID。