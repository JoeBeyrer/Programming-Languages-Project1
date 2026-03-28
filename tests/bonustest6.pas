program BonusTest6;

function MulAdd(a: Integer; b: Integer; c: Integer): Integer;
var
  temp: Integer;
begin
  temp := a * b;
  MulAdd := temp + c;
end;

begin
  writeln(MulAdd(2, 3, 4));
end.
