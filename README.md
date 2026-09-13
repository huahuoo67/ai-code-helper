# AI 编程小助手

基于 Spring Boot、LangChain4j 和 Vue 3 的编程学习与求职问答项目，支持流式回答、Redis 会话记忆、知识库检索以及工具调用。

> 项目已由作者部署到个人服务器。实际访问地址与服务器部署细节待作者确认，本文先说明仓库中已有的代码和部署配置。

## 功能

- **流式对话**：通过 SSE 逐步返回回答，前端支持停止接收、复制回答和新建会话。
- **编程学习与求职问答**：覆盖学习路线、项目建议、求职指南和面试准备。
- **会话记忆**：按 `memoryId` 存储到 Redis，每个会话保留最多 10 条消息，默认 TTL 为 7 天。
- **RAG 知识库**：加载 classpath 中的 `docs` 文档，分段后写入 Redis 向量库；每次检索最多返回 5 条、最低相似度阈值为 0.75。
- **工具调用**：通过 Jsoup 检索面试题，通过智谱 MCP 接入网络搜索。
- **基础输入检查**：包含简单的英文敏感词匹配示例，并非完整内容审核系统。

## 技术栈

以下版本来自项目配置，不代表服务商当前推荐版本。

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5.11、Maven |
| AI 编排 | LangChain4j 1.18.0-beta28、DashScope、MCP |
| 数据存储 | Redis 会话存储、Redis 向量检索、Jedis |
| 前端 | Vue 3、Vite 6、Axios、原生 EventSource |
| 部署 | Docker Compose、Nginx、Java 21 JRE |

## 目录结构

```text
ai-code-helper/
├── src/main/java/com/fzq/aicodehelper/
│   ├── ai/                    # 模型服务、RAG、MCP、工具与输入检查
│   ├── config/                # Redis 与跨域配置
│   └── controller/            # SSE 对话接口
├── src/main/resources/
│   ├── application.yml        # 公共配置，不包含真实密钥
│   ├── system-prompt.txt       # 系统提示词
│   └── docs/                  # 知识库文档
├── src/test/                  # 依赖外部服务的集成测试示例
├── ai-code-helper-frontend/    # Vue 前端源码和依赖锁文件
├── docker/                    # 运行时镜像与 Nginx 配置
├── docker-compose.yml
├── .env.example               # 环境变量示例
└── pom.xml
```

## 运行前准备

1. 安装 Java 21、Maven，以及 Node.js/npm。Maven Wrapper 也可使用，首次运行需要下载其配置的 Maven 版本。
2. 准备 DashScope 聊天、流式聊天和 Embedding 调用凭据，以及智谱 MCP 服务凭据。
3. 准备支持项目所需向量索引与检索能力的 Redis 服务。只有普通键值读写能力的 Redis 实例不足以支持本项目 RAG；实际服务版本由部署者确认。
4. 确认 Embedding 输出维度与 `APP_REDIS_VECTOR_DIMENSION` 一致，代码默认维度为 `1024`。

应用启动时会连接 Redis、初始化 MCP，并可能调用 Embedding 服务建立索引，因此并非仅启动一个 HTTP 服务。外部调用可能产生费用。

## 配置

复制 `.env.example` 为 `.env` 并在本地填写。**Docker Compose 会通过 `env_file` 读取它；直接运行 Spring Boot 或 Maven 时不会自动读取这个文件**，请通过终端环境变量、IDE 运行配置或外部 Spring 配置注入。

| 环境变量 | 用途 / 默认值 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 示例为 `prod`；公共配置默认为 `local`，仓库中没有专用 prod 配置文件 |
| `LANGCHAIN4J_COMMUNITY_DASHSCOPE_CHAT_MODEL_API_KEY` | 聊天模型密钥 |
| `LANGCHAIN4J_COMMUNITY_DASHSCOPE_EMBEDDING_MODEL_API_KEY` | 向量模型密钥 |
| `LANGCHAIN4J_COMMUNITY_DASHSCOPE_STREAMING_CHAT_MODEL_API_KEY` | 流式模型密钥 |
| `BIGMODEL_API_KEY` | 智谱 MCP 密钥 |
| `APP_REDIS_HOST` / `APP_REDIS_PORT` | Redis 地址和端口；代码默认 `127.0.0.1:6379` |
| `APP_REDIS_USER` / `APP_REDIS_PASSWORD` | Redis 用户与密码；默认用户为 `default`，默认密码为空 |
| `APP_REDIS_TTL_SECONDS` | 会话 TTL，默认 `604800` |
| `APP_REDIS_VECTOR_DIMENSION` | 向量维度，默认 `1024` |
| `APP_REDIS_VECTOR_INDEX_NAME` | 索引名，默认 `ai-code-helper-vector-index` |
| `APP_REDIS_VECTOR_KEY_PREFIX` | 向量 key 前缀，默认 `ai-code-helper:vector:` |
| `APP_REDIS_VECTOR_INGESTION_MARKER` | 建库标记，默认 `ai-code-helper:rag:indexed:v1` |

模型名称配置在 `application.yml`，请按账户实际可用模型设置，也可通过对应的 `..._MODEL_NAME` 环境变量覆盖。前端通过 `VITE_API_BASE_URL` 设置接口前缀，默认 `/api`；前端环境变量会进入浏览器产物，请勿存放服务端密钥。

本地专用配置 `application-local.yml`、`application-local.yaml`、`application-local.properties` 已加入 Git 忽略和 Maven 资源排除规则。需要使用这类配置时，应从应用外部加载；不要依赖其被打入 JAR。修改资源排除规则后请执行 `clean package`，避免旧构建残留。

## 本地开发

先向运行环境注入上述真实配置，再在项目根目录启动后端：

```bash
mvn spring-boot:run
```

也可使用 Windows 的 `mvnw.cmd` 或 macOS/Linux 的 `./mvnw` 替代 `mvn`。后端默认监听 `8081`，接口上下文为 `/api`。

在前端目录运行：

```bash
npm ci
npm run dev
```

浏览器访问本机 5173 端口。开发服务器把 `/api` 请求代理到本机 8081 端口。

## 构建与测试

```bash
# 根目录：编译主代码、测试代码并打包，跳过外部服务测试
mvn -DskipTests clean package

# 前端目录：根据锁文件安装并构建
npm ci
npm run build
```

后端输出 `target/ai-code-helper-0.1.0.jar`，前端输出 `ai-code-helper-frontend/dist/`。

现有 `@SpringBootTest` 测试会访问真实 Redis、模型服务和 MCP，不是离线单元测试。仅在配置独立测试资源并确认调用费用后执行 `mvn test`。其中输入检查测试目前未声明预期异常，测试断言和覆盖仍需完善。跳过测试的构建成功不等于端到端验证通过。

## Docker Compose 部署配置

当前 Dockerfile 是**运行时镜像**，需要预先构建产物，不会自动编译源码。按照现有配置，构建上下文需要以下内容：

```text
项目根目录/
├── app/ai-code-helper.jar     # 从 target 中复制并重命名
├── web/                      # 复制前端 dist 内的全部内容
├── docker/
├── docker-compose.yml
└── .env                      # 仅部署环境保存，不提交
```

准备好以上产物并填写 `.env` 后，在项目根目录执行：

```bash
docker compose up -d --build
docker compose ps
```

- Compose 将前端映射到宿主机 `8082` 端口，后端 `8081` 仅在容器网络内暴露。
- Nginx 将 `/api/` 代理到 `backend:8081`，并关闭代理缓冲以支持 SSE。
- 当前 Compose **没有 Redis 服务**。请配置后端容器可达的 Redis 地址；容器内的 `127.0.0.1` 指向容器自身。
- 域名、HTTPS、访问控制以及服务器上的实际部署目录未在仓库中定义，应按实际环境配置。
- `app/`、`web/`、`target/`、`dist/` 和 `release/` 为生成内容，不提交 Git。

## 对话接口

`GET /api/ai/chat`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `memoryId` | int | 会话标识 |
| `message` | string | 用户问题，应进行 URL 编码 |

响应为 SSE 数据流。前端使用 `EventSource` 接收；当前后端以连接关闭表示结束，没有显式完成事件。

## 使用边界与注意事项

- 当前代码未实现登录鉴权、请求限流或会话所有权验证。`memoryId` 来自客户端，不是权限凭证；对外开放前应补齐这些控制。
- 当前 CORS 允许任意来源并允许携带凭据，生产环境应限制为真实前端来源。
- GET 查询参数可能进入代理访问日志；请勿在问题中输入密钥或其他私密数据。普通聊天代码也包含回答日志，日志留存策略需要另行检查。
- MCP 请求和响应详细日志已关闭，以降低凭据和对话内容进入日志的风险。
- 当前前端可能将收到部分内容后的连接异常当作完成；完整的成功/失败区分仍需显式终止事件。
- 知识库仅在建库标记不存在时导入。文档变化不会自动刷新索引，重建需同时规划旧向量清理、索引维度和建库标记，避免重复或混合数据。
- 面试题检索依赖第三方页面结构，结果可能为空；模型与 MCP 能力以实际服务配置为准。

## 提交范围

提交源码、知识库（需作者确认可公开范围）、依赖清单与锁文件、Maven Wrapper 脚本、脱敏配置示例、Docker/Nginx 配置及本文档。

不提交真实密钥、环境私有配置、个人简历、IDE 文件、依赖目录、日志、缓存或构建产物。仓库尚未选择开源许可证，发布可见性与授权范围由作者决定。
