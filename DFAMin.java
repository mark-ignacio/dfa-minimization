import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class DFAMin {

    public static void main(String[] args) throws FileNotFoundException {
        new DFAMin();
    }

    public DFAMin() throws FileNotFoundException {
        Scanner inFile = new Scanner(new File("sample.in"));
        int numDFAs = Integer.parseInt(inFile.nextLine());
        ArrayList<DFA> DFAs = new ArrayList<DFA>();
        for (int i = 0; i < numDFAs; i++) {
            DFA automaton = readDFA(inFile);
            automaton.trim();
            automaton.minimize();
            DFAs.add(automaton);
        }

        int i = 1;
        for (DFA automaton : DFAs) {
            System.out.println("DFA #" + i + ":");
            System.out.println(automaton.toString());
            System.out.println();
            i++;
        }
    }

    private DFA readDFA(Scanner inFile) {
        String[] line = inFile.nextLine().split(" ");
        int numStates = Integer.parseInt(line[0]);
        int numAlpha = Integer.parseInt(line[1]);

        line = inFile.nextLine().split(" ");

        int acceptStates[] = new int[line.length - 1];
        for (int i = 1; i < line.length; i++) {
            acceptStates[i - 1] = Integer.parseInt(line[i]);
        }

        // the arduous process of reading inputs...
        DFAState[] states = new DFAState[numStates];
        for (int i = 0; i < numStates; i++) {
            line = inFile.nextLine().split(" ");
            int[] transitions = new int[line.length];
            for (int j = 0; j < line.length; j++) {
                transitions[j] = Integer.parseInt(line[j]);
            }

            DFAState state = new DFAState(transitions);
            states[i] = state;
        }

        return new DFA(numAlpha, acceptStates, states);
    }

    private class DFA {

        DFAState[] states;
        int[] acceptStates;
        char[] alphabet;

        public DFA(int numAlphabet, int[] acceptStates, DFAState[] states) {
            alphabet = new char[numAlphabet];
            this.states = states;
            this.acceptStates = acceptStates;

            // fill alphabet
            for (int i = 0; i < numAlphabet; i++) {
                alphabet[i] = (char) (i + 97);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);

            formatter.format("%d %d", states.length, alphabet.length);

            return sb.toString();
        }

        public void minimize() {

        }

        public void trim() {
            // do a BFS for unreachable nodes, cut them out, and renumber accordingly
            // iterative because why not
            boolean[] visited = new boolean[states.length];
            Queue<Integer> visitQueue = new LinkedList<Integer>();

            // init with q0
            visitQueue.add(0);
            visited[0] = true;
            while (!visitQueue.isEmpty()) {
                int toVisit = visitQueue.remove();
                DFAState visitingState = states[toVisit];
                for (int otherState : visitingState.transitions) {
                    if (!visited[otherState]) {
                        visitQueue.add(otherState);
                    }
                }
                visited[toVisit] = true;
            }

            // null out unreachable states
            for (int i = 0; i < visited.length; i++) {
                if (!visited[i]) {
                    states[i] = null;
                }
            }
        }
    }

    // pretty much a node in a directed graph
    private class DFAState {
        public int[] transitions;

        public DFAState(int[] transitions) {
            this.transitions = transitions;
        }
    }
}
