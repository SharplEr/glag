---
name: java-project-style
description: Apply the project's Java coding conventions and design principles when editing or generating Java code. Use for implementation, refactoring, tests, API design, performance-sensitive code, and code review in this repository.
---

# Java project style guide

Follow these rules whenever you write or modify Java code in this project.

## Main goal

Optimize for three things simultaneously:

1. readability,
2. performance,
3. reliability.

Do not prefer OOP, FP, inheritance, patterns, or abstractions for their own sake. Prefer whatever makes the code easier to understand, test, compose, and modify while preserving speed and correctness.

## Language level

Assume Java 25 language features are available and should be used when they improve clarity.

Prefer modern Java features, including:

- `var`
- records
- pattern matching
- switch expressions
- sealed types when they simplify closed hierarchies

Use modern syntax by default unless there is a strong reason not to.

## Local variable style

Prefer `var` almost everywhere for local variables, even for primitive types like `int`, for consistency.

Examples:

```java
var count = 0;
var name = "Alice";
var words = new ArrayList<String>();
var ids = new IntArrayList();
```

When constructing generic collections with `var`, put generic arguments on the right-hand side:

```java
var words = new ArrayList<String>();
var map = new HashMap<String, Integer>();
```

Do not use diamond on the right-hand side when `var` would otherwise erase useful type information:

```java
var words = new ArrayList<String>();   // preferred
var words = new ArrayList<>();         // avoid when it weakens type information
```

Exception: if a variable is assigned in different branches and there is no initializer from which the type can be inferred clearly, write the type explicitly.

Example:

```java
List<String> result;
if (flag) {
  result = List.of("a");
} else {
  result = List.of("b");
}
```

Use `var` consistently in resource variables inside `try`-with-resources as well:

```java
try (var stream = Files.lines(path)) {
  ...
}
```

## Data modeling

Prefer `record` for plain immutable data carriers, small result objects, parsed values, and structured return types, even if this exposes state a bit more than classic encapsulation would.

Use records aggressively when they reduce boilerplate and make data flow obvious.

Prefer small focused types over ambiguous tuples, raw maps, or loosely structured objects.

When an invariant matters, try to encode it in the type design first, not only in comments.

## Nullability

Use JSpecify nullness consistently.

At package level, default to:

```java
@NullMarked
package com.example.foo;

import org.jspecify.annotations.NullMarked;
```

Assume non-null by default everywhere.

Use `@Nullable` only where null is genuinely part of the contract and cannot be modeled better another way.

Prefer making nullability explicit in APIs rather than relying on conventions or documentation.

When designing or changing APIs:

* minimize nullable inputs and outputs,
* keep nullable scope narrow,
* propagate nullability intentionally,
* avoid using `null` as a multi-purpose sentinel when a dedicated type or state is clearer.

## Mutability conventions

Be careful and explicit about mutability.

Project convention:

* variables, fields, parameters, and return types declared as `List`, `Set`, or `Collection` are considered immutable by default,
* when mutability is intended and relevant, expose a concrete mutable type such as `ArrayList`, `HashSet`, `LinkedHashMap`, etc.

Examples:

```java
List<String> names = List.of("a", "b");          // immutable intent
ArrayList<String> names = new ArrayList<String>(); // mutable intent
```

Do not hide mutability behind abstract collection interfaces unless there is a strong reason.

When receiving a mutable collection, be disciplined about ownership:

* mutate in place only when that is the explicit contract,
* otherwise copy defensively or return a new value.

Prefer final-by-behavior code even if Java does not enforce full immutability.

## Primitive performance and boxing

Avoid boxing unless there is a clear benefit that outweighs the cost.

Be especially careful in hot paths, tight loops, parsers, indexing code, numeric code, and frequently allocated data structures.

Use primitive-specialized structures from fastutil for primitive collections and maps.

Examples:

* `IntArrayList` instead of `ArrayList<Integer>`
* `IntOpenHashSet` instead of `HashSet<Integer>`
* `Int2ObjectOpenHashMap<V>` instead of `HashMap<Integer, V>`

Do not introduce wrapper types accidentally through streams, collectors, lambdas, or overly generic helper APIs.

Prefer straightforward loops over abstraction-heavy code when it avoids boxing, allocations, or hidden overhead.

## Invariants and validation

Use `assert` for invariant checks in hot code, internal assumptions, and conditions that should hold in correct program states.

Use explicit runtime validation with clear exceptions in cold code, especially when:

* parsing input,
* validating external data,
* processing config,
* handling user-provided values,
* handling I/O boundaries.

Rule of thumb:

* hot/internal code -> `assert`
* cold/boundary/parsing code -> explicit checks and informative failures

Do not replace important input validation with `assert`, because assertions may be disabled.

## API design

Design APIs so invalid states are hard or impossible to express.

Prefer:

* narrow, intention-revealing parameter types,
* dedicated domain types,
* small records for grouped arguments,
* explicit return models

Avoid overly generic APIs that make misuse easy.

Prefer compile-time guidance over comments when practical.

When choosing between flexibility and safety, prefer the design that preserves composability without making misuse cheap.

## Abstractions

Keep hierarchies flat.

Preferred order:

1. record
2. final class
3. interface + record/class implementations
4. abstract class only when it materially simplifies the code

Avoid class inheritance unless it is forced by a library or gives a substantial concrete benefit.

If you need polymorphism, usually prefer an `interface` and small implementations. Records are preferred over classes where applicable.

Do not create deep inheritance trees.

Avoid inner classes unless they are genuinely the clearest representation of tightly coupled local behavior. Do not default to private nested helper classes for one-off logic when a private method, local record, or a small top-level/package-private type is flatter and easier to read.

Do not introduce abstractions just to satisfy a pattern. Introduce them when they remove duplication, clarify a business concept, or protect correctness/performance.

## State and mutation

Prefer data flowing through method parameters and return values over hidden object state.

If a helper conceptually computes a collection or lookup structure, prefer returning it from the helper rather than mutating a field as an implicit side effect.

Avoid spreading one logical computation across multiple methods by mutating shared fields unless that shared state is intrinsic to the object’s long-lived responsibility.

Keep mutation narrow, explicit, and local. If state is only needed within one processing pass, model it as a local variable or an explicit returned value.

## Business logic and composition

Avoid duplicating business logic across code paths.

When two code paths differ only in small details, try to factor the shared logic into a single composable implementation and inject the varying behavior explicitly.

Prefer composable pieces over ad-hoc special cases.

Introduce an extra abstraction layer only when it cleanly separates:

* a concrete low-level mechanism
  from
* a higher-level business concept

Do not build accidental mini-DSLs or overengineered abstraction towers that make simple code harder to follow.

## Overloading

Use overloads sparingly.

Overloads are acceptable as a small interoperability convenience, especially when adapting awkward third-party APIs.

Do not use overloads when methods do meaningfully different things. In that case, give them different names.

Be careful with overloads involving lambdas, method references, and generic functional interfaces, because they can hurt readability and type inference.

## Control flow and readability

Prefer code that is easy to scan.

* prefer guard clauses over deeply nested conditionals,
* keep methods focused,
* avoid hidden side effects,
* make important invariants and boundary checks easy to spot.

Prefer the simplest standard library construct that expresses the intent clearly.

Do not reach for a more obscure stream operator or abstraction just to avoid one obvious intermediate concept such as `null` filtering, especially on cold code. For example, prefer `map(...).filter(Objects::nonNull)` over `mapMulti(...)` when the latter does not materially improve the code.

## Testing strategy

Tests are not written to maximize JaCoCo mechanically. Tests are written to make it hard to find a counterexample for the code.

Use JaCoCo as a hint about which cases are not exercised yet, not as the source of truth for what tests should exist.

### What JaCoCo is good for

JaCoCo is useful for:

* noticing missing boundary cases,
* finding branches that were never exercised,
* seeing which public parsing or aggregation paths have no tests yet.

JaCoCo is not the goal. It may disagree with the real testing goal when some code is intentionally unreachable or only exists to strengthen invariants.

### Intentionally uncovered code

Some code should not be forced to 100% coverage:

* `assert`-based invariant checks,
* static startup checks over statically known values,
* enum validation and similar “compile-time checks implemented at runtime”.

Examples in this project:

* [`SafepointValueType`](../../../../src/main/java/org/sharpler/glag/parsing/SafepointValueType.java) has static validation of enum prefixes. This protects the index design. It is not useful to write tests that try to make this startup check fail by bending the language or using custom classloading tricks.
* `assert` checks in hot code are there to catch internal inconsistencies earlier during tests. If JaCoCo does not count the failing branch of an `assert`, that is JaCoCo's limitation, not a testing task.

Do not write tests whose only purpose is to make an `assert` fail or to trigger static validation of known source constants. Those are not meaningful behavioral tests.

### Refactor code when tests are hard to write

If a test is awkward to write, first ask whether the production code is shaped correctly.

Typical good sequence:

1. test is hard to write,
2. refactor code so invalid states become harder or impossible to express,
3. test becomes simple,
4. write the test,
5. if the test finds a real bug, fix the code.

Prefer making invalid states impossible over testing many impossible states.

If an invariant can be established at the parsing boundary or by construction, do that there. After that:

* boundary code should use explicit runtime validation,
* downstream internal code should usually rely on `assert`.

This keeps runtime validation close to user data and makes the deeper code easier to reason about and easier to test.

Example in this project:

* [`SafepointRecordBuilder`](../../../../src/main/java/org/sharpler/glag/parsing/SafepointRecordBuilder.java) is boundary code that assembles parsed data and performs runtime validation.
* [`SafepointLogRecord`](../../../../src/main/java/org/sharpler/glag/records/SafepointLogRecord.java) uses `assert` for internal invariants after the builder has established validity.

Tests should focus on the builder’s reachable invalid usage, not on creating impossible internal states inside the record.

### Reflection in tests is a red flag

Do not use reflection to inject invalid state into objects unless the user explicitly asks for white-box tests of an otherwise unreachable state.

Reflection-based tests usually mean one of two things:

* the test is trying to cover impossible states,
* the production code should be refactored so the behavior can be exercised through its real API.

For this project, reflection in tests should be treated as a strong smell. Prefer deleting such tests and testing reachable states instead.

### Property-based testing guidance

Prefer property-based tests for:

* parsers,
* data transformations,
* aggregations,
* invariants,
* indexing and matching logic.

However, do not write “two copies of the same algorithm” and compare them. That is not useful unless one side is a trusted reference implementation.

Bad pattern:

* production code computes result,
* test re-implements the same logic in another method,
* property asserts both results are equal.

This usually just makes two similar bugs agree with each other.

Better options:

* assert structural invariants of the result,
* generate inputs with known properties,
* split one vague property into several focused properties over different input families.

Examples in this project:

* [`CumulativeDistributionBuilderTest`](../../../../src/test/java/org/sharpler/glag/distribution/CumulativeDistributionBuilderTest.java) checks invariants of the produced distribution:
  * every point must come from the input,
  * points appear only for a reason,
  * rounded-equal neighbors must collapse,
  * the last point must represent the maximum with probability `1.0`.
  This is much better than duplicating the builder logic in a “reference” helper.
* [`SafepointRecordBuilderTest`](../../../../src/test/java/org/sharpler/glag/parsing/SafepointRecordBuilderTest.java) generates valid combinations of optional times and checks the constructed record. This is a good pattern for builder-like logic with many combinations.

### Generate inputs with known properties

When a direct oracle is hard to write, design generators so the expected behavior is obvious from construction.

Preferred approach:

* write several smaller properties,
* each property uses a generator that guarantees a particular shape,
* assert only the consequences of that shape.

Examples:

* for interval matching, generate definitely overlapping and definitely disjoint intervals in separate properties;
* for duration formatting, generate separate ranges for `ns`, `µs`, `ms`, and `s`;
* for parser resolution, generate lines where the type must be known, must be absent, or must prefer a more precise decorator.

This is usually better than one giant randomized property with many conditionals.

### Reachable invalid cases only

When writing negative tests, test only invalid states that are reachable through the intended API.

Good negative tests:

* missing required builder steps,
* malformed input lines,
* impossible timestamps coming from user-controlled parsing input,
* inconsistent boundary data.

Bad negative tests:

* corrupting private fields,
* bypassing construction rules,
* forcing enum startup checks to fail,
* making internal `assert` branches fail only to satisfy coverage tooling.

If a negative test feels artificial, step back and ask whether it is actually testing a user-visible failure mode.
* prefer explicit names over cleverness,
* prefer direct code over abstraction layers when the abstraction does not buy much.

Use pattern matching and modern `switch` forms where they make branching clearer.

Do not add defensive `null` handling unless `null` is actually permitted by the API contract or is realistically produced by the code path in question. Prefer trusting non-null contracts and keeping branches out of the code when they encode impossible states.

## Multiline formatting

When a method call, constructor call, or similar argument list is formatted across multiple lines, put each argument on its own line.

Prefer this shape:

```java
var result =
    someCall(
        firstArgument,
        secondArgument,
        thirdArgument
    );
```

Do not keep several arguments on the same continuation line once the call has already been split across lines.

Keep the closing `)` on its own line aligned with the start of the call continuation, and keep chained wrapping visually flat and easy to scan.

## Testing

Prefer property-based testing using jqwik for logic-heavy code, parsers, transformations, invariants, algebraic behavior, and edge-case discovery.

Use JUnit Jupiter for assertions and regular test structure.

When writing tests:

* favor properties over many repetitive examples when appropriate,
* still include focused example-based tests for important scenarios and regressions,
* test invariants, round-trips, boundaries, and degenerate cases,
* test nullness, mutation boundaries, and failure modes where relevant.

A good test suite should make refactoring safer, not just increase line count.

## Performance mindset

Treat performance as a first-class design constraint, but do not sacrifice clarity without evidence.

Prefer designs that are both simple and fast.

Watch for:

* boxing,
* accidental allocations,
* hidden copies,
* stream overhead in hot paths,
* virtual dispatch in critical inner loops,
* unnecessary temporary objects,
* overly generic APIs in performance-sensitive code.

Use simple imperative code when it is clearer and cheaper.

## Refactoring guidance

When refactoring:

* preserve invariants,
* reduce ceremony where possible,
* improve types so they communicate more,
* remove duplication in business logic,
* simplify hierarchies,
* keep abstractions composable,
* do not introduce unnecessary framework-like structure.

Prefer refactorings that improve both correctness and maintainability without regressing performance.

## When uncertain

If several designs are possible, prefer the one that:

1. makes invalid usage harder,
2. keeps runtime costs visible,
3. keeps the code flatter and easier to read,
4. preserves composability,
5. is easier to test.
