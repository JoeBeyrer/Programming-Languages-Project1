LLVM backend notes

Build Java compiler:
  javac -cp lib/antlr-4.13.2-complete.jar src/*.java

Generate LLVM IR:
  java -cp "src;lib/antlr-4.13.2-complete.jar" Main tests/test1.pas -o output.ll

Compile generated IR natively for quick testing:
  clang output.ll runtime_native.c -o program
  ./program

This backend supports the subset exercised by the included tests:
- Integer, Boolean, String literals
- Global and local variables
- Assignment
- Arithmetic and comparisons
- if / while / for / break / continue
- Procedures and functions with parameters
- Class fields, constructors, methods, destructors
- Simple inheritance field layout reuse
- readln(Integer) and writeln(Integer/Boolean/String)

Notes:
- Interface declarations are parsed and carried through class metadata, but no interface dispatch is implemented.
- Nested routine declarations are not lowered, matching the original interpreter behavior.
- Extra-credit browser WASM glue is not validated in this container because llc/wasm-ld are unavailable here.
