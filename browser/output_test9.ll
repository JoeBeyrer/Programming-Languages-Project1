; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

@g.i = global i32 0

define i32 @main() {
entry:
  %for.end.i.0 = alloca i32
  store i32 1, ptr @g.i
  store i32 3, ptr %for.end.i.0
  br label %for.cond.1
for.cond.1:
  %t1 = load i32, ptr @g.i
  %t2 = load i32, ptr %for.end.i.0
  %t3 = icmp sle i32 %t1, %t2
  br i1 %t3, label %for.body.2, label %for.end.4
for.body.2:
  %t4 = load i32, ptr @g.i
  call void @print_i32(i32 %t4)
  br label %for.step.3
for.step.3:
  %t5 = load i32, ptr @g.i
  %t6 = add i32 %t5, 1
  store i32 %t6, ptr @g.i
  br label %for.cond.1
for.end.4:
  %for.end.i.4 = alloca i32
  store i32 3, ptr @g.i
  store i32 1, ptr %for.end.i.4
  br label %for.cond.5
for.cond.5:
  %t7 = load i32, ptr @g.i
  %t8 = load i32, ptr %for.end.i.4
  %t9 = icmp sge i32 %t7, %t8
  br i1 %t9, label %for.body.6, label %for.end.8
for.body.6:
  %t10 = load i32, ptr @g.i
  call void @print_i32(i32 %t10)
  br label %for.step.7
for.step.7:
  %t11 = load i32, ptr @g.i
  %t12 = sub i32 %t11, 1
  store i32 %t12, ptr @g.i
  br label %for.cond.5
for.end.8:
  ret i32 0
}

