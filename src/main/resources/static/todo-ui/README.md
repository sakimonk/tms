# TODO 简易 Web UI

原生 HTML/CSS/JS，无构建步骤。随 Spring Boot 一并由 `classpath:/static/` 提供静态资源。

## 访问方式

启动应用后浏览器打开：

- `http://localhost:8080/todo-ui/index.html`  
  （若配置了 `server.port` 或 `context-path`，请相应修改 URL。）

**请勿**用 `file://` 直接打开本地文件，否则无法请求同源下的 `/todos` API。

## 功能

- 列表：筛选（状态、优先级、阻塞、截止时间 `dueFrom`/`dueTo`）、排序、分页；支持勾选多行后 **批量改状态**（`PATCH /todos/batch/status`）
- 新建：含循环任务、依赖 id 列表
- 编辑：标题、描述、截止时间、状态、优先级、依赖（留空保存可清空依赖）
- 删除：点删除后弹出确认框，可在框内勾选「删除时同步关联」：`true`（默认）时在无剩余实例时一并软删循环规则；`false` 时仅删当前实例

## 说明

- 顶部「用户 ID」用于创建/更新时的 `userId` / `updatedBy`，会写入 `localStorage`。
- 循环任务**仅支持在新建时**配置；编辑时对已有循环实例仅提示 `recurrenceId`，规则请在 API 层维护。
