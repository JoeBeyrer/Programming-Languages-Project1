; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.d = global ptr null
@g.c = global ptr null

%class.Dog = type { i32 }
%class.Cat = type { i32 }

define ptr @Dog_Create(ptr %this, i32 %arg.a) {
entry:
  %a.addr = alloca i32
  store i32 %arg.a, ptr %a.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %a.addr
  %t2 = getelementptr inbounds %class.Dog, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  ret ptr %this
}

define ptr @Cat_Create(ptr %this, i32 %arg.l) {
entry:
  %l.addr = alloca i32
  store i32 %arg.l, ptr %l.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %l.addr
  %t2 = getelementptr inbounds %class.Cat, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  ret ptr %this
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 8)
  %t2 = call ptr @Dog_Create(ptr %t1, i32 3)
  store ptr %t1, ptr @g.d
  %t3 = call ptr @malloc(i64 8)
  %t4 = call ptr @Cat_Create(ptr %t3, i32 9)
  store ptr %t3, ptr @g.c
  %t5 = load ptr, ptr @g.d
  %t6 = getelementptr inbounds %class.Dog, ptr %t5, i32 0, i32 0
  %t7 = load i32, ptr %t6
  call void @print_i32(i32 %t7)
  %t8 = load ptr, ptr @g.c
  %t9 = getelementptr inbounds %class.Cat, ptr %t8, i32 0, i32 0
  %t10 = load i32, ptr %t9
  call void @print_i32(i32 %t10)
  ret i32 0
}

