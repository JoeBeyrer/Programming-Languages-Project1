# Programming Languages Project 2

## Folder layout

```text
README.md
grammar/
  delphi.g4
src/
  Main.java
  AstNodes.java
  AstBuilder.java
  ConstantFolder.java
  AstPrinter.java
  Interpreter.java
  Scope.java
  Signals.java
lib/
  antlr-4.13.2-complete.jar
tests/
  test1.pas ...
  bonustest1.pas ...
```

## Git Bash commands

### 1. Clean old files

If any errors are encountered during execution, execute the following commands to clean and regenerate old files.

```bash
rm -f src/*.class
rm -f src/*.tokens
rm -f src/*.interp
rm -f src/delphiLexer.java src/delphiParser.java src/delphiListener.java src/delphiBaseListener.java src/delphiVisitor.java src/delphiBaseVisitor.java
```

### 2. Regenerate ANTLR files

```bash
java -jar lib/antlr-4.13.2-complete.jar -visitor -listener -o src grammar/delphi.g4
```

### 3. Compile

```bash
javac -cp "lib/antlr-4.13.2-complete.jar" src/*.java
```

### 4. Run tests

```bash
java -cp "src;lib/antlr-4.13.2-complete.jar" Main tests/test1.pas
```

### 5. Print the folded AST while running a test

This is useful for the constant-propagation bonus.

```bash
java -cp "src;lib/antlr-4.13.2-complete.jar" Main tests/bonustest4.pas --folds
```

## Tests

### Project 1 tests

- `test1.pas`
- `test2.pas`
- `test3.pas`
- `test4.pas`
- `test5.pas`
- `test6.pas`
- `test7.pas`
- `bonustest1.pas`
- `bonustest2.pas`

### Project 2 tests

- `test8.pas` - basic `while`
- `test9.pas` - `for` with `to` and `downto`
- `test10.pas` - `break`
- `test11.pas` - `continue`
- `test12.pas` - user-defined procedure
- `test13.pas` - user-defined function
- `test14.pas` - function using a global plus its own local
- `test15.pas` - `break` and `continue` inside `for`

### Bonus passing tests

- `bonustest3.pas` - constant propagation full fold
- `bonustest4.pas` - constant propagation partial fold
- `bonustest5.pas` - formal parameter passing in procedure
- `bonustest6.pas` - formal parameter passing in function

### Intentional failure tests

These are expected to stop with a runtime error. They are to show that scope does not leak.

- `errotest1.pas`
- `errotest2.pas`
- `errotest3.pas`
- `errotest4.pas`
- `bonuserrotest1.pas`



## What was implemented

### Core Ffeatures

- classes and objects
- constructors and destructors
- object method calls
- terminal I/O with `readln` and `writeln`
- while ... do
- for ... do
- break
- continue
- user-defined procedures
- user-defined functions
- static scoping for routines
- loop-body scopes for `while` and `for`

### Bonus features

- simple constant propagation over the AST
- formal parameter passing for procedures and functions

