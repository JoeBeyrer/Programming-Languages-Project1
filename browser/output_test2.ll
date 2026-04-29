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

%class.Box = type { i8 }

define ptr @Box_Create(ptr %this) {
entry:
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  call void @print_i32(i32 1)
  ret ptr %this
}

define void @Box_Destroy(ptr %this) {
entry:
  call void @print_i32(i32 0)
  ret void
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 1)
  %t2 = call ptr @Box_Create(ptr %t1)
  store ptr %t1, ptr @g.b
  %t3 = load ptr, ptr @g.b
  call void @Box_Destroy(ptr %t3)
  ret i32 0
}

