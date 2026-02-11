# Android Team Conventions

This document outlines the specific conventions and workflows adopted by the Android subteam. These rules are in addition to the general repository conventions.

## PR Workflow

Every team member is responsible for understanding and owning the code they write and the code they approve.

### Opening PRs

#### 1. One issue at a time

Work on one issue at a time. Do not claim or begin work on a new issue until your current PR is ready for review (see below).

#### 2. Understand your own code

Before requesting a review, you must be able to:

- Explain what the component does and how it fits into the wider system
- Describe what it interacts with and what the expected behaviour is
- Confirm that the implementation matches expected behaviour, and that if something goes wrong, you know where in the codebase to look for the bug

AI tools for generating code are encouraged, but you are still responsible for understanding the implementation. AI-generated code is a starting point, not a finished product.

#### 3. PRs must remain in draft until ready

Every PR must be created in draft mode. It should not be marked as ready for review until all of the following are true:

- All automated checks (CI, linting, tests) pass
- No outstanding Copilot review comments (run until none are produced)
- You have read and understood every new component
- You have validated that the implementation matches expected behaviour
- There are no `TODO` comments, incomplete implementations or stub components

#### 4. Only then: request review and move on

Once you are confident the PR is complete and you can defend the implementation, mark it as ready for review and request a review from your subteam partner. Only at this point may you move on to the next issue.

### Reviewing PRs

When you are asked to review someone else's PR, you are the last line of defence before that code enters the codebase. This is a responsibility, not a formality.

#### 1. Read the code yourself

You must read the diff and understand what the PR does before approving it. Do not delegate your review to an AI tool — asking an assistant to review the code and then merging on that basis is not a review.

#### 2. Check for correctness, not just style

A review should verify that:

- The implementation matches the linked issue's requirements
- The logic is sound and handles edge cases appropriately
- The code integrates correctly with existing components
- Tests are meaningful and actually validate the described behaviour

#### 3. Raise concerns, don't rubber-stamp

If something is unclear or looks wrong, comment on it. If you don't understand part of the implementation, ask the author to explain. Approving code you haven't read places an unfair burden on the author to be flawless in their implementation and removes the safety net that code review is supposed to provide.

### Prerequisites for Merging

A PR may only be merged once all of the following are satisfied:

- The branch is up to date with the target branch it is being merged into
- At least one approval from a subteam member
- All automated checks pass (CI, tests, linting)
- All review comments have been addressed or resolved
- No merge conflicts


## Communication

GitHub and WhatsApp are our primary communication channels. It is essential that every team member is genuinely reading and engaging with what others write.

### Human-written content only

All communication on GitHub and WhatsApp — issue comments, PR descriptions, review comments, discussion replies, and chat messages — must be written by you, in your own words. Do not let an AI assistant draft or post responses on your behalf.

Using AI to help you think through a problem or understand a concept is fine. But when you reply to a teammate, the words need to be yours. If a comment is obviously AI-generated, it will be treated as such — that is, as if no response was given.

### Why this matters

Our GitHub threads and WhatsApp chats are how we stay aligned on design decisions, surface problems, and coordinate work outside of in person meetings. If someone posts an AI-generated reply without reading the original message, the conversation and shared context breaks down and decisions get made without consensus.
