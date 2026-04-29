program ErrorTest4;
var
  i: Integer;
begin
  for i := 1 to 1 do
  begin
    loopOnly := 77;
    writeln(loopOnly);
  end;
  writeln(i);
  writeln(loopOnly);
end.
