# soffan-continuous-integration

**[TODO] One-line description of project**

<br>

## What this project does

**[TODO]** 1–2 paragraphs

<br>


## How to use it

**Requirements**
- **Java 17** or later
- **Gradle wrapper** (included)

<br>

**Running and testing**
- **Build**: `./gradlew build`
- **Run**: `./gradlew run` or `java -jar build/libs/CI-1.0-SNAPSHOT.jar`
- **Test**: `./gradlew test`

<br>

**Environment Variables** <br>
Create a `gradle.properties` file in the root folder:
| Variable Name      | Description |
| ----------- | ----------- |
| `githubToken`      | Set this for activating ability to report back to GitHub on the commit status.
| `webhookSecret`      | We recommend to use a password-protected Webhook. If this variable is set, the CI server will verify signature of incoming requests.

<br>

> **`githubToken`**: Can be a "Fine-grained Personal Access Token" created under GitHub settings > Developer settings (at the bottom) > Personal access tokens > Fine-grained tokens. Give it Repository access > Only select repositories (for write access) and then your repo. Under permissions you need to add "Commit statuses" and change it to read and write.


<br>

## Implementation of Core CI features
The CI server is built using a modular architecture where `ContinuousIntegrationServer` acts as the orchestrator, delegating specific tasks to specialized utility classes.

### Core CI feature #1: Compilation
- **Implementation**: Upon receiving a valid GitHub `push` event, the server identifies the branch name from the JSON payload. It uses `CommandRunner.cloneOrFetchRepo` to ensure a local copy of the specific branch is present, then executes `./gradlew build -x test` to compile the project while skipping tests to isolate the compilation stage.
- **Unit Testing**: `CommandRunnerTest.java` verifies this feature by initializing a temporary Gradle project and asserting that `buildRepo` returns `true` for a valid project and `false` for an empty or invalid directory.

### Core CI feature #2: Testing
- **Implementation**: Following a successful build, the server invokes `CommandRunner.testRepo`. This method executes `./gradlew test` within the repository directory. Finally, the exit code is captured: a return value of `0` indicates all tests passed, which is then used to determine the final notification status.
- **Unit Testing**: `CommandRunnerTest.java` includes `testEmptyGradleProjectTest`, which simulates a full CI lifecycle—cloning, building, and testing—within a temporary environment to ensure the execution logic is robust.

### Core CI feature #3: Notification (Commit status)
- **Implementation**: The server provides real-time feedback via the GitHub REST API. It first sends a `pending` status when the build begins. Once the build and test stages conclude, `GitHubStatusClient` sends a POST request to GitHub's status endpoint with a state of `success` or `failure`, including a brief description of the outcome (e.g., "Build failed!" or "Tests passed!").
- **Unit Testing**: `GitHubStatusClientTest.java` ensures the URI builder correctly replaces `{sha}` placeholders in the GitHub URL template and validates that the client gracefully handles and rejects malformed URLs.

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

**Pierre Castañeda Segerström, pise@kth.se**
- Setup structure on running terminal commands (`CommandRunner`).
- Implemented verification of signature of incoming requests (`PayloadVerifier`).
- Done most of the documenation and reporting.
