# 本地密钥配置

PaiFlow 本地开发环境只在仓库根目录的 `.env.local` 中保存真实密钥。模型表、插件表、Redis 和工作流协议只允许保存 `env:变量名` 引用。

```dotenv
PAIFLOW_DEEPSEEK_V4_API_KEY=替换为本机密钥
PAIFLOW_ALI_TTS_API_KEY=替换为本机密钥
```

`.env.local` 已被 Git 忽略。建议执行 `chmod 600 .env.local`，限制为当前用户可读写。

在页面中配置时：

- 模型管理的“API 密钥环境变量”填写 `PAIFLOW_DEEPSEEK_V4_API_KEY`。
- 插件 Server 鉴权的“密钥环境变量”填写 `PAIFLOW_ALI_TTS_API_KEY`。
- 页面提交后，数据库中分别保存 `env:PAIFLOW_DEEPSEEK_V4_API_KEY` 和 `env:PAIFLOW_ALI_TTS_API_KEY`，不会保存真实值。

服务启动时会一次性把仓库根目录 `.env.local` 读取到进程内存，后续请求只解析内存中的值，不会重复读取文件。进程环境变量优先于本地文件，也可以用 `PAIFLOW_SECRETS_FILE` 指向其他本地文件。修改密钥文件后需要重启 Hub、Workflow 和 Link 服务。

已有明文配置可运行：

```bash
./scripts/migrate-local-secrets-to-env.sh
```

迁移脚本会把当前模型和插件明文密钥写入权限为 `600` 的 `.env.local`，把数据库及 Redis 中的值替换为引用，并清理本地运行日志中的同值明文。已发生过泄露的密钥仍应在供应商控制台作废并重新生成。

数据库变更也单独保存在 `docker/PaiFlow/mysql/migrations/20260711_secret_references.sql`，便于审查或手工执行。必须先把真实值写入 `.env.local`，再执行 SQL；SQL 本身不会也不能把数据库中的真实值导出到本地文件。
