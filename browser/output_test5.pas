program Test5;

type
  Dog = class
  private
    age: Integer;
  public
    constructor Create(a: Integer);
  end;

  Cat = class
  private
    lives: Integer;
  public
    constructor Create(l: Integer);
  end;

constructor Dog.Create(a: Integer);
begin
  age := a;
end;

constructor Cat.Create(l: Integer);
begin
  lives := l;
end;

var
  d: Dog;
  c: Cat;
begin
  d := Dog.Create(3);
  c := Cat.Create(9);
  writeln(d.age);
  writeln(c.lives);
end.
