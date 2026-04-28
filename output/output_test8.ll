; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.x = global i32 0

define i32 @main() {
entry:
  store i32 0, ptr @g.x
  br label %while.cond.1
while.cond.1:
  %t1 = load i32, ptr @g.x
  %t2 = icmp slt i32 %t1, 3
  br i1 %t2, label %while.body.2, label %while.end.3
while.body.2:
  %t3 = load i32, ptr @g.x
  call void @print_i32(i32 %t3)
  %t4 = load i32, ptr @g.x
  %t5 = add i32 %t4, 1
  store i32 %t5, ptr @g.x
  br label %while.cond.1
while.end.3:
  ret i32 0
}

