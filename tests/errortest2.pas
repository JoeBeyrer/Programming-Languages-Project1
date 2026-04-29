program ErrorTest2;
var
  i: Integer;

procedure ShowTemp;
begin
  writeln(temp);
end;

begin
  i := 0;
  while i < 1 do
  begin
    temp := 99;
    ShowTemp;
    i := i + 1;
  end;
end.
