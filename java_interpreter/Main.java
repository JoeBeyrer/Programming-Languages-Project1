import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main {
    public static void main(String[] args) throws Exception {
        String inputFile = args.length > 0 ? args[0] : "../tests/test1.pas";

        delphiLexer lexer = new delphiLexer(CharStreams.fromFileName(inputFile));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        delphiParser parser = new delphiParser(tokens);

        delphiParser.ProgramContext tree = parser.program();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException("Parsing failed with " + parser.getNumberOfSyntaxErrors() + " syntax errors.");
        }

        Interpreter interpreter = new Interpreter();
        interpreter.visit(tree);
    }
}
