# 诗云（ShiYun）第三方版权与授权致谢声明（NOTICE）

本项目自身源代码采用 [MIT License](LICENSE) 授权开源。

本项目在开发与构建过程中，使用或参考了以下开源项目、公共数据集及设计资源。特此向相关创作者和开源社区致以由衷的谢意：

---

## 一、古籍诗词数据源声明

本项目内置收录的 600 首唐诗与宋词数据，来源于 GitHub 知名开源古籍整理项目 **chinese-poetry**：

- **项目名称**：最全中华古诗词数据库（chinese-poetry）
- **上游仓库**：[https://github.com/chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
- **开源许可证**：MIT License
- **版权所有**：Copyright (c) 2017 JackeyGao
- **法律与公有领域说明**：
  - 本项目所呈现的中国古代诗词原文（李白、杜甫、王维、苏轼、李清照等唐宋历代名家作品），其创作者逝世均已逾数百年乃至上千年，根据各国著作权法，**古代诗词正文本身均已完全处于公有领域（Public Domain）**。
  - 数据结构整理、标点校订与选本分类索引遵从 `chinese-poetry` 上游仓库之 MIT 开源许可条款。

---

## 二、主要系统依赖与构建组件

本项目构建与运行依赖 Google 官方维护的 Android Jetpack、Jetpack Compose 及 Material Design 3 组件库：

- **组件组织**：Google LLC / Android Open Source Project (AOSP)
- **包含组件**：
  - `androidx.compose.bom` / `androidx.compose.material3:material3`
  - `androidx.compose.ui:ui*`
  - `androidx.activity:activity-compose`
  - `androidx.core:core-ktx`
  - `androidx.lifecycle:lifecycle-runtime-ktx`
- **开源许可证**：Apache License, Version 2.0
- **许可文本**：[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

---

## 三、视觉与背景素材说明

- **素材名称**：诗云水墨山水主题背景视觉素材（`bg_home_*.webp` 共 10 张）
- **设计与呈现形式**：
  - 以同一幅中国传统古典水墨山水构图意象为基础，根据诗云 10 套预设主题的专属色彩风格（宣纸米白、竹青、黛青、朱砂、琥珀、松烟墨黑、柳黄、荷青、枫红、雪隐冷灰）分别进行色调微调与反相适配；
  - 画面布局特设中部大面积水流云雾留白区，旨在保障诗词正文阅读的通透度与高对比度。
- **原始底本核查状态**：**【待核实】**
  - 该水墨山水画作的数字化扫描底本源头尚未在代码中附带确凿出处链接，项目不虚构作者姓名、画作名称与馆藏归属。后续版本核实原始文献后将在此持续更新完善。

---

## 四、字体使用与版权边界说明

1. **应用内置字体**：
   - 本项目 APK **未打包任何第三方商业中文字体文件（`.ttf` / `.otf`）**；
   - 诗云内置的字体选项直接调用用户手机系统自带的字体渲染引擎（系统默认字体、系统衬线字体 `Serif`、系统等宽字体 `Monospace`），不产生任何商业字体侵权风险。
2. **用户本地字体导入功能**：
   - 本项目仅提供标准的文件读取接口（调用系统 `OpenDocument` 协议），允许用户将存放于个人设备本地的字体文件导入到应用私有沙盒目录供个人排版使用；
   - 用户自选导入之字体文件的合法性与授权范围，由导入者个人自行负责。

---

## 五、单机运行与数据纯净说明

- 诗云为 **100% 纯本地离线单机应用**，不包含任何第三方数据统计、广告推送或用户跟踪 SDK；
- 应用未申请 `android.permission.INTERNET` 网络访问权限，在操作系统沙盒层面完全阻断任何联网行为。
