# TMS 开发说明

TMS 为基于 Spring Boot 的 Todo 管理后端，提供 REST API，数据存 MySQL，接口文档由 SpringDoc（Swagger）生成。

---

## 1. 技术栈与环境

**技术栈：** Java 17，Spring Boot 3.5.x，MyBatis-Plus，MySQL，SpringDoc OpenAPI，Maven（可用仓库内 `mvnw` / `mvnw.cmd`）。

**本地需要：** JDK 17、Maven 或 Maven Wrapper、MySQL 8。若使用容器构建/运行，需安装 Docker。

**换行符：** Windows 协作建议源码与配置使用 **CRLF**。

---

## 2. `doc` 目录里有什么

| 文件 | 说明 |
|------|------|
| `README.md` | 本说明 |
| `DDL.sql` | MySQL 建表脚本（含 `USE tms;`，库名请与 JDBC 一致） |
| `决策日志.md` | 架构与范围方面的决策备忘 |
| `apidoc/Swagger0 OpenAPI 3.json` | OpenAPI 导出快照；以运行中服务的 `/v3/api-docs` 为准，变更接口后需重导 |
| 其他（如图、方案草稿） | 以仓库内实际文件名为准 |

---

## 3. 仓库结构（摘要）

```text
tms/
├── src/main/java/com/test/tms/   # 业务代码
├── src/main/resources/
│   ├── application.yml           # 主配置
│   ├── mapper/                   # MyBatis XML
│   └── static/todo-ui/           # 静态前端
├── doc/                          # 文档、DDL、OpenAPI 导出等
├── ci/docker/                    # Dockerfile、compose、.dockerignore
└── pom.xml
```

---

## 4. 数据库

1. 创建数据库（例如 `tms`），名称与连接串一致。  
2. 执行 `doc/DDL.sql`。  
3. 建议使用 **utf8mb4**。

---

## 5. 配置与启动

**数据源：** 默认读 `src/main/resources/application.yml`。生产密码勿提交仓库；本地可用环境变量覆盖，例如：

`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_DATASOURCE_DRIVER_CLASS_NAME`（MySQL 一般为 `com.mysql.cj.jdbc.Driver`）。

或使用 `application-local.yml`（勿提交密钥）并设置 `spring.profiles.active=local`。

**启动（在项目根目录）：**

```text
mvnw spring-boot:run
```

打包运行：

```text
mvnw -DskipTests package
java -jar target/tms-0.0.1-SNAPSHOT.jar
```

默认端口 **8080**（未改 `server.port` 时）。

**文档地址：** `http://localhost:8080/v3/api-docs`；Swagger UI：`/swagger-ui.html` 或 `/swagger-ui/index.html`。

**测试：** `mvnw test`。

**说明：** `application.yml` 中若开启 MyBatis SQL 打印到控制台，仅建议开发环境；生产或 Docker 中宜改为 Slf4j 或关闭，减少噪音与泄露风险。

---

## 6. Docker

**构建镜像（仓库根目录）：**

```text
docker build -f ci/docker/Dockerfile -t tms:latest .
```

**Compose（仓库根目录）：**

```text
docker compose -f ci/docker/docker-compose.yml up --build
```

编排内容与端口等以 **`ci/docker/docker-compose.yml`** 为准；停止：`docker compose -f ci/docker/docker-compose.yml down`。

---

