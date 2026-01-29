# Git Conventions

To ensure our history is readable, automated tools work correctly, and our workflow remains smooth, we follow strict conventions for commits, branches and pull requests.

<br>

### Allowed Commit Types

| Type | Description |
| :--- | :--- |
| `feat` | A new feature for the user. |
| `fix` | A bug fix. |
| `docs` | Documentation only changes. |
| `style` | Formatting, missing semi-colons, white-space; no code change. |
| `refactor` | Refactoring production code (no new features or bug fixes). |
| `test` | Adding missing tests or refactoring existing tests. |
| `chore` | Updating build tasks, package manager configs, etc. |
| `ci` | Changes to CI configuration files and scripts. |

<br>

## 1. Branches

### Naming

We use the following structure for branch names to ensure context and traceability:

`<type>/<issue-number>`

* **Type:** Must match one of the allowed commit types.
* **Issue Number:** The specific issue number from our issue tracker.

<br>

**Examples:**
* `feat/1`
* `fix/2`
* `docs/3`

<br>

**Other notes**
- Branch type reflects the primary intent, commit types inside the branch can vary.

<br>

## 2. Pull Requests and Merging

### Merge strategy
The repo is set up to only allow **"squash merges"**. Therefore, make sure the "Pull Request title" follows the convention:

```<type>: <subject>. #<issue-number>```

<br>

The individual commits inside a branch will automatically show up like this in the "squashed commit":
```text
<Pull Request Title>

- Add login endpoint
- Add password hashing
- Fix typo in auth middleware
```

<br>

**Other notes**
- Feature and fix branches **should be deleted** after merge.
- If the text `Fixes #<issue-number>` is included in the PR description, then the associated issue will be auto-closed when the PR is closed.

<br>

## 3. Commits inside branches

**Guidelines**
- Are concise and readable.
- Use the imperative tense (e.g., "add" not "added" or "adding").
- Stay within 50 characters.

<br>

**Examples**
- `Add login endpoint`
- `Add password hashing`
- `Fix typo in auth middleware`