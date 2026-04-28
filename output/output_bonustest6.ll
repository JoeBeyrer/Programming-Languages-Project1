; Generated from Pascal/Delphi AST

declare void @print_i32(i32)
declare void @print_bool(i1)
declare void @print_str(ptr)
declare i32 @read_i32()
declare ptr @malloc(i64)
declare i32 @strcmp(ptr, ptr)
declare ptr @pas_str_concat(ptr, ptr)

@.str.0 = private unnamed_addr constant [1 x i8] c"\00"

define i32 @MulAdd(i32 %arg.a, i32 %arg.b, i32 %arg.c) {
entry:
  %a.addr = alloca i32
  store i32 %arg.a, ptr %a.addr
  %b.addr = alloca i32
  store i32 %arg.b, ptr %b.addr
  %c.addr = alloca i32
  store i32 %arg.c, ptr %c.addr
  %MulAdd.result = alloca i32
  store i32 0, ptr %MulAdd.result
  %temp.addr = alloca i32
  store i32 0, ptr %temp.addr
  %t1 = load i32, ptr %a.addr
  %t2 = load i32, ptr %b.addr
  %t3 = mul i32 %t1, %t2
  store i32 %t3, ptr %temp.addr
  %t4 = load i32, ptr %temp.addr
  %t5 = load i32, ptr %c.addr
  %t6 = add i32 %t4, %t5
  store i32 %t6, ptr %MulAdd.result
  %t7 = load i32, ptr %MulAdd.result
  ret i32 %t7
}

define i32 @main() {
entry:
  %t1 = call i32 @MulAdd(i32 2, i32 3, i32 4)
  call void @print_i32(i32 %t1)
  ret i32 0
}

