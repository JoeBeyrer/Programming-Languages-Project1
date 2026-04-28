; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.b = global ptr null

%class.Base = type { i32 }

define ptr @Base_Create(ptr %this, i32 %arg.v) {
entry:
  %v.addr = alloca i32
  store i32 %arg.v, ptr %v.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %v.addr
  %t2 = getelementptr inbounds %class.Base, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  ret ptr %this
}

define void @Base_Destroy(ptr %this) {
entry:
  call void @print_i32(i32 0)
  ret void
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 8)
  %t2 = call ptr @Base_Create(ptr %t1, i32 99)
  store ptr %t1, ptr @g.b
  %t3 = load ptr, ptr @g.b
  %t4 = getelementptr inbounds %class.Base, ptr %t3, i32 0, i32 0
  %t5 = load i32, ptr %t4
  call void @print_i32(i32 %t5)
  %t6 = load ptr, ptr @g.b
  call void @Base_Destroy(ptr %t6)
  ret i32 0
}

