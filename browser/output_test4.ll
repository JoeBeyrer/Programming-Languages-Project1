; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.c = global ptr null

%class.Calculator = type { i32 }

define ptr @Calculator_Create(ptr %this, i32 %arg.a, i32 %arg.b) {
entry:
  %a.addr = alloca i32
  store i32 %arg.a, ptr %a.addr
  %b.addr = alloca i32
  store i32 %arg.b, ptr %b.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %a.addr
  %t2 = load i32, ptr %b.addr
  %t3 = add i32 %t1, %t2
  %t4 = getelementptr inbounds %class.Calculator, ptr %this, i32 0, i32 0
  store i32 %t3, ptr %t4
  ret ptr %this
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 8)
  %t2 = call ptr @Calculator_Create(ptr %t1, i32 12, i32 8)
  store ptr %t1, ptr @g.c
  %t3 = load ptr, ptr @g.c
  %t4 = getelementptr inbounds %class.Calculator, ptr %t3, i32 0, i32 0
  %t5 = load i32, ptr %t4
  call void @print_i32(i32 %t5)
  ret i32 0
}

