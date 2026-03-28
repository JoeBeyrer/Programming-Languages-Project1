program Test13;
var
  g: Integer;

function NextValue: Integer;
begin
  NextValue := g + 1;
end;

begin
  g := 20;
  writeln(NextValue());
end.
