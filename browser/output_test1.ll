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
@g.n = global i32 0

%class.Counter = type { i32 }

define ptr @Counter_Create(ptr %this, i32 %arg.n) {
entry:
  %n.addr = alloca i32
  store i32 %arg.n, ptr %n.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %n.addr
  %t2 = getelementptr inbounds %class.Counter, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  ret ptr %this
}

define void @Counter_PrintCount(ptr %this) {
entry:
  %t1 = getelementptr inbounds %class.Counter, ptr %this, i32 0, i32 0
  %t2 = load i32, ptr %t1
  call void @print_i32(i32 %t2)
  ret void
}

define i32 @main() {
entry:
  %t1 = call i32 @read_i32()
  store i32 %t1, ptr @g.n
  %t2 = call ptr @malloc(i64 8)
  %t3 = load i32, ptr @g.n
  %t4 = call ptr @Counter_Create(ptr %t2, i32 %t3)
  store ptr %t2, ptr @g.c
  %t5 = load ptr, ptr @g.c
  call void @Counter_PrintCount(ptr %t5)
  ret i32 0
}

