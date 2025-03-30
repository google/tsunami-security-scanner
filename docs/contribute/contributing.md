# How to Contribute

We'd love to accept your patches and contributions to this project. There are
just a few small guidelines you need to follow.

## Contributor License Agreement

Contributions to this project must be accompanied by a Contributor License
Agreement. You (or your employer) retain the copyright to your contribution;
this simply gives us permission to use and redistribute your contributions as
part of the project. Head over to <https://cla.developers.google.com/> to see
your current agreements on file or to sign a new one.

You generally only need to submit a CLA once, so if you've already submitted one
(even if it was for a different project), you probably don't need to do it
again.

## Code reviews

All submissions, including submissions by project members, require review. We
use GitHub pull requests for this purpose. Consult
[GitHub Help](https://help.github.com/articles/about-pull-requests/) for more
information on using pull requests.

## Community Guidelines

This project follows
[Google's Open Source Community Guidelines](https://opensource.google/conduct/).

## Reviewing and Merging Pull Requests

To effectively review and merge pull requests, follow these steps:

1. **Understand the context**: Familiarize yourself with the purpose of the repository and the specific area of the codebase that the pull request affects. For example, the `README.md` provides an overview of the project, and the `docs/contribute/contributing.md` outlines the contribution guidelines.

2. **Check for adherence to guidelines**: Ensure that the pull request follows the contribution guidelines mentioned in `docs/contribute/contributing.md`. This includes having a clear description, following the code style, and including necessary documentation.

3. **Review the code changes**: Examine the code changes in the pull request. Look for potential issues such as code quality, readability, and maintainability. For example, the `common/src/main/java/com/google/tsunami/common/cli/CliOption.java` file should be reviewed for proper implementation of command-line options.

4. **Verify tests**: Ensure that the pull request includes appropriate tests for the changes. Check the test files, such as `common/src/test/java/com/google/tsunami/common/cli/CliOptionsModuleTest.java`, to verify that the new code is adequately tested.

5. **Run the tests**: Execute the tests to ensure that the changes do not introduce any regressions. This includes running both unit tests and integration tests.

6. **Check for documentation updates**: Ensure that any necessary documentation updates are included in the pull request. For example, if the pull request affects the usage of the tool, the `docs/howto/new-detector.md` file should be updated accordingly.

7. **Provide feedback**: If there are any issues or improvements needed, provide clear and constructive feedback to the contributor. Be polite and specific about the changes required.

8. **Approve and merge**: Once the pull request meets all the requirements and passes the tests, approve the changes and merge the pull request. Ensure that the commit message is clear and descriptive.
