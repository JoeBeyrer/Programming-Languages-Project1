program ErrorTest3;
var
  i: Integer;
begin
  i := 0;
  while i < 1 do
  begin
    loopOnly := 42;
    i := i + 1;
  end;
  writeln(i);
  writeln(loopOnly);
end.
