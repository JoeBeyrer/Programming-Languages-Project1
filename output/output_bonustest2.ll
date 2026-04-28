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

%class.Document = type { i32 }

define ptr @Document_Create(ptr %this, i32 %arg.p) {
entry:
  %p.addr = alloca i32
  store i32 %arg.p, ptr %p.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %p.addr
  %t2 = getelementptr inbounds %class.Document, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  ret ptr %this
}

define void @Document_Print(ptr %this) {
entry:
  %t1 = getelementptr inbounds %class.Document, ptr %this, i32 0, i32 0
  %t2 = load i32, ptr %t1
  call void @print_i32(i32 %t2)
  ret void
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 8)
  %t2 = call ptr @Document_Create(ptr %t1, i32 50)
  store ptr %t1, ptr @g.d
  %t3 = load ptr, ptr @g.d
  call void @Document_Print(ptr %t3)
  ret i32 0
}

