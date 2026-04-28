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

define void @ShowNext() {
entry:
  %t1 = load i32, ptr @g.g
  %t2 = add i32 %t1, 1
  store i32 %t2, ptr @g.g
  %t3 = load i32, ptr @g.g
  call void @print_i32(i32 %t3)
  ret void
}

define i32 @main() {
entry:
  store i32 10, ptr @g.g
  call void @ShowNext()
  call void @ShowNext()
  ret i32 0
}

