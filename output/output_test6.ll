; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.r = global ptr null
@g.w = global i32 0
@g.h = global i32 0

%class.Rectangle = type { i32, i32, i32 }

define ptr @Rectangle_Create(ptr %this, i32 %arg.w, i32 %arg.h) {
entry:
  %w.addr = alloca i32
  store i32 %arg.w, ptr %w.addr
  %h.addr = alloca i32
  store i32 %arg.h, ptr %h.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %w.addr
  %t2 = getelementptr inbounds %class.Rectangle, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  %t3 = load i32, ptr %h.addr
  %t4 = getelementptr inbounds %class.Rectangle, ptr %this, i32 0, i32 1
  store i32 %t3, ptr %t4
  %t5 = load i32, ptr %w.addr
  %t6 = load i32, ptr %h.addr
  %t7 = mul i32 %t5, %t6
  %t8 = getelementptr inbounds %class.Rectangle, ptr %this, i32 0, i32 2
  store i32 %t7, ptr %t8
  ret ptr %this
}

define void @Rectangle_PrintInfo(ptr %this) {
entry:
  %t1 = getelementptr inbounds %class.Rectangle, ptr %this, i32 0, i32 0
  %t2 = load i32, ptr %t1
  call void @print_i32(i32 %t2)
  %t3 = getelementptr inbounds %class.Rectangle, ptr %this, i32 0, i32 1
  %t4 = load i32, ptr %t3
  call void @print_i32(i32 %t4)
  %t5 = getelementptr inbounds %class.Rectangle, ptr %this, i32 0, i32 2
  %t6 = load i32, ptr %t5
  call void @print_i32(i32 %t6)
  ret void
}

define i32 @main() {
entry:
  %t1 = call i32 @read_i32()
  store i32 %t1, ptr @g.w
  %t2 = call i32 @read_i32()
  store i32 %t2, ptr @g.h
  %t3 = call ptr @malloc(i64 24)
  %t4 = load i32, ptr @g.w
  %t5 = load i32, ptr @g.h
  %t6 = call ptr @Rectangle_Create(ptr %t3, i32 %t4, i32 %t5)
  store ptr %t3, ptr @g.r
  %t7 = load ptr, ptr @g.r
  call void @Rectangle_PrintInfo(ptr %t7)
  ret i32 0
}

