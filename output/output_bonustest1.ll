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

%class.Animal = type { i32 }
%class.Dog = type { i32, i32 }

define ptr @Animal_Create(ptr %this, i32 %arg.a) {
entry:
  %a.addr = alloca i32
  store i32 %arg.a, ptr %a.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %a.addr
  %t2 = getelementptr inbounds %class.Animal, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  ret ptr %this
}

define ptr @Dog_Create(ptr %this, i32 %arg.a, i32 %arg.t) {
entry:
  %a.addr = alloca i32
  store i32 %arg.a, ptr %a.addr
  %t.addr = alloca i32
  store i32 %arg.t, ptr %t.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %a.addr
  %t2 = getelementptr inbounds %class.Dog, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  %t3 = load i32, ptr %t.addr
  %t4 = getelementptr inbounds %class.Dog, ptr %this, i32 0, i32 1
  store i32 %t3, ptr %t4
  ret ptr %this
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 16)
  %t2 = call ptr @Dog_Create(ptr %t1, i32 4, i32 3)
  store ptr %t1, ptr @g.d
  %t3 = load ptr, ptr @g.d
  %t4 = getelementptr inbounds %class.Dog, ptr %t3, i32 0, i32 0
  %t5 = load i32, ptr %t4
  call void @print_i32(i32 %t5)
  %t6 = load ptr, ptr @g.d
  %t7 = getelementptr inbounds %class.Dog, ptr %t6, i32 0, i32 1
  %t8 = load i32, ptr %t7
  call void @print_i32(i32 %t8)
  ret i32 0
}

