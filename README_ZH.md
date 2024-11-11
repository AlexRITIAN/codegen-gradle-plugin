# codegen-gradle-plugin

从 [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin)修改而来，用于生成代码。添加默认的配置，简化使用。使用 `flyway` 和 `testcontainer` 来实现通过本地数据库来生成代码。

# 使用方法

## 引入插件

```kotlin
plugins {
    id 'io.github.alexritian.codegenGradlePlugin'
}
```

## 配置插件

```kotlin
jooq {
    configurations {
        main {
            database {
                schema = 'public'
                includes = '.*'
            }
            output {
                packageName = 'io.github.alexritian.codegen'
            }
            forcedTypes {
                timestampToInstant()
            }
        }
    }
}
```
> [!NOTE]
>  沿用了 [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin) 的扩展名字。同样支持多个配置。

### `database`

默认会使用 `testcontainer` 来启动一个数据库. 如果要使用其他数据库，可以通过加入 `url`, `driver`, `user` 和 `password` 来配置。

### `forcedTypes`

可以配置一些强制转换的类型
`timestampToInstant` 会将数据库中的下列类型转换为 `java.time.Instant` 类型。

- `timestamp`
- `timestamp without time zone`
- `timestamp with time zone`
- `timestamptz`

> [!NOTE]
> 目前只有 `timestampToInstant` 一个方法，后续会补充其他类型转换方法
### 其他配置

对 [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin) 进行一些默认配置。