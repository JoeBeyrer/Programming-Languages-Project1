; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.g = global i32 0

define i32 @AddToGlobal() {
entry:
  %AddToGlobal.result = alloca i32
  store i32 0, ptr %AddToGlobal.result
  %localValue.addr = alloca i32
  store i32 0, ptr %localValue.addr
  store i32 5, ptr %localValue.addr
  %t1 = load i32, ptr @g.g
  %t2 = load i32, ptr %localValue.addr
  %t3 = add i32 %t1, %t2
  store i32 %t3, ptr %AddToGlobal.result
  %t4 = load i32, ptr %AddToGlobal.result
  ret i32 %t4
}

define i32 @main() {
entry:
  store i32 10, ptr @g.g
  %t1 = call i32 @AddToGlobal()
  call void @print_i32(i32 %t1)
  ret i32 0
}

