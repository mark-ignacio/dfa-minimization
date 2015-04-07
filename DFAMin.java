import java.awt.*;
import java.util.*;
import java.util.Queue;

public class DFAMin {

    public static void main(String[] args) {
        new DFAMin();
    }

    public DFAMin() {
        Scanner inFile = new Scanner(System.in);
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
                } else {
                    formatter.format("%d\n", val);
                }
            }

            // states
            for (DFAState state : states) {
                for (int i = 0; i < state.transitions.size(); i++) {
                    Integer val = state.transitions.get(i);
                    if (i < state.transitions.size() - 1) {
                        formatter.format("%d ", val);
                    } else {
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
                    Arrays.fill(D[i], false);
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
                    // only pairs that are as of yet indistinguishable
                    if (D[i][j]) {
                        continue;
                    }

                    DFAState qi = states[i];
                    DFAState qj = states[j];

                    // one of the things being compared is unreachable
                    if (qi == null || qj == null) {
                        continue;
                    }

                    // helps emulate "for any"
                    boolean distinguished = false;
                    for (int k = 0; k < qi.transitions.size(); k++) {
                        int m = qi.transitions.get(k);
                        int n = qj.transitions.get(k);

                        // if on the same letter, qm and qn move to distinguishable states
                        if (D[m][n] || D[n][m]) {
                            dist(i, j);
                            distinguished = true;
                            break;
                        }
                    }

                    if (!distinguished) {
                        // qm and qn are indistinguishable
                        for (int k = 0; k < qi.transitions.size(); k++) {
                            int m = qi.transitions.get(k);
                            int n = qj.transitions.get(k);

                            if (m < n && !(i == m && j == n)) {
                                S.get(m).get(n).add(new Point(i, j));
                            } else if (m > n && !(i == n && j == m)) {
                                S.get(n).get(m).add(new Point(i, j));
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
            HashMap<Integer, Integer> merged = new HashMap<Integer, Integer>();
            ArrayList<ArrayList<Integer>> mergeGroups = new ArrayList<ArrayList<Integer>>();
            for (int i = 0; i < D.length; i++) {
                if (merged.get(i) != null || states[i] == null) {
                    continue;
                }

                DFAState state = states[i];

                ArrayList<Integer> toMerge = new ArrayList<Integer>();
                for (int j = i + 1; j < D.length; j++) {
                    if (!D[i][j]) {
                        toMerge.add(j);
                        merged.put(j, i);
                    }
                }

                // renumber existing transitions
                for (int j = 0; j < state.transitions.size(); j++) {
                    Integer transition = state.transitions.get(j);
                    if (merged.containsKey(transition)) {
                        state.transitions.set(j, merged.get(transition));
                    }
                }

                if (acceptStates.contains(i)) {
                    newAcceptStates.add(i);
                }
                toMerge.add(i);
                mergeGroups.add(toMerge);
                newStates.add(state);
            }

            renumberStates(mergeGroups, newAcceptStates);

            // replace attributes
            DFAState[] newStatesArray = new DFAState[newStates.size()];
            newStatesArray = newStates.toArray(newStatesArray);
            states = newStatesArray;
            acceptStates = newAcceptStates;
        }

        private void renumberStates(ArrayList<ArrayList<Integer>> groups, HashSet<Integer> newAcceptStates) {
            for (int i = 0; i < groups.size(); i++) {
                ArrayList<Integer> group = groups.get(i);
                for (DFAState state : states) {
                    if (state == null) {
                        continue;
                    }
                    for (int j = 0; j < state.transitions.size(); j++) {
                        Integer val = state.transitions.get(j);
                        if (group.contains(val)) {
                            state.transitions.set(j, i);
                        }
                    }
                }
                for (Integer state : new HashSet<Integer>(newAcceptStates)) {
                    if (group.contains(state)) {
                        newAcceptStates.remove(state);
                        newAcceptStates.add(i);
                    }
                }
            }
        }

        private void dist(int i, int j) {
            _dist(new Point(i, j), new HashSet<Point>());
        }

        private void _dist(Point point, HashSet<Point> visited) {
            if (visited.contains(point)) {
                return;
            }

            int i = point.x, j = point.y;
            D[i][j] = true;
            visited.add(point);
            for (Point pair : S.get(i).get(j)) {
                _dist(pair, visited);
            }
        }

        public void trim() {
            // do a BFS for unreachable nodes, cut them out
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
