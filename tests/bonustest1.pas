program BonusTest1;

type
  Animal = class
  private
    age: Integer;
  public
    constructor Create(a: Integer);
  end;

  Dog = class(Animal)
  private
    tricks: Integer;
  public
    constructor Create(a: Integer; t: Integer);
  end;

constructor Animal.Create(a: Integer);
begin
  age := a;
end;

constructor Dog.Create(a: Integer; t: Integer);
begin
  age := a;
  tricks := t;
end;

var
  d: Dog;
begin
  d := Dog.Create(4, 3);
  writeln(d.age);
  writeln(d.tricks);
end.