---
name: clarify-prd
description: Turn a rough intention into an approved, codebase-grounded PRD.md.
disable-model-invocation: true
---

# Clarify PRD

Turn a rough intention into a concrete product requirements document — grounded in the part of the codebase it touches. The user explains what they want; you go read the code and report how you understand the task against what's actually there. From there it's a back-and-forth: you research, you report, the user corrects, you ask what's still ambiguous — in whatever order the conversation needs, looping until you share an accurate model and the requirements are clear. Then, **with the user's explicit approval**, you write `PRD.md`.

The work can be any size — a single change or a months-long ambition. Capture it at its true size; don't shrink it to fit one sitting. Slicing into iteration-sized pieces happens later, per iteration.

## Initial Prompt

When invoked, prompt the user:

"Explain what you want me to build or change — in as much or as little detail as you like, a single sentence or a full page. Then I'll go read the relevant code and tell you how I understand the task against what's actually there."

Wait for the answer. Let the user give as much detail as they want before you dive in.

## The clarification work

Two things have to be true before you write anything: you and the user share an accurate model of the relevant code, and the requirements are concrete and unambiguous. Getting there is not a fixed sequence — research, reporting, correction, and clarifying questions interleave however the conversation flows. Some threads need code-reading before you can ask anything useful; others get answered the moment you surface them.

As you go:

- **Research the code yourself.** Read any files, tickets, or docs the user pointed at FULLY first. Then explore the task-relevant slice on your own — WHERE the relevant things live, HOW they work, HOW they connect, the data flow.
- **Report how you understand it.** Explain to the user how you read the codebase **and how you read the task against it** — what you'd touch, what already exists, where it fits. Anchor everything to file paths so they can verify you read the real code. Use code examples if needed. Flag where your understanding is thin or uncertain — the user knows things the code doesn't say.
- **Invite correction and align.** Make it easy for the user to tell you what you got wrong or missed. Fold corrections back in and confirm the updated picture.
- **Clarify the requirements.** Resolve the ambiguities that block doing the work — as relevant: the **goal** (what should be true when done that isn't today), **user stories / user value** (who benefits and how), **acceptance criteria** (how you and a reviewer will know it works), **scope** (what's explicitly in and out), and **constraints** (where it fits, what it must not break, which existing patterns it must follow).
- **Co-design when asked.** If the user wants help deciding *how* the solution should be built — not just what — act as a co-designer: weigh approaches, surface trade-offs, and shape the design together in back-and-forth. Only when the user invites it; otherwise stay on clarifying what the requirements are.

## Asking for approval before writing

When you feel that you and the user share an accurate model of the code and the requirements are clear and unambiguous:

1. **Summarize** what you intend to capture in the PRD — the task, the codebase map, the goal, the user stories, and the acceptance criteria, in brief.
2. **Ask the user for explicit approval to write the PRD**, and tell them the path you'll write to (`tasks/YYYY-MM-DD-<slug>/PRD.md`).
3. **Wait for an affirmative answer.** Do NOT write the file until the user clearly approves. If they push back or want changes, fold them in and ask again. Only an explicit "yes / go ahead / write it" unblocks the write.

## Writing the PRD

Once approved, create the task directory and write `PRD.md` inside it:

- `mkdir -p tasks/YYYY-MM-DD-<slug>` — today's date plus a short kebab-case summary (e.g. `tasks/2026-06-06-add-auth-middleware`).
- Write `PRD.md` into that directory using the template below, then report the full path.

Write only what the session established — don't re-investigate or re-interview, and don't invent or pad. Fill only small gaps by reading code if needed.

<prd-template>

# PRD: <short title>

## Problem

What problem this solves and why it matters now, from the user's perspective.

## Relevant Codebase

A map of the slice this work touches — what exists today, so the requirements are grounded in reality.
- **What's there**: the components/files involved and their responsibility (`path/to/file.ext:123`).
- **How it works**: trace the data/control flow through the area.
- **Patterns to follow**: conventions the change should respect, anchored to examples.
- **Integration points**: what this connects to — callers, callees, shared contracts.

## Goal

What should be true when complete that isn't true today, from the user's perspective.

## User Stories

The user-facing value as concrete stories — who benefits and how. "As a <role>, I want <capability> so that <outcome>."

## Acceptance Criteria

A numbered list of concrete, checkable conditions that define "done" for the whole task. Each observable — demoable or testable, not a vague aspiration.

## Scope

### In scope
What this work covers.

### Out of scope
What it explicitly does NOT cover — including tangents deferred to future work.

## Risks

The places most likely to bite during implementation — tricky areas, fragile integration points, things to watch out for. (Open questions should already be resolved during clarification; if any genuinely remain, note them here.)

</prd-template>

## Guidelines

- Document what EXISTS and how you read the task against it — no suggestions or critiques of the design (only if the user asks). Your job is to understand, clarify, and capture, not to judge.
- Be specific: use file paths and line numbers. Confident-but-wrong is the most expensive failure in brownfield work — flag uncertainty as an open question, never paper over it.
- Stay focused — note tangents as future work rather than absorbing them. Focused means on-task, not small; the work is whatever size it truly is.
- Never write the PRD without explicit user approval. The approval gate is mandatory.
- A destination, not a plan — no implementation steps. Acceptance criteria describe the finished work, however many iterations that takes.
- Don't plan or implement — the goal is to clarify and capture the requirements, not to build them.
- The year is 2026 and AI coding agents exist, so treat coding, refactoring, testing, and rewriting as cheap; do not let the historical cost of hand-written software constrain the size, scope, or ambition of the solution, and instead optimize for the best final system: clean, correct, maintainable, well-tested, and shaped by the real constraints of the task rather than old assumptions about implementation effort.
