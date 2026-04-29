; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.v = global i32 0

define i32 @main() {
entry:
  store i32 42, ptr @g.v
  %t1 = load i32, ptr @g.v
  call void @print_i32(i32 %t1)
  ret i32 0
}

