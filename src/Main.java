
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        String inputFile = args.length > 0 ? args[0] : "tests/test1.pas";
        boolean showProp = false;
        String outputFile = "output.ll";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--show-prop")) {
                showProp = true;
            }
            if (args[i].equals("-o") && i + 1 < args.length) {
                outputFile = args[i + 1];
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

        LlvmCodeGenerator generator = new LlvmCodeGenerator();
        String ir = generator.generate(program);
        Files.writeString(Path.of(outputFile), ir);
        System.out.println("Wrote LLVM IR to " + outputFile);
    }
}