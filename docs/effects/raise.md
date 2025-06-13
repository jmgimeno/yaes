---
layout: default
title: "Raise Effect"
---

# Raise Effect

The `Raise[E]` effect describes the possibility that a function can raise an error of type `E`. It provides typed error handling inspired by the [`raise4s`](https://github.com/rcardin/raise4s) library.

## Overview

The `Raise` effect allows you to define functions that can fail with specific error types, providing a functional approach to error handling without exceptions.

## Basic Usage

### With Exception Types

```scala
import in.rcard.yaes.Raise.*

def divide(a: Int, b: Int)(using Raise[ArithmeticException]): Int =
  if (b == 0) Raise.raise(new ArithmeticException("Division by zero"))
  else a / b
```

### With Custom Error Types

```scala
import in.rcard.yaes.Raise.*

object DivisionByZero
type DivisionByZero = DivisionByZero.type

def divide(a: Int, b: Int)(using Raise[DivisionByZero]): Int =
  if (b == 0) Raise.raise(DivisionByZero)
  else a / b
```

## Utility Functions

### Ensuring Conditions

```scala
import in.rcard.yaes.Raise.*

def divide(a: Int, b: Int)(using Raise[DivisionByZero]): Int = {
  Raise.ensure(b != 0) { DivisionByZero }
  a / b
}
```

### Catching Exceptions

Transform exceptions into typed errors:

```scala
import in.rcard.yaes.Raise.*

def divide(a: Int, b: Int)(using Raise[DivisionByZero]): Int =
  Raise.catching[ArithmeticException] {
    a / b
  } { _ => DivisionByZero }
```

## Handlers

### Union Type Handler

Handle errors as union types:

```scala
import in.rcard.yaes.Raise.*

val result: Int | DivisionByZero = Raise.run {
  divide(10, 0)
}
```

### Either Handler

Transform errors into `Either` types:

```scala
import in.rcard.yaes.Raise.*

val result: Either[DivisionByZero, Int] = Raise.either {
  divide(10, 0)
}
```

### Option Handler

Ignore error details and get `Option`:

```scala
import in.rcard.yaes.Raise.*

val result: Option[Int] = Raise.option {
  divide(10, 0)
}
```

### Nullable Handler

Get nullable results:

```scala
import in.rcard.yaes.Raise.*

val result: Int | Null = Raise.nullable {
  divide(10, 0)
}
```

## Error Composition

Combine multiple error types:

```scala
import in.rcard.yaes.Raise.*

sealed trait ValidationError
case object InvalidEmail extends ValidationError
case object InvalidAge extends ValidationError

def validateUser(email: String, age: Int)(using Raise[ValidationError]): User = {
  val validEmail = if (email.contains("@")) email 
                   else Raise.raise(InvalidEmail)
  val validAge = if (age >= 0) age 
                 else Raise.raise(InvalidAge)
  User(validEmail, validAge)
}
```

## Best Practices

- Use specific error types rather than generic exceptions
- Combine with other effects like `IO` for comprehensive error handling
- Handle errors at appropriate boundaries in your application
- Use union types for simple error handling, `Either` for more complex scenarios
