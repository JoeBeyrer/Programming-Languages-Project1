; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.p1 = global ptr null
@g.p2 = global ptr null

%class.Point = type { i32, i32 }

define ptr @Point_Create(ptr %this, i32 %arg.px, i32 %arg.py) {
entry:
  %px.addr = alloca i32
  store i32 %arg.px, ptr %px.addr
  %py.addr = alloca i32
  store i32 %arg.py, ptr %py.addr
  %ctor.result = alloca ptr
  store ptr %this, ptr %ctor.result
  %t1 = load i32, ptr %px.addr
  %t2 = getelementptr inbounds %class.Point, ptr %this, i32 0, i32 0
  store i32 %t1, ptr %t2
  %t3 = load i32, ptr %py.addr
  %t4 = getelementptr inbounds %class.Point, ptr %this, i32 0, i32 1
  store i32 %t3, ptr %t4
  ret ptr %this
}

define i32 @main() {
entry:
  %t1 = call ptr @malloc(i64 16)
  %t2 = call ptr @Point_Create(ptr %t1, i32 3, i32 7)
  store ptr %t1, ptr @g.p1
  %t3 = call ptr @malloc(i64 16)
  %t4 = call ptr @Point_Create(ptr %t3, i32 10, i32 20)
  store ptr %t3, ptr @g.p2
  %t5 = load ptr, ptr @g.p1
  %t6 = getelementptr inbounds %class.Point, ptr %t5, i32 0, i32 0
  %t7 = load i32, ptr %t6
  call void @print_i32(i32 %t7)
  %t8 = load ptr, ptr @g.p1
  %t9 = getelementptr inbounds %class.Point, ptr %t8, i32 0, i32 1
  %t10 = load i32, ptr %t9
  call void @print_i32(i32 %t10)
  %t11 = load ptr, ptr @g.p2
  %t12 = getelementptr inbounds %class.Point, ptr %t11, i32 0, i32 0
  %t13 = load i32, ptr %t12
  call void @print_i32(i32 %t13)
  %t14 = load ptr, ptr @g.p2
  %t15 = getelementptr inbounds %class.Point, ptr %t14, i32 0, i32 1
  %t16 = load i32, ptr %t15
  call void @print_i32(i32 %t16)
  ret i32 0
}

