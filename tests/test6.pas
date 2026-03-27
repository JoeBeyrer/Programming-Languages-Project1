program Test9;

type
  Rectangle = class
  private
    width: Integer;
    height: Integer;
    area: Integer;
  public
    constructor Create(w: Integer; h: Integer);
    procedure PrintInfo;
  end;

constructor Rectangle.Create(w: Integer; h: Integer);
begin
  width := w;
  height := h;
  area := w * h;
end;

procedure Rectangle.PrintInfo;
begin
  writeln(width);
  writeln(height);
  writeln(area);
end;

var
  r: Rectangle;
  w: Integer;
  h: Integer;
begin
  readln(w);
  readln(h);
  r := Rectangle.Create(w, h);
  r.PrintInfo;
end.
