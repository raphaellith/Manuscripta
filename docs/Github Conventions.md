This issue outlines the basic conventions to be adopted within this GitHub repository.

## Subteams

Our team is currently divided into two subteams:

- Nemo and Raphael working on Windows app development; and
- Will and Priya working on Android app development.

In general, the former subteam is responsible for code within the ```windows``` directory, whereas the latter subteam is responsible for code within the ```android``` directory.

In addition to the four team members' personal accounts, this repository also includes the collaborator @manuscripta0. This is a shared Gmail account, created predominantly to effectively utilise Git on the loaned Windows laptop.


## Branches

This repository uses the following long-lived branches:

- At any given time, the ```main``` branch must contain stable, bug-free and working versions of both the Windows and Android applications.
- The ```windows-app``` and ```android/dev``` branches serve as integration branches for Windows and Android app development respectively.

Create a new branch for each feature. For Windows app features, prefix your branch with ```windows/```. For Android app features, prefix your branch with ```android/```.

When that feature is deemed complete, write a pull request (see below). When it is approved, it should be merged into either ```windows-app``` or ```android/dev```.

After a number of features have been merged into ```windows-app``` and/or ```android/dev```, integration tests should be carried out to ensure their interoperability. Another pull request may be then created to merge these features into ```main```.

Additional branches may be created for other uses, such as documentation and experimentation.


## Issues and pull requests (PRs)

Each issue or pull request, if applicable, should be marked with either an ```android``` or ```windows``` label. (Alternatively, an issue or pull request may have a title beginning with either "[Android]" or "[Windows]".) Other labels may also be added.

A pull request should only be merged after it is approved by the other member of your subteam. Each subteam may request PR reviews from the other subteam should they deem the PR to be of considerable significance.

Issues can only be closed once their respective PRs have been merged either into our dev branches or main.


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

## Documentation

All forms of system-wide documentation should be stored as Markdown files in the ```docs``` directory. Any change to this directory's contents must be approved by all team members via a PR.

Each Markdown documentation file must be clearly marked with a unique version number, e.g. ```V1```. When necessary, such as at the end of an iteration cycle, these files may be replaced by a newer version with an incremented version number.


### API contract

The API contract provides a list of API endpoints and communication protocols to be used between the Windows and Android specifications. 

When writing code, issues or PRs, any part of the contract may be referred by its associated (sub)subheading *and* the API contract version number.


### Project specification

The project specification outlines the overview, goals, and functional requirements of the Manuscripta project.

Each functional requirement in the project specification is associated with a unique alphanumeric code ```SECx```, where ```SEC``` is a three-letter prefix denoting the requirement's category, and ```x``` is a positive integer. Requirements in the same section are given the same prefix and ordered in ascending order of ```x```.

When writing code, issues or PRs, any functional requirement may be referred to by its code *and* the specification version number.

### Editing the project specification

Note the following conventions for adding, removing, and modifying requirements.

1.  **To add a new requirement:** Add the requirement using a new code at the end of the section, or use an alphanumeric code to maintain clause relationships (e.g., REQ5A after REQ5).
2.  **To remove a requirement:**
    * If the requirement is obsolete and will not be replaced, replace the contents with `[REMOVED]`.
    * If the requirement has been merged or updated (see Convention 3), replace the contents with `[REMOVED] see {new_requirement_code}`.
    * **Do not** delete the requirement code itself.
3.  **To update or modify a requirement:** Following conventions 1 and 2, add the updated version as a new requirement (with a new code). Then, replace the contents of the outdated requirement with the pointer string pointing to that new code. **Never directly edit the contents of a requirement.**

---

### **Examples of how this looks in practice**

#### **Example 1: Updating a requirement (end of section)**

To update requirement **REQ005** into a new version **REQ022** added at the end:

> **REQ005:** `[REMOVED] see REQ022`
>
> *(... intervening requirements ...)*
>
> **REQ022:** The system must allow users to export logs in CSV and JSON formats.

#### **Example 2: Replacing a requirement (inline)**

To replace **REQ005** with an improved version immediately following it:

> **REQ005:** `[REMOVED] see REQ005A`
> 
> **REQ005A:** The system must allow users to export logs in CSV and JSON formats.
>
> **REQ006:** ...

#### **Example 3: Inserting a requirement (inline)**

To add a new requirement after **REQ003**:

> **REQ003:** The system must authenticate users via OAuth 2.0.
> 
> **REQ003A:** The system must support multi-factor authentication as an optional security layer.
>
> **REQ004:** ...