import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
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
        HashSet<Integer> acceptStates;
        char[] alphabet;

        private boolean[][] D;
        private ArrayList<ArrayList<HashSet<Point>>> S;

        public DFA(int numAlphabet, int[] acceptStates, DFAState[] states) {
            alphabet = new char[numAlphabet];
            this.states = states;

            this.acceptStates = new HashSet<Integer>();
            for (int acceptState : acceptStates) {
                this.acceptStates.add(acceptState);
            }

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
            // Sudkamp minimization algorithm

            // 1. init structures
            D = new boolean[states.length][states.length];
            S = new ArrayList<ArrayList<HashSet<Point>>>();  // lol

            for (int i = 0; i < states.length; i++) {
                ArrayList<HashSet<Point>> innerList = new ArrayList<HashSet<Point>>();

                for (int j = 0; j < states.length; j++) {
                    innerList.add(new HashSet<Point>());
                }
                S.add(innerList);
            }

            // 2. states with different acceptances are distinguishable
            for (int i = 0; i < states.length; i++) {
                for (int j = i + 1; j < states.length; j++) {
                    if (acceptStates.contains(i) != acceptStates.contains(j)) {
                        D[i][j] = true;
                    }
                }
            }

            // 3. mark as possibly indistinguishable, enforce distinguishability
            for (int i = 0; i < states.length; i++) {
                for (int j = i + 1; j < states.length; j++) {
                    DFAState qi = states[i];
                    DFAState qj = states[j];

                    // helps emulate "for any"
                    boolean distinguished = false;
                    for (int k = 0; k < qi.transitions.length; k++) {
                        int qm = qi.transitions[k];
                        int qn = qj.transitions[k];

                        // if on the same letter, qm and qn move to distinguishable states
                        if (D[qm][qn] || D[qm][qn]) {
                            dist(i, j);
                            distinguished = true;
                        }

                        if (distinguished) {
                            break;
                        }
                    }

                    if (!distinguished) {
                        // qm and qn are indistinguishable
                        for (int k = 0; k < qi.transitions.length; k++) {
                            int qm = qi.transitions[k];
                            int qn = qj.transitions[k];

                            if (qm < qn && !(i == qm && j == qn)) {
                                S.get(qm).get(qn).add(new Point(qm, qn));
                            } else if (qm > qn && !(i == qn && j == qm)) {
                                S.get(qn).get(qm).add(new Point(qn, qm));
                            }
                        }
                    }

                }
            }


        }

        private void dist(int i, int j) {
            D[i][j] = true;

            HashSet<Point> sSet = S.get(i).get(j);
            for (Point pair : sSet) {
                sSet.remove(pair);
                dist(pair.x, pair.y);
            }

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
