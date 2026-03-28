program BonusTest5;

procedure PrintSum(a: Integer; b: Integer);
var
  total: Integer;
begin
  total := a + b;
  writeln(total);
end;

begin
  PrintSum(4, 6);
end.
