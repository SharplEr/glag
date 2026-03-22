# AGENTS.md

## Purpose

Write Java code that is clear, fast, reliable, and easy to modify.
Do not prefer OOP, FP, inheritance, or patterns for their own sake.
Prefer the simplest design that preserves correctness, composability, and performance.

## Language and syntax

- Assume Java 25 features are available and use modern language features when they improve clarity.
- Prefer `var` for local variables almost everywhere, including primitives, for consistency.
- When using `var` with generic collections, keep generic parameters on the right side, for example: `var words = new ArrayList<String>()`.
- Use an explicit type only when the type cannot be inferred clearly, for example for variables assigned in different branches.
- Prefer pattern matching, switch expressions, records, and other modern Java constructs where they simplify the code.

## Data modeling and abstractions

- Prefer `record` for immutable data carriers and small structured return values, even if it slightly weakens encapsulation.
- Keep inheritance hierarchies flat.
- Prefer `record`, then `final class`, then `interface` with small implementations.
- Use abstract classes only when they materially simplify the code.
- Avoid class inheritance unless it is required by a library or provides a substantial concrete benefit.
- Prefer interfaces plus small record/class implementations over deep hierarchies.

## Mutability and nullability

- Be explicit and careful about mutability.
- By project convention, `List`, `Set`, and `Collection` mean immutable intent unless stated otherwise.
- If mutability matters, use a concrete mutable type such as `ArrayList` or `HashSet`.
- Use JSpecify nullness consistently.
- Mark packages with `org.jspecify.annotations.NullMarked`.
- Use `org.jspecify.annotations.Nullable` only where null is truly part of the contract.

## Performance

- Avoid boxing in performance-sensitive code.
- Prefer primitive-specialized collections from fastutil for primitives.
- Prefer simple loops and straightforward code over abstraction-heavy constructs when that avoids allocations, boxing, or hidden overhead.
- Keep runtime costs visible.

## Validation and invariants

- Use `assert` for invariants and internal assumptions in hot code.
- Use explicit runtime validation with clear failures in cold code, especially when parsing external input or validating configuration.
- Do not rely on `assert` for user-facing input validation.

## API and business logic

- Prefer APIs that make invalid states hard to express.
- Prefer dedicated small types over weakly typed argument lists, maps, or nullable sentinel values.
- Avoid duplicating business logic across code paths.
- Factor shared logic into composable units when different code paths vary only in small details.
- Prefer explicit, intention-revealing code over clever abstractions.

## Testing

- Prefer property-based testing with jqwik for logic-heavy code, transformations, parsers, and invariants.
- Use JUnit Jupiter for assertions and regular test structure.
- Write tests that improve confidence for refactoring, not just line coverage.

## When choosing between designs

Prefer the design that:
1. makes misuse harder,
2. keeps code flatter and easier to read,
3. keeps performance characteristics obvious,
4. improves testability,
5. remains easy to compose and modify.