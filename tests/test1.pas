program Test2;

type
  Counter = class
  private
    count: Integer;
  public
    constructor Create(n: Integer);
    procedure PrintCount;
  end;

constructor Counter.Create(n: Integer);
begin
  count := n;
end;

procedure Counter.PrintCount;
begin
  writeln(count);
end;

var
  c: Counter;
  n: Integer;
begin
  readln(n);
  c := Counter.Create(n);
  c.PrintCount;
end.
