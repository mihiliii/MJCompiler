# MJCompiler

A full **MicroJava compiler** implemented in Java that translates MicroJava source code (`.mj`) into MicroJava bytecode (`.obj`) executable on the MJ virtual machine.

---

## Overview

MicroJava is a simplified, Java-like language designed for educational use. This compiler implements all four classical compilation phases:

1. **Lexical Analysis** — tokenizes source code using a JFlex-generated scanner (`mjlexer.lex`)
2. **Syntax Analysis** — parses the token stream using a CUP-generated LALR(1) parser (`mjparser.cup`), producing an Abstract Syntax Tree (AST)
3. **Semantic Analysis** — performs scope resolution, type checking, symbol table management, and validates language rules (e.g. presence of `main`, correct use of enums, arrays, etc.)
4. **Code Generation** — traverses the AST and emits MicroJava bytecode using the MJ runtime library

---

## Project Structure

```
MJCompiler/
├── spec/
│   ├── mjlexer.lex          # JFlex lexer specification
│   └── mjparser.cup         # CUP parser grammar
├── src/rs/ac/bg/etf/pp1/
│   ├── Compiler.java        # Main entry point
│   ├── SemanticAnalyzer.java
│   ├── CodeGenerator.java
│   ├── ast/                 # Auto-generated AST node classes
│   ├── structs/             # Helper structures (Declaration, Literal)
│   ├── symboltable/         # Symbol table implementation
│   └── util/                # Logging utilities
├── lib/
│   ├── JFlex.jar            # Lexer generator
│   ├── cup_v10k.jar         # Parser generator
│   ├── mj-runtime-1-1.jar   # MicroJava virtual machine & runtime
│   ├── symboltable-1-1.jar  # Symbol table library
│   └── log4j-1.2.17.jar     # Logging framework
├── test/
│   ├── program.mj           # Example MicroJava source file
│   ├── program.obj          # Compiled bytecode output
│   ├── gentest.mj           # Additional test program
│   ├── semantic_errors.mj   # Test file with intentional semantic errors
│   └── input.txt            # Standard input for test runs
├── config/
│   └── log4j.xml            # Log4j configuration
└── build.xml                # Apache Ant build script
```

---

## Prerequisites

- **Java JDK 8+**
- **Apache Ant** (for building)

---

## Building

The project uses Apache Ant. The build process automatically generates the lexer and parser from their specifications before compiling:

```bash
ant compile
```

This runs the full pipeline: `delete` → `lexerGen` → `parserGen` → `repackage` → `compile`.

To only regenerate the lexer and parser without compiling:

```bash
ant repackage
```

---

## Running the Compiler

After building, compile a `.mj` source file by running `Compiler` with the source file path as the argument:

```bash
java -cp "bin:lib/*" rs.ac.bg.etf.pp1.Compiler test/program.mj
```

On Windows:

```bash
java -cp "bin;lib/*" rs.ac.bg.etf.pp1.Compiler test\program.mj
```

If compilation succeeds, the bytecode is written to `test/program.obj`.

---

## Running the Bytecode

To execute the compiled `.obj` file on the MicroJava virtual machine:

```bash
ant runObj
```

Or directly:

```bash
java -cp "lib/mj-runtime-1-1.jar" rs.etf.pp1.mj.runtime.Run test/program.obj < test/input.txt
```

To disassemble the bytecode for inspection:

```bash
ant disasm
```

---

## MicroJava Language Features

The compiler supports the following MicroJava language constructs:

- Primitive types: `int`, `char`, `bool`
- Constants (`const`) and global/local variables
- One-dimensional arrays with `new T[size]` allocation and `.length`
- `enum` declarations with optional custom values
- Functions/methods including `void` and typed return values
- Built-in functions: `print`, `read`, `ord`, `chr`, `len`
- Arithmetic, relational, and logical operators
- Increment/decrement operators (`++`, `--`)
- Ternary (conditional) expression: `condition ? expr : expr`
- Single-line comments (`//`)
- A mandatory `main()` method as program entry point

### Example Program

```java
program example

enum Color { RED, GREEN, BLUE }
const int MAX = 10;
int arr[];

{
    void main()
        int x;
    {
        x = MAX;
        arr = new int[3];
        arr[0] = 42;
        print(arr[0]);
        print(eol);
    }
}
```

---

## Logging

The compiler uses **Log4j** for structured logging. All compilation events, errors, and debug info are written to the `logs/` directory. The logging configuration can be adjusted in `config/log4j.xml`.

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JFlex | — | Lexer generator |
| CUP | v10k | Parser generator (LALR) |
| mj-runtime | 1.1 | MicroJava VM & bytecode utilities |
| symboltable | 1.1 | Symbol table data structures |
| Log4j | 1.2.17 | Logging |

---