# Programming Languages Project 2

## What was implemented

### Core features

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
java -cp "src;lib/antlr-4.13.2-complete.jar" Main tests/bonustest4.pas --show-prop
```

## Tests

### Project 1 tests (carried over)

- `test1.pas`: constructor with parameter + readln input + private field access
  - Requires integer input in command line to run
  - Outputs the class field created from the user input
- `test2.pas`: no-arg constructor + destructor
  - Outputs 1 after succesfull construction and 0 after destruction
- `test3.pas`: multiple instances of the same class
  - Outputs point coordinates for two instances of the point object (3, 7) and (10, 20)
- `test4.pas`: arithmetic inside constructor body
  - Outputs 20 from 12 + 8 (hard-coded parameters) calculation inside constructor body
- `test5.pas`: multiple different classes in one program
  - Outputs 3 and 9 from fields of  Dog and Cat class instances
- `test6.pas`: readln input + arithmetic in constructor + multiple fields
  - Requires two integer inputs in command line to run
  - Outputs width (first input), height (second input), and area (product of two inputs)
- `test7.pas`: constructor with parameter + destructor explicitly called
  - Outputs 99 (parameter hardcoded in constructor) and 0 upon instance destruction
- `bonustest1.pas`: inheritance + subclass accessing parent field + two-parameter constructor
  - Outputs 4 and 3 (parameters of inherited class)
- `bonustest2.pas`: interface declaration + class implementing interface + procedure method call on instance
  - Outputs 50 (the hardcoded parameter passed to the constructor) using the Print procedure

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

