program Test10;

type
  Base = class
  private
    value: Integer;
  public
    constructor Create(v: Integer);
    destructor Destroy;
  end;

constructor Base.Create(v: Integer);
begin
  value := v;
end;

destructor Base.Destroy;
begin
  writeln(0);
end;

var
  b: Base;
begin
  b := Base.Create(99);
  writeln(b.value);
  b.Destroy;
end.
