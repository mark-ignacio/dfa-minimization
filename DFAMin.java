import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

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
            ArrayList<Integer> transitions = new ArrayList<Integer>();
            for (String num : line) {
                transitions.add(Integer.parseInt(num));
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

            formatter.format("%d %d\n", states.length, alphabet.length);
            
            // accept states
            ArrayList<Integer> acceptable = new ArrayList<Integer>(acceptStates);
            Collections.sort(acceptable);
            formatter.format("%d ", acceptable.size());
            for (int i = 0; i < acceptable.size(); i++) {
                Integer val = acceptable.get(i);
                if (i < acceptable.size() - 1) {
                    formatter.format("%d ", val);
                }
                else {
                    formatter.format("%d\n", val);
                }
            }
            
            // states
            for (DFAState state: states) {
                for (int i = 0; i < state.transitions.size(); i++) {
                    Integer val = state.transitions.get(i);
                    if (i < state.transitions.size() - 1) {
                        formatter.format("%d ", val);
                    }
                    else {
                        formatter.format("%d\n", val);
                    }
                }
            }

            return sb.toString();
        }

        public void minimize() {
            // Sudkamp minimization algorithm

            // 1. init structures
            D = new boolean[states.length][states.length];
            S = new ArrayList<ArrayList<HashSet<Point>>>();  // lol

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < states.length; i++) {
                ArrayList<HashSet<Point>> innerList = new ArrayList<HashSet<Point>>();

                //noinspection ForLoopReplaceableByForEach
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
                    for (int k = 0; k < qi.transitions.size(); k++) {
                        int qm = qi.transitions.get(k);
                        int qn = qj.transitions.get(k);

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
                        for (int k = 0; k < qi.transitions.size(); k++) {
                            int qm = qi.transitions.get(k);
                            int qn = qj.transitions.get(k);

                            if (qm < qn && !(i == qm && j == qn)) {
                                S.get(qm).get(qn).add(new Point(qm, qn));
                            } else if (qm > qn && !(i == qn && j == qm)) {
                                S.get(qn).get(qm).add(new Point(qn, qm));
                            }
                        }
                    }

                }
            }

            mergeStates();
        }

        private void mergeStates() {
            // merge states together by smallest equivalent
            ArrayList<DFAState> newStates = new ArrayList<DFAState>();
            HashSet<Integer> newAcceptStates = new HashSet<Integer>();
            HashSet<Integer> merged = new HashSet<Integer>();
            for (int i = 0; i < D.length; i++) {
                if (merged.contains(i)) {
                    continue;
                }

                DFAState state = states[i];

                ArrayList<Integer> toMerge = new ArrayList<Integer>();
                for (int j = i + 1; j < D.length; j++) {
                    if (!D[i][j] && !D[j][i]) {
                        toMerge.add(j);
                    }
                }

                // merge states (if applicable)
                for (int mergeState : toMerge) {
                    for (int j : states[mergeState].transitions) {
                        if (!state.transitions.contains(j)) {
                            state.transitions.add(j);
                        }
                    }

                    merged.add(mergeState);
                }
                if (acceptStates.contains(i)) {
                    newAcceptStates.add(i);
                }
                newStates.add(state);
            }

            DFAState[] newStatesArray = new DFAState[newStates.size()];
            newStatesArray = newStates.toArray(newStatesArray);
            states = newStatesArray;
            acceptStates = newAcceptStates;
        }

        private void dist(int i, int j) {
            // spoilers, recursion on this is bad
            D[i][j] = true;

            // a BFS in a ~different form~
            HashSet<Point> distinctSet = new HashSet<Point>();
            Queue<Point> pointQueue = new LinkedList<Point>();
            for (Point pair : S.get(i).get(j)) {
                pointQueue.add(pair);
                distinctSet.add(pair);
            }
            
            while (!pointQueue.isEmpty()) {
                Point pair = pointQueue.remove();
                D[pair.x][pair.y] = true;
                for (Point suspicious: S.get(i).get(j)) {
                    if (!distinctSet.contains(suspicious)) {
                        pointQueue.add(suspicious);
                    }
                }
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
        public ArrayList<Integer> transitions;

        public DFAState(ArrayList<Integer> transitions) {
            this.transitions = transitions;
        }
    }
}
