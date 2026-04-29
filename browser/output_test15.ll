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
  store i32 5, ptr %for.end.i.0
  br label %for.cond.1
for.cond.1:
  %t1 = load i32, ptr @g.i
  %t2 = load i32, ptr %for.end.i.0
  %t3 = icmp sle i32 %t1, %t2
  br i1 %t3, label %for.body.2, label %for.end.4
for.body.2:
  %t4 = load i32, ptr @g.i
  %t5 = icmp eq i32 %t4, 2
  br i1 %t5, label %if.then.5, label %if.end.6
if.then.5:
  br label %for.step.3
  br label %if.end.6
if.end.6:
  %t6 = load i32, ptr @g.i
  %t7 = icmp eq i32 %t6, 5
  br i1 %t7, label %if.then.7, label %if.end.8
if.then.7:
  br label %for.end.4
  br label %if.end.8
if.end.8:
  %t8 = load i32, ptr @g.i
  call void @print_i32(i32 %t8)
  br label %for.step.3
for.step.3:
  %t9 = load i32, ptr @g.i
  %t10 = add i32 %t9, 1
  store i32 %t10, ptr @g.i
  br label %for.cond.1
for.end.4:
  ret i32 0
}

