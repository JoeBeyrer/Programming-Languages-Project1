program Test12;
var
  g: Integer;

procedure ShowNext;
begin
  g := g + 1;
  writeln(g);
end;

begin
  g := 10;
  ShowNext;
  ShowNext;
end.
