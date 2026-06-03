# Contributing to JavaSkript

JavaSkript is an open source project, and contributions are welcome. Please review these guidelines before submitting issues or pull requests.

## Behavior

Please treat others with respect. Issues where inappropriate behavior is observed may be closed or deleted. Repeated or particularly egregious behavior will result in being banned from the repository.

## Issues

Issues are used to report bugs and request improvements or features.

### Reporting Bugs

First, make sure you have the latest version available. Search the issue tracker to see if your issue has already been reported. If you can't find anything, open a new issue.

When opening an issue, use the bug report template and fill it completely. Provide the output of `/js info` as requested in the template.

## Pull Requests

### Choosing What to Work On

You can find issues tagged with "good first issue" to see if there is something for you. If you want to work on one, leave a comment so others don't pick the same issue.

If you want to work on something not in the issue tracker, consider opening an issue to discuss it first. Some changes may not fit JavaSkript's design goals.

### Getting Started

Fork the repository and clone it to your local machine:

```bash
git clone https://github.com/YOUR_USERNAME/JavaSkript.git --recurse-submodules
```

Create a new branch for your changes:

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-number
```

We recommend using an IDE such as IntelliJ. Please follow the code conventions (Google Java Format).

Build JavaSkript using Gradle:

```bash
./gradlew clean build
```

Write descriptive commit messages. We don't enforce a specific format, but they should explain what changed and why.

### Testing

Test your changes thoroughly. Run the build before submitting:

```bash
./gradlew clean build
```

Test manually on a Paper or Folia server. If you think you might have broken something, test that too.

### Submitting

When ready to submit, fill out the pull request template. Your pull request will be reviewed and merged when ready.

Good luck!

## After Submitting

Other contributors may make comments or ask questions. Please respond to these respectfully and in a timely manner.

Developers may request changes to your code or formatting. Please address these requests. Pull requests that don't address requested changes within 6 months may be closed.

Once you have made requested changes, request a re-review from the developer.

## Merging

Pull requests may be left un-merged until an appropriate time. All pull requests that are ready prior to a release will be included in that release.

For a contribution to be merged it requires at least one approving review. If your PR has been open for a week without a response, feel free to politely ask for an update.

## License

By contributing, you agree that your contributions will be licensed under the GNU General Public License v3.0.

