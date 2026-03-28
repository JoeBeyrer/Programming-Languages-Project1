import java.util.List;

public class ConstantPropagationPrinter {

    public void print(List<String> changes) {
        if (changes == null || changes.isEmpty()) {
            System.out.println("No constant propagation opportunities found.");
            return;
        }

        System.out.println("Constant propagation:");
        for (String change : changes) {
            System.out.println(change);
        }
        System.out.println();
    }
}