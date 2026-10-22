# Exercise 6
# Task: Critical Review of Matriz Project Documentation

## Goal
Perform a critical review of the existing Matriz documentation for Exercise 6 of the AI Powered Software Engineering course. Identify problems and recommend improvements, but do not automatically apply corrections.

## Context
Matriz is a proposed project and task management application for work teams within a company.

The project is currently in the requirements analysis and context preparation phase. The functional scope is provisional, and the technical architecture and implementation have not yet been defined.

## Step 1 — Read the Documentation
Read the following files before starting the review:
- `README.md`
- `AGENTS.md`
- `docs/requirements.md`
- `docs/user-stories.md`
- `docs/constraints.md`

Follow the instructions in `AGENTS.md`.

## Step 2 — Perform a Critical Review
Analyse the documentation and identify:

1. Ambiguous, incomplete or contradictory requirements.
2. Proposed features that are not sufficiently justified by the original client brief.
3. Assumptions incorrectly presented as confirmed facts.
4. Acceptance criteria that are vague, subjective or not objectively testable.
5. Security and privacy risks, including missing requirements.
6. Inconsistencies between documents, including differences in terminology, priorities, scope, requirement IDs and assumptions.
7. Missing traceability between functional requirements and user stories.

For every finding, provide:
- A unique reference ID.
- The affected file and section or requirement ID.
- A description of the issue.
- Why it matters.
- A suggested correction or clarification.
- A priority: High, Medium or Low.

Distinguish genuine contradictions from reasonable assumptions or decisions that are still pending validation.

## Step 3 — Respect the Project Context
- Do not invent new client requirements.
- Distinguish confirmed information, provisional assumptions and proposed scope decisions.
- Pay particular attention to task assignment, provisional user roles, project access rules, task statuses and project progress calculations.
- Do not assume that a numerical completion percentage is required.
- Do not treat excluded MVP features as confirmed client decisions.
- Do not choose a programming language, framework, database or architecture.
- Do not recommend implementing complex permissions before the relevant requirements have been clarified.

## Step 4 — Human-in-the-Loop Decisions
For each finding, recommend one of the following:
- ACCEPT: The proposed correction is justified and should be adopted.
- MODIFY: The recommendation needs adjustment before adoption.
- REJECT: The recommendation is unnecessary, unjustified or conflicts with the project context.

Explain the reasoning behind each recommendation. Do not assume every suggestion is correct.

## Step 5 — Report Format
Produce a structured report with these sections:

1. Executive summary.
2. Ambiguous or contradictory requirements.
3. Unjustified features.
4. Incorrectly stated assumptions.
5. Unverifiable acceptance criteria.
6. Security and privacy risks.
7. Cross-document inconsistencies.
8. Recommended decisions (Accept, Modify or Reject).
9. Prioritised list of proposed documentation changes.
10. Outstanding questions requiring client clarification.

Use tables where helpful. Reference exact file names, requirement IDs, user story IDs and acceptance criteria wherever possible.

## Constraints
- This is a read-only review.
- Do not modify, create, rename or delete any files.
- Do not run commands that change the repository.
- Do not implement features or make technical decisions.
- If a file is missing, report it rather than assuming its contents.
- Base findings on the actual repository contents, not on guesses.
- If a potential issue cannot be verified, label it as uncertain.

## Completion Criteria
The review is complete when all five documentation files have been examined, the findings are supported by specific references, and the recommendations leave final decisions to the human reviewer.
