program Test3;

type
  Point = class
  private
    x: Integer;
    y: Integer;
  public
    constructor Create(px: Integer; py: Integer);
  end;

constructor Point.Create(px: Integer; py: Integer);
begin
  x := px;
  y := py;
end;

var
  p1: Point;
  p2: Point;
begin
  p1 := Point.Create(3, 7);
  p2 := Point.Create(10, 20);
  writeln(p1.x);
  writeln(p1.y);
  writeln(p2.x);
  writeln(p2.y);
end.
