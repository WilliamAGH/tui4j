---
title: "Code change policy contract"
usage: "Use whenever creating/modifying files: where to put code, when to create new types, and how to stay SRP compliant"
description: "Evergreen contract for change decisions (new file vs edit), repository structure, and library architecture; references rule IDs in `AGENTS.md`"
---

# Code Change Policy Contract

See `AGENTS.md` ([LOC1a-e], [MO1a-g], [FS1a-g], [AB1a-d]).

## Non-negotiables (applies to every change)

- **Public API Stability**: 100% backward compatibility required. Never remove/rename public methods.
- **SRP only**: each new type/method has one reason to change ([MO1d]).
- **New feature → new file**; do not grow monoliths ([MO1b]).
- **No edits to >500 LOC files**; first split/retrofit ([LOC1c]).
- **No Dependencies**: Library logic should minimize external deps.

## Decision matrix: create new file vs edit existing

Use this as a hard rule, not a suggestion.

| Situation | MUST do | MUST NOT do |
|----------|---------|-------------|
| New library feature (new bubble, new utility) | Add a new, narrowly scoped type in the correct package ([MO1b]) | “Just add a method” to an unrelated class ([MO1a], [MO1d]) |
| Bug fix (existing behavior wrong) | Edit the smallest correct owner; add/adjust tests to lock behavior | Create a parallel/shadow implementation |
| Logic change in stable code | Extract/replace via composition; keep stable code stable ([MO1g]) | Add flags, shims, or “compat” paths to hide uncertainty |
| Touching a large/overloaded file | Extract at least one seam (new type + typed contract) ([FS1f], [MO1b]) | Grow the file further ([MO1a]) |

### When adding a method is allowed

Adding to an existing type is allowed only when all are true:

- It is the **same responsibility** as the type’s existing purpose ([MO1d]).
- It does not break binary compatibility.
- It does not pull in a new dependency direction.

If any bullet fails, create a new type.

## Create-new-type checklist (before you write code)

1. **Search/reuse first**: confirm a type/pattern doesn’t already exist ([FS1a]).
2. **Pick the correct package** (match upstream Go structure if porting).
3. **Name by role** (ban generic names; suffix declares meaning) ([FS1c]).
4. **Keep the file small** (stay comfortably under 500 LOC; split by concept early) ([LOC1a]).
5. **Add/adjust tests** using existing patterns/utilities.
6. **Verify** with repo-standard commands (`./gradlew build`).

## Repository structure and naming (placement is part of the contract)

### Package structure

- `com.williamcallahan.tui4j.compat.**`: Ports of upstream Go libraries.
- `com.williamcallahan.tui4j.**`: Native extensions.

### Naming conventions

- Classes: `PascalCase`.
- Interfaces: `PascalCase`.
- Methods: `camelCase`.

## Verification gates (do not skip)

- LOC enforcement: manual check / script ([LOC1c]).
- Build/test: `./gradlew build`.
