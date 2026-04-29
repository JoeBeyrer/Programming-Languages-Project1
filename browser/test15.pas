program Test15;
var
  i: Integer;
begin
  for i := 1 to 5 do
  begin
    if i = 2 then
      continue;
    if i = 5 then
      break;
    else
      writeln(i);
  end;
end.
