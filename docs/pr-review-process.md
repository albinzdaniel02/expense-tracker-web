# Pull Request Review Process

To maintain high code quality and prevent self-review bias, all code changes for the Expense Tracker application must follow a strict, independent review workflow.

## Guidelines

1. **Role Separation**: The agent or subagent that writes the code and implements a task MUST NOT review the Pull Request (PR) itself. The review must be performed by a separate, independent subagent (e.g. `pr_reviewer`).
2. **Review & Wait**: Once the implementing agent pushes the branch and creates a PR, it must spawn the `pr_reviewer` subagent and wait for the reviewer to inspect the changes and post its findings on the PR.
3. **Review Loop**:
   - The reviewer agent inspects the changes against requirements, and posts comments to the PR (`gh pr comment` or `gh pr review`).
   - If there are issues/feedback, the implementing agent must fix the implementation, commit, push, and ask the reviewer to re-review.
   - This loop must be followed continuously until the reviewer confirms that **no issues exist** and the PR fully matches specifications.
4. **Merge**: Once the implementing agent confirms that the reviewer has approved the PR (no issues remain), the **implementing agent** is responsible for merging the PR into `main` (`gh pr merge --squash --delete-branch`).
