# 方案 B（客户端 Mixin `Entity.tick()`）可行性记录

> 分支：`feature/plan-b-mixin` —— 实验性尝试，**结论：在 Forge 1.20.1 上不可行**，已回退为方案 A。

## 结论
Forge 1.20.1（ForgeGradle 6 / `net.minecraftforge:forge:47.2.0`）**无法通过标准方式加载 mod 的 mixin 配置**，因此"用客户端 Mixin 拦截 `Entity.tick()` 实现全实体动画定格"的方案 B 不可行。已保留并继续使用方案 A（事件式 `ClientFreezer` + `ClientEntityFreezer`）。

## 实证过程

### 1. Forge 1.20.1 的 FML 没有 mixin 配置加载代码
- `net.minecraftforge:fmlloader:1.20.1-47.2.0` / `fmlcore` / `forge` universal jar 中：**0 条** `mixin` 相关字符串/类。
- `ModFileParser` 不含 `[[mixins]]` 读取（对比 NeoForge `fancymodloader/4.0.39` 的 `ModFileParser$MixinConfig` 才有）。
- 结论：Forge 1.20.1 既不读 `mods.toml` 的 `[[mixins]]`，也不自动扫描 `*.mixins.json`。

### 2. 标准配置方式实测：配置从未被加载
- 放置 `src/main/resources/timestop.mixins.json`（根目录）+ `mods.toml` 追加 `[[mixins]] config="timestop.mixins.json"`。
- 启动客户端并开启 `mixin.debug.verbose=true`，日志中 **无任何** `timestop.mixins` / `EntityFreezeMixin` 记录。
- 只看到 `MixinService [ModLauncher] was successfully booted`（Mixin 库本身在跑），但没有配置喂给它。

### 3. 程序化注册可加载配置，但 Entity 已加载（FATAL）
- 在 `@Mod` 构造器中调用 `MixinEnvironment.getDefaultEnvironment().addConfiguration("timestop.mixins.json")`：
  - 成功：`Selecting config timestop.mixins.json`、`Preparing timestop.mixins.json (1)`
  - 失败：`MixinTargetAlreadyLoadedException: ... target net.minecraft.world.entity.Entity was loaded too early.`（FATAL，客户端无法正常启动）
- 原因：Forge 启动早期（注册表/实体类型初始化）就已加载 `Entity` 类，mod 构造器阶段注册 mixin 已太晚。
- 附带：Mixin 0.8.5 的 `CompatibilityLevel` 最高仅支持 **JAVA_13**，写 `JAVA_17` 会告警。

## 结论与建议
- **保留方案 A**：生物实体用 `ClientFreezer`（取消 `LivingTickEvent`）+ 非生物实体用 `ClientEntityFreezer`（快照复位 + 冻结 tickCount）+ 白名单状态包同步。
- 若一定要用 Mixin（连 partialTick 驱动动画也停），需：
  - 用 CoreMod 等**早于 Entity 类加载**的挂钩注册 mixin 配置（非标准、复杂、风险高）；或
  - 迁移到 **NeoForge**（其 FML 支持 `[[mixins]]`，可正常加载 mixin）。
