program BonusTest2;

type
  IPrintable = interface
    procedure Print;
  end;

  Document = class(TObject, IPrintable)
  private
    pages: Integer;
  public
    constructor Create(p: Integer);
    procedure Print;
  end;

constructor Document.Create(p: Integer);
begin
  pages := p;
end;

procedure Document.Print;
begin
  writeln(pages);
end;

var
  d: Document;
begin
  d := Document.Create(50);
  d.Print;
end.