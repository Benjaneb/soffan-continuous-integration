# soffan-continuous-integration

**A lightweight, webhook-driven Continuous Integration (CI) server for automated building, testing, and status reporting of GitHub repositories.**

<br>

## What this project does

This project implements a custom Continuous Integration (CI) server that automates the software development lifecycle. Upon receiving a push event from a GitHub webhook, the server securely validates the request signature, clones the specific branch of the repository, and executes an automated build and test suite using Gradle. It then provides immediate feedback by notifying the developers of the build results via the GitHub Commit Status API.

<br>

## How to use it

**Requirements** **[TODO]**
- **Java 17** or later
- **Gradle wrapper** (included)

<br>

**Running and testing** **[TODO]**
- **Build**: `./gradlew build`
- **Run**: `./gradlew run` or `java -jar build/libs/CI-1.0-SNAPSHOT.jar`
- **Test**: `./gradlew test`

<br>

## Contributing

General contribution guidelines are described in [CONTRIBUTING.md](CONTRIBUTING.md).

<br>

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for full license text.

<br>

## Statement of Contributions
This section describes the specific contributions made by each team member for this assignment.

**(TODO)**
**Benjamin Widman, bwidman@kth.se**
- Implementing...
- Design...
- Documented...

**(TODO)**
**David Hübinette, davpers@kth.se**
- Implementing...
- Design...
- Documented...

**(TODO)**
**Daglas Aitsen, daglas@kth.se**
- Implementing...
- Design...
- Documented...

**(TODO)**
**Pierre Castañeda Segerström, pise@kth.se**
- Implementing...
- Design...
- Documented...
