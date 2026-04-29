program Test2;

type
  Box = class
  public
    constructor Create;
    destructor Destroy;
  end;

constructor Box.Create;
begin
  writeln(1);
end;

destructor Box.Destroy;
begin
  writeln(0);
end;

var
  b: Box;
begin
  b := Box.Create;
  b.Destroy;
end.
