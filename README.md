# TUI4J

[![Maven Central](https://img.shields.io/maven-central/v/com.williamcallahan/tui4j)](https://central.sonatype.com/artifact/com.williamcallahan/tui4j)
[![GitHub Release](https://img.shields.io/github/v/release/WilliamAGH/tui4j)](https://github.com/WilliamAGH/tui4j/releases)
[![Context7](src/main/resources/static/img/context7-badge.svg)](https://context7.com/williamagh/tui4j)
[![DeepWiki](src/main/resources/static/img/deepwiki-badge.svg)](https://deepwiki.com/WilliamAGH/tui4j)
[![Docs](https://img.shields.io/badge/docs-mintlify-18b884)](https://www.mintlify.com/WilliamAGH/tui4j)

**TUI4J** (Terminal User Interface for Java) is a Java TUI framework inspired by [Bubble Tea](https://github.com/charmbracelet/bubbletea). It includes a compatibility module that mirrors the original Go API for developers familiar with the Charm ecosystem.

![Pulse TUI4J](src/main/resources/static/img/Pulse_TUI4J_Screenshot.png)

[![Brief screenshot](src/main/resources/static/img/brief-screenshot.png)](https://github.com/WilliamAGH/brief)

## Installation

> Latest stable release: **0.3.3** — [view on Maven Central](https://central.sonatype.com/artifact/com.williamcallahan/tui4j) | [view on mvnrepository](https://mvnrepository.com/artifact/com.williamcallahan/tui4j)

### Maven

```xml
<dependency>
    <groupId>com.williamcallahan</groupId>
    <artifactId>tui4j</artifactId>
    <version>0.3.3</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.williamcallahan:tui4j:0.3.3'
```

### Snapshots

Snapshots are published automatically on every push to `main` and are available from Sonatype's snapshot repository (separate from Maven Central releases).

Maven:

```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.williamcallahan</groupId>
        <artifactId>tui4j</artifactId>
        <version>0.3.4-SNAPSHOT</version>
    </dependency>
</dependencies>
```

Gradle:

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    implementation("com.williamcallahan:tui4j:0.3.4-SNAPSHOT")
}
```

## Quick Start

TUI4J uses [The Elm Architecture](docs/how-to/tutorial.md): implement a `Model` with `init()`, `update()`, and `view()` methods, then run it with `Program`:

```java
new Program(new MyModel()).run();
```

## Examples

See the [examples](src/main/resources/examples) directory for lists, text inputs, spinners, and more. For Spring Boot integration, see the [Spring examples](src/main/resources/examples/spring).

The screenshots above show [Pulse](src/main/resources/examples/showcases/README.md) (a terminal implementation demo) and [Brief](https://github.com/WilliamAGH/brief) (a terminal AI chat client built with TUI4J).

## Documentation

- [Tutorial](docs/how-to/tutorial.md) - Complete walkthrough
- [List Component](docs/how-to/list-component.md) - List widget guide
- [Compatibility Maps](docs/compatibility) - Charm API mappings
- [Porting Status](docs/compatibility/status.md) - What's implemented

## Built with TUI4J

### Brief

**[Brief](https://github.com/WilliamAGH/brief)** - Terminal AI chat client with slash-command palette and local tool execution. Available via Homebrew.

**Using TUI4J in your project?** We'd love to feature it! [Open an issue](https://github.com/WilliamAGH/tui4j/issues/new?title=Add%20project%20to%20Built%20with%20TUI4J&labels=showcase) or submit a PR.

## Compatibility with Charmbracelet

TUI4J ports the [charmbracelet](https://github.com/charmbracelet) ecosystem to Java:

| Go Library | Description | Java Package |
|------------|-------------|--------------|
| [bubbletea](https://github.com/charmbracelet/bubbletea) | Core TUI framework | `compat.bubbletea.*` |
| [bubbles](https://github.com/charmbracelet/bubbles) | UI components (viewport, textarea, table, progress, etc.) | `compat.bubbles.*` |
| [lipgloss](https://github.com/charmbracelet/lipgloss) | Styling, colors, borders, layout | `compat.lipgloss.*` |
| [x/ansi](https://github.com/charmbracelet/x) | ANSI parsing, text width | `compat.x.ansi.*` |
| [harmonica](https://github.com/charmbracelet/harmonica) | Spring physics animation | `compat.harmonica.*` |

TUI4J seeks to replicate upstream behavior as closely as possible, with a general 1:1 mapping for compatibility. When TUI4J adds functionality without a charmbracelet equivalent, those APIs are treated as additive extensions.

Check [status.md](docs/compatibility/status.md) and [docs/compatibility](docs/compatibility) for current porting status.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on reporting issues and submitting PRs.

## Acknowledgments & License

[MIT License](LICENSE.md)

This project began as a fork of the original [Latte](https://github.com/activey/latte) by [Lukasz Grabski](https://github.com/activey), which was released under the MIT License.

[Bubble Tea](https://github.com/charmbracelet/bubbletea) is a trademark of [Charmbracelet, Inc.](https://charm.sh/) The original Go implementations are also licensed under MIT.

## Other Java Projects

- **[Apple Maps Java](https://github.com/WilliamAGH/apple-maps-java)** - Java SDK for Apple Maps Server API
- **[Other projects](https://williamcallahan.com/projects)** - More from the author
