; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

define void @PrintSum(i32 %arg.a, i32 %arg.b) {
entry:
  %a.addr = alloca i32
  store i32 %arg.a, ptr %a.addr
  %b.addr = alloca i32
  store i32 %arg.b, ptr %b.addr
  %total.addr = alloca i32
  store i32 0, ptr %total.addr
  %t1 = load i32, ptr %a.addr
  %t2 = load i32, ptr %b.addr
  %t3 = add i32 %t1, %t2
  store i32 %t3, ptr %total.addr
  %t4 = load i32, ptr %total.addr
  call void @print_i32(i32 %t4)
  ret void
}

define i32 @main() {
entry:
  call void @PrintSum(i32 4, i32 6)
  ret i32 0
}

