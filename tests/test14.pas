program Test14;

procedure MakeLocal;
var
  localValue: Integer;
begin
  localValue := 7;
  writeln(localValue);
end;

begin
  MakeLocal;
  writeln(localValue);
end.
