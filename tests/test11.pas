program Test11;
var
  x: Integer;
begin
  x := 0;
  while x < 5 do
  begin
    x := x + 1;
    if x = 3 then
      continue;
    writeln(x);
  end;
end.
