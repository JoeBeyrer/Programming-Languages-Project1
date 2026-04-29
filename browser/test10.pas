program Test10;
var
  x: Integer;
begin
  x := 0;
  while x < 5 do
  begin
    if x = 3 then
      break;
    writeln(x);
    x := x + 1;
  end;
end.
