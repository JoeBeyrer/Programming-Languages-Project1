program Test19;
var
  i: Integer;
begin
  for i := 1 to 5 do
  begin
    if i = 2 then
      continue;
    if i = 5 then
      break;
    writeln(i);
  end;
end.
