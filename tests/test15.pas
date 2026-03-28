program Test15;
var
  g: Integer;

function AddToGlobal: Integer;
var
  localValue: Integer;
begin
  localValue := 5;
  AddToGlobal := g + localValue;
end;

begin
  g := 10;
  writeln(AddToGlobal());
end.
