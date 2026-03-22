# TMS 本地设置与开发说明

本文档说明如何在本地搭建 **TMS（Todo 管理服务）** 开发环境、运行应用，以及使用 Docker 一键起栈。

---

## 1. 项目简介

基于 **Spring Boot 3** 的 Web 服务，提供 TODO 相关 REST API；持久层使用 **MyBatis-Plus**，数据库为 **MySQL 8**；接口文档通过 **SpringDoc OpenAPI** 提供。

---

## 2. `doc` 文件夹内文件说明

本目录存放**与数据库、接口契约、说明文档**相关的资料，与业务源码分离，便于查阅与交付。

| 路径                                 | 作用                                                                                                                                                |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **README.md**                      | 本文件：本地环境搭建、运行、Docker、测试及协作说明；并汇总本文件夹各文件用途。                                                                                                        |
| **DDL.sql**                        | MySQL **建表脚本**（`USE tms;` 及业务表结构）。新建库或初始化环境时执行；Docker Compose 首次启动 MySQL 时也会挂载执行（路径以 `ci/docker/docker-compose.yml` 为准）。                          |
| **apidoc/Swagger0 OpenAPI 3.json** | 从应用导出的 **OpenAPI 3.x 规范快照**（JSON），可用于 Postman/Insomnia 导入、契约评审或离线存档；**运行时仍以服务内 SpringDoc 为准**（如 `http://host:port/v3/api-docs`），有接口变更时需重新导出更新此文件。 |
| **deploy architechture.png**       | 部署架构图                                                                                                                                             |
| **todo_live_update                 | todolist实时更新方案                                                                                                                                    |

```text
doc/
├── README.md
├── DDL.sql
└── apidoc/
    └── Swagger0 OpenAPI 3.json   # 文件名以仓库内实际为准
```

---

## 3. 技术栈与版本

| 类别 | 说明 |
|------|------|
| 语言 / 运行时 | Java **17** |
| 框架 | Spring Boot **3.5.x** |
| Web | spring-boot-starter-web |
| 持久化 | MyBatis-Plus **3.5.x**、MySQL Connector/J |
| 校验 | spring-boot-starter-validation |
| API 文档 | springdoc-openapi（Swagger UI） |
| 构建 | Maven 3.x（可用仓库内 `mvnw` / `mvnw.cmd`） |

---

## 4. 环境要求

- **JDK 17**（建议使用 Eclipse Temurin 或 Oracle OpenJDK）
- **Maven 3.9+**（或使用项目自带的 Maven Wrapper）
- **MySQL 8.0+**（本地安装，或通过本仓库提供的 Docker Compose 启动）
- **Docker Desktop**（可选，用于容器化构建与本地编排）

**换行约定（Windows）：** 若参与本项目协作，建议编辑器将行结束符设为 **CRLF**，与团队约定保持一致。

---

## 5. 获取代码与目录说明

```text
tms/
├── src/main/java/com/test/tms/    # 业务代码（Controller / Service / Mapper / Entity 等）
├── src/main/resources/
│   ├── application.yml             # 主配置（数据源等）
│   ├── mapper/                     # MyBatis XML（若有）
│   └── static/todo-ui/             # 前端静态资源（若有）
├── doc/
│   ├── README.md                   # 本说明
│   ├── DDL.sql                     # 建表脚本（MySQL）
│   └── apidoc/                     # OpenAPI 导出等接口契约
├── ci/docker/                      # Dockerfile、compose、.dockerignore
└── pom.xml
```

---

## 6. 数据库准备

### 6.1 创建库并执行 DDL

1. 在 MySQL 中创建数据库（名称需与 JDBC URL 一致，例如本地常用 `tms`）。
2. 执行 **`doc/DDL.sql`** 创建表结构（脚本内含 `USE tms;`，请按实际库名调整或先选中目标库再执行）。

### 6.2 字符集建议

与生产一致时，建议使用 **utf8mb4** 与 **utf8mb4_unicode_ci**（Docker Compose 中的 MySQL 已按此方式配置）。

---

## 7. 本地配置（数据源）

应用默认从 **`src/main/resources/application.yml`** 读取 `spring.datasource.*`。

**安全建议：**

- **不要将真实生产库账号密码提交到 Git。**
- 本地开发推荐任选其一：
  - 使用 **环境变量** 覆盖（Spring Boot 支持），例如：
    - `SPRING_DATASOURCE_URL`
    - `SPRING_DATASOURCE_USERNAME`
    - `SPRING_DATASOURCE_PASSWORD`
    - `SPRING_DATASOURCE_DRIVER_CLASS_NAME`（一般为 `com.mysql.cj.jdbc.Driver`）
  - 或新增 **`application-local.yml`**（加入 `.gitignore`），并在启动时指定：  
    `spring.profiles.active=local`

**示例（PowerShell，仅示意本地库）：**

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/tms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME="你的用户名"
$env:SPRING_DATASOURCE_PASSWORD="你的密码"
```

然后在项目根目录启动应用（见下一节）。

### MyBatis 日志

`application.yml` 中可能配置了将 SQL 打印到控制台（`StdOutImpl`），便于调试；上线或 Docker 环境建议改为 Slf4j 或关闭详细 SQL 日志，避免噪音与信息泄露。

---

## 8. 本地运行与构建

**项目根目录**执行（Windows 可用 `mvnw.cmd`）：

```text
# 编译并运行（跳过测试可加 -DskipTests）
mvnw spring-boot:run

# 或先打包再运行
mvnw -DskipTests package
java -jar target/tms-0.0.1-SNAPSHOT.jar
```

默认 Web 端口一般为 **8080**（若未在配置中修改 `server.port`）。

### API 文档（Swagger UI）

服务启动后访问（路径以实际配置为准）：

- **OpenAPI JSON：** `http://localhost:8080/v3/api-docs`
- **Swagger UI：** `http://localhost:8080/swagger-ui.html` 或 `http://localhost:8080/swagger-ui/index.html`

### 单元测试

```text
mvnw test
```

---

## 9. Docker：生产向镜像构建

在**仓库根目录**，构建上下文为当前目录，指定 Dockerfile 路径：

```text
docker build -f ci/docker/Dockerfile -t tms:latest .
```

说明：多阶段构建会在镜像内完成 Maven 打包，最终镜像仅包含 JRE 与可执行 JAR。

---

## 10. Docker Compose：本地一键起栈（应用 + MySQL）

在**仓库根目录**执行：

```text
docker compose -f ci/docker/docker-compose.yml up --build
```

或进入 `ci/docker` 后：

```text
docker compose -f docker-compose.yml up --build
```

**默认行为摘要：**

- MySQL 映射到宿主机 **3307 → 容器 3306**（避免占用本机已有 3306）。
- 应用映射 **8080 → 8080**。
- 首次初始化数据库时会挂载执行 **`doc/DDL.sql`**。
- 应用通过环境变量覆盖数据源（与 `application.yml` 中可能存在的远程配置无关，以容器内环境变量为准）。

停止并删除容器（数据卷是否删除视需求而定）：

```text
docker compose -f ci/docker/docker-compose.yml down
```

仅删除容器但保留命名卷（MySQL 数据）：使用 `down` 不加 `-v`；需要清空数据时再使用 `down -v`。

---

## 11. 常见问题

| 现象 | 处理思路 |
|------|----------|
| 无法连接数据库 | 检查 URL、端口、库名、用户权限；Docker 场景下应用应访问服务名 `mysql`，而非 `127.0.0.1`。 |
| 表不存在 | 确认已执行 `DDL.sql`，且 `USE` 的库名与 JDBC URL 一致。 |
| 端口被占用 | 修改 `server.port` 或释放 8080 / 3307。 |
| Docker 构建慢 | 已提供 `ci/docker/.dockerignore` 缩小构建上下文；首次拉取基础镜像需网络稳定。 |

---

## 12. 协作与提交

- 提交前确认 **未包含** 个人本机密码、生产密钥、内网地址等敏感信息。
- 大型或破坏性变更建议先开分支，并通过 `mvnw test` 与本地联调后再合并。

---

如有与本文不一致之处，以仓库内 **`pom.xml`**、**`application.yml`** 及 **`ci/docker/`** 下实际配置为准。

