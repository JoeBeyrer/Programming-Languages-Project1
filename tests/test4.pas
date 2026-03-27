program Test6;

type
  Calculator = class
  private
    result: Integer;
  public
    constructor Create(a: Integer; b: Integer);
  end;

constructor Calculator.Create(a: Integer; b: Integer);
begin
  result := a + b;
end;

var
  c: Calculator;
begin
  c := Calculator.Create(12, 8);
  writeln(c.result);
end.
