---
layout: default
title: "YÆS - Yet Another Effect System"
---

# Yet Another Effect System (yæs)

![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/rcardin/yaes/scala.yml?branch=main)
![Maven Central](https://img.shields.io/maven-central/v/in.rcard.yaes/yaes-core_3)
![GitHub release](https://img.shields.io/github/v/release/rcardin/yaes)
[![javadoc](https://javadoc.io/badge2/in.rcard.yaes/yaes-core_3/javadoc.svg)](https://javadoc.io/doc/in.rcard.yaes/yaes-core_3)

YÆS is an experimental effect system in Scala inspired by the ideas behind Algebraic Effects. Using Scala 3 [context parameters](https://docs.scala-lang.org/scala3/reference/contextual/using-clauses.html) and [context functions](https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html), it provides a way to define and handle effects in a modular and composable manner.

## 🎥 Featured Talk

Watch the talk from **Scalar 2025** about the main concepts behind the library:

[![Watch the video](https://img.youtube.com/vi/TXUxCsPpZp0/maxresdefault.jpg)](https://youtu.be/TXUxCsPpZp0)

## 📦 Available Modules

- **`yaes-core`**: The main effects of the YÆS library
- **`yaes-data`**: Functional data structures that complement the YÆS effects system

### YÆS Core
The core module provides a comprehensive set of effects for functional programming:
- IO operations and side effect management
- Structured concurrency with async computations
- Typed error handling and resource management  
- Console I/O, logging, and system integration

### YÆS Data
The data module provides functional data structures optimized for use with effects:
- **Flow**: Cold asynchronous data streams with rich transformation operators
- Future additions: Immutable collections, persistent data structures

## 🚀 Quick Start

Add the dependencies to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "in.rcard.yaes" %% "yaes-core" % "0.2.0",
  "in.rcard.yaes" %% "yaes-data" % "0.2.0"  // Optional: for Flow and other data structures
)
```

## ✨ What's New in YÆS?

You can choose between **monadic style**:

```scala
import in.rcard.yaes.Random.*
import in.rcard.yaes.Raise.*
import in.rcard.yaes.Yaes.*

def drunkFlip(using Random, Raise[String]): String = for {
  caught <- Random.nextBoolean
  heads  <- if (caught) Random.nextBoolean else Raise.raise("We dropped the coin")
} yield if (heads) "Heads" else "Tails"
```

Or a more **direct style**:

```scala
import in.rcard.yaes.Random.*
import in.rcard.yaes.Raise.*

def drunkFlip(using Random, Raise[String]): String = {
  val caught = Random.nextBoolean
  if (caught) {
    val heads = Random.nextBoolean
    if (heads) "Heads" else "Tails"
  } else {
    Raise.raise("We dropped the coin")
  }
}
```

## 🎯 Core Concepts

In YÆS, types like `Random` and `Raise` are **Effects**:

- A **Side Effect** is an unpredictable interaction, usually with an external system
- An **Effect System** manages Side Effects by tracking and wrapping them into Effects
- An **Effect** describes the type of the Side Effect and the return type of an effectful computation

YÆS uses **deferred execution** - calling effectful functions returns a value that represents something that can be run but hasn't yet.

## 🛠 Effect Management

Effects are managed using **Handlers**:

```scala
import in.rcard.yaes.Random.*
import in.rcard.yaes.Raise.*

val result: String = Raise.run { 
  Random.run { 
    drunkFlip
  }
}
```

## 📚 Available Effects

- [**IO**](effects/io.html) - Side-effecting operations
- [**Async**](effects/async.html) - Asynchronous computations and fiber management
- [**Raise**](effects/raise.html) - Error handling and propagation
- [**Resource**](effects/resource.html) - Automatic resource management
- [**Input**](effects/input.html) - Console input operations
- [**Output**](effects/output.html) - Console output operations
- [**Random**](effects/random.html) - Random content generation
- [**Clock**](effects/clock.html) - Time management
- [**System**](effects/system.html) - System properties and environment variables
- [**Log**](effects/log.html) - Logging at different levels

## 🗃 Data Structures

- [**Flow**](data-structures.html#flow) - Cold asynchronous data streams with rich operators
- [**More data structures**](data-structures.html) - Additional functional data structures

## 🤝 Contributing

Contributions are welcome! Please check our [contributing guidelines](contributing.html) to get started.

## 🙏 Acknowledgments

Special thanks to all the smart engineers who helped with ideas and suggestions, including Daniel Ciocîrlan, Simon Vergauwen, Jon Pretty, Noel Welsh, and Flavio Brasil.
