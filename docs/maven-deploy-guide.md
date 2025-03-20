# Maven 部署指南

本文档提供将 BlackFog 库部署到 Maven 仓库的说明。

## 准备工作

在部署之前，您需要完成以下准备工作：

### 1. 配置 Maven 凭证

在您的 `~/.m2/settings.xml` 文件中添加以下配置：

```xml
<settings>
  <servers>
    <server>
      <id>clojars</id>
      <username>your-clojars-username</username>
      <password>your-clojars-token</password>
    </server>
    <server>
      <id>github</id>
      <username>your-github-username</username>
      <password>your-github-token</password>
    </server>
    <!-- 如果您计划发布到 Maven Central -->
    <server>
      <id>ossrh</id>
      <username>your-sonatype-username</username>
      <password>your-sonatype-password</password>
    </server>
  </servers>
  
  <!-- GPG 签名配置 -->
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

### 2. 创建并配置 GPG 密钥

如果您计划发布到 Maven Central，需要创建 GPG 密钥：

```bash
# 生成 GPG 密钥
gpg --gen-key

# 查看已生成的密钥
gpg --list-keys

# 上传公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 3. 修改项目配置

在 `build.clj` 文件中，更新以下配置：

1. 将 `io.github.yourusername/blackfog` 修改为您的真实 Maven 组织和库名
2. 根据您的需要更新 GitHub 链接和项目描述

## 构建和部署

### 本地安装

要在本地安装库（用于测试）：

```bash
clojure -T:build install
```

### 部署到 Clojars

要部署到 Clojars：

```bash
clojure -T:build deploy
```

### 部署到 Maven Central

部署到 Maven Central 需要额外的配置，具体步骤如下：

1. 注册 Sonatype OSSRH 帐户
2. 在 `build.clj` 中修改 `deploy` 函数：

```clojure
(defn deploy [_]
  (jar nil)
  (b/process {:command-args ["mvn" "deploy:deploy-file"
                             (str "-Dfile=" jar-file)
                             (str "-DpomFile=" (str class-dir "/META-INF/maven/" (namespace lib) "/" (name lib) "/pom.xml"))
                             (str "-DrepositoryId=ossrh")
                             (str "-Durl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                             "-Pgpg"]}))
```

3. 执行部署命令：

```bash
clojure -T:build deploy
```

## 验证部署

部署完成后，您可以验证库是否已正确发布：

1. 对于 Clojars：访问 https://clojars.org/io.github.yourusername/blackfog
2. 对于 Maven Central：访问 https://search.maven.org/artifact/io.github.yourusername/blackfog

部署通常需要几分钟到几小时不等，具体取决于仓库的处理时间。

## 版本管理

BlackFog 使用基于 Git 提交数的版本号。如果您需要手动设置版本号，请修改 `build.clj` 中的版本定义：

```clojure
(def version "0.1.0") ;; 替换为静态版本号
```

## 常见问题

### 部署失败

如果部署失败，请检查：

1. Maven 凭证是否正确
2. GPG 密钥是否正确配置
3. POM 文件是否包含所有必要信息

### 依赖冲突

如果有用户报告依赖冲突，可以在 POM 中使用 exclusions 来排除有冲突的依赖项。 