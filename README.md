# codegen-gradle-plugin

Derived from [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin), this plugin is designed for code generation with additional default configurations for ease of use. It utilizes `flyway` and `testcontainer` to generate code from a local database.

# Usage

## Applying the Plugin

```kotlin
plugins {
    id 'io.github.alexritian.codegenGradlePlugin'
}
```

## Configuring the Plugin

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
> The extension name follows [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin) conventions. Multiple configurations are supported as well.

### `database`

By default, `testcontainer` will start a database instance. To use a different database, you can specify the `url`, `driver`, `user`, and `password` parameters.

### `forcedTypes`

You can configure some forced type conversions.
The `timestampToInstant` function converts the following database types to `java.time.Instant`:

- `timestamp`
- `timestamp without time zone`
- `timestamp with time zone`
- `timestamptz`

> [!NOTE]
> Currently, only the `timestampToInstant` method is available, but additional type conversion methods will be added in the future.

### Other Configurations

Some default configurations are provided based on [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin).