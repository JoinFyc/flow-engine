## wflow-pro 后端

#### 📌 pro私有仓库禁止 直接fork，否则永久移出仓库，请遵循授权协议使用

#### 📢 严禁私下传播、倒卖等未经授权侵犯知识产权的使用行为

使用方式及文档参见官网 [wflow-pro](http://wflow.willianfu.top/docs/dev-pro/project.html)

## 注意，后端版本 >= 1.5.0 的用户
> 在流程引擎 act_xxx 表创建成功后，请将流程变量表
> `act_ru_variable` 和 `act_hi_varinst` 的 TEXT_ 字段类型改成 text 类型或者放大长度，否则会出现流程变量值过长导致的异常
> 在新版本中，添加了流程变量序列化自定义，表单的值会存成json字符串，所以需要放大字段长度
