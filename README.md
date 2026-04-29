# Project 3

## Project 3 Compiler

This project keeps the existing Pascal/Delphi front end from Projects 1 and 2 and replaces direct interpretation with LLVM IR generation. `Main.java` parses a `.pas` file, builds the AST, runs constant propagation, and writes LLVM IR to the file passed.

Implemented LLVM-backed language coverage includes:

- Integer, Boolean, String, and nil literals
- Global and local variables
- Assignment
- Arithmetic operators: `+`, `-`, `*`, `/`, `div`, `mod`
- Boolean operators: `and`, `or`, `not`
- Comparisons: `=`, `<>`, `<`, `>`, `<=`, `>=`
- `if`, `while`, `for ... to`, `for ... downto`
- `break` and `continue`
- User-defined procedures and functions with value parameters
- Class fields, constructors, methods, destructors, and simple inherited field layout
- Runtime calls for `readln(Integer)` and `writeln(Integer/Boolean/String)`

## Git Bash commands

### 1. Clean old files

If any errors are encountered during execution, execute the following commands to clean and regenerate old files. Otherwise, skip to step 4.

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
Note: If the ANTLR files do not generate directly in src they may need to be manually moved to src.

### 3. Compile

```bash
javac -cp "lib/antlr-4.13.2-complete.jar" src/*.java
```

### 4. Generate LLVM IR

```bash
# Windows / Git Bash
java -cp "src;lib/antlr-4.13.2-complete.jar" Main tests/test8.pas -o output/output_test1.ll

# macOS / Linux
java -cp "src:lib/antlr-4.13.2-complete.jar" Main tests/test8.pas -o output/output_test1.ll
```

### 5. Run LLVM IR natively

```bash
clang output/output_test1.ll runtime_native.c -Wno-override-module -o program
./program
```

### 6. Compile LLVM IR to WebAssembly (bonus)

When `llc` is available, use these commands (I was unable to get llc working so I just used Clang):

```bash
llc -mtriple=wasm32-unknown-unknown -filetype=obj output/output_bonustest1.ll -o output/output_bonustest1.wasm32.o
clang --target=wasm32-unknown-unknown -O0 -nostdlib -fno-builtin -c wasm_runtime.c -o output/wasm_runtime.o
wasm-ld --no-entry --export=main --export-memory --allow-undefined output/output_bonustest1.wasm32.o output/wasm_runtime.o -o browser/output_bonustest1.wasm
```

If `llc` is not installed, use this Clang invocation that still uses LLVM's WebAssembly backend:

```bash
clang --target=wasm32-unknown-unknown -O0 -nostdlib -fno-builtin output/output_test1.ll wasm_runtime.c -Wl,--no-entry -Wl,--export=main -Wl,--export-memory -Wl,--allow-undefined -o browser/output_test1.wasm
```

### 7. Use Bash Scripts
The included helper script accepts an LLVM IR input and a `.wasm` output path. It also copies the matching `.ll` file and original `.pas` source next to the generated `.wasm` file to display the test program inputs:

```bash
./build_wasm.sh output/output_test1.ll browser/output_test1.wasm
```

An additional helper script can be used to create `.wasm` files for all test cases in the tests directory:
```bash
bash build_all_wasm.sh
```

### 8. Run the browser demo

Local server setup:

```bash
cd browser
python -m http.server 8000
```

Then open:

```text
http://localhost:8000/index.html
```

## Bonus runtime/browser files

- `wasm_runtime.c` is the WebAssembly runtime.
- `build_wasm.sh` builds a `.wasm` module from generated `.ll` plus `wasm_runtime.c`, then copies the `.ll` file and matching `.pas` source next to the `.wasm` file for browser display. 
- `build_all_wasm.sh` builds all `.wasm` modules for each test case.
- `browser/index.html` is the browser UI.
- `browser/app.js` fetches/instantiates the selected `.wasm`, dynamically updates the loaded file names, fetches the matching `.pas` and `.ll` files, supplies imported functions, decodes strings from WASM memory, and calls exported `main()`.


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

### Project 2 tests (carried over)

- `test8.pas` - basic `while`
- `test9.pas` - `for` with `to` and `downto`
- `test10.pas` - `break`
- `test11.pas` - `continue`
- `test12.pas` - user-defined procedure
- `test13.pas` - user-defined function
- `test14.pas` - function using a global plus its own local
- `test15.pas` - `break` and `continue` inside `for`

### Bonus passing tests (carried over)

- `bonustest3.pas` - constant propagation full fold
- `bonustest4.pas` - constant propagation partial fold
- `bonustest5.pas` - formal parameter passing in procedure
- `bonustest6.pas` - formal parameter passing in function

### Intentional failure tests (carried over)

These are expected to stop with a runtime error. They are to show that scope does not leak.

- `errotest1.pas`
- `errotest2.pas`
- `errotest3.pas`
- `errotest4.pas`
- `bonuserrotest1.pas`
