
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main {

    public static void main(String[] args) throws Exception {
        String inputFile = args.length > 0 ? args[0] : "tests/test1.pas";
        boolean showProp = false;

        for (String arg : args) {
            if (arg.equals("--show-prop")) {
                showProp = true;
            }
        }

        delphiLexer lexer = new delphiLexer(CharStreams.fromFileName(inputFile));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        delphiParser parser = new delphiParser(tokens);

        delphiParser.ProgramContext tree = parser.program();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException(
                "Parsing failed with " + parser.getNumberOfSyntaxErrors() + " syntax errors."
            );
        }

        AstBuilder builder = new AstBuilder();
        ProgramNode program = (ProgramNode) builder.visit(tree);

        ConstantPropagation propagation = new ConstantPropagation();
        program = propagation.propagateProgram(program);

        if (showProp) {
            ConstantPropagationPrinter reporter = new ConstantPropagationPrinter();
            reporter.print(propagation.getChanges());
        }

        Interpreter interpreter = new Interpreter();
        interpreter.executeProgram(program);
    }
}