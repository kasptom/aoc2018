package year2019;

import aoc.IAocTask;
import year2019.utils.Aoc2019Utils;
import year2019.utils.Pair;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Day18V2 implements IAocTask {

    private static final List<Pair<Integer>> MOVES = Aoc2019Utils.createMoves();
    private static final boolean ENABLE_DEBUG_PRINT = false;

    String[][] maze;
    HashSet<Pair<Integer>> vertices;
    HashMap<String, Pair<Integer>> keysAndGatesCoordinates;
    HashMap<Pair<Integer>, String> coordinatesToKeysAndGates;

    Map<String, List<Path>> keyOrGateToPaths;
    Map<String, Set<String>> keyDependencies;

    int shortestPath;
    List<String> keyOrderForShortestPath = null;

    @Override
    public String getFileName() {
        return "aoc2019/input_18_small_136.txt";
    }

    @Override
    public void solvePartOne(List<String> lines) {
        loadMaze(lines);
        shortestPath = Integer.MAX_VALUE;

        keyOrGateToPaths = keysAndGatesCoordinates
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        keyValue -> findShortestPaths(keyValue.getKey())
                ));

        printKeysOrGatesToPaths();

        createKeyDependencies();

        List<String[]> keyOrders = generatePossibleKeyPermutations();
        int minPathCost = Integer.MAX_VALUE;
        String[] minPath = null;

        for (String[] keyOrder : keyOrders) {
            int pathCost = computeCost(keyOrder, keyOrGateToPaths);
            if (pathCost < minPathCost) {
                minPathCost = pathCost;
            }
            minPath = keyOrder;
        }
        System.out.printf("Min cost: %d for path %s%n", minPathCost, Arrays.toString(minPath));
    }

    private void createKeyDependencies() {
        List<Path> paths = keyOrGateToPaths.get("@");
        keyDependencies = new HashMap<>();

        keysAndGatesCoordinates.keySet()
                .stream()
                .filter(key -> key.matches("[a-z]"))
                .forEach(key -> updateKeyDependencies(key, paths));

        int dependenciesCountLimit = keyDependencies.size() - 1;
        keyDependencies.forEach((keyName, requiredKeys) -> {
            int dependenciesCount = 0;
            Set<String> dependencies = new HashSet<>(requiredKeys);
            Queue<String> dependenciesQueue = new LinkedBlockingQueue<>(dependencies);
            while (!dependenciesQueue.isEmpty()) {
                dependenciesCount++;
                if (dependenciesCount > dependenciesCountLimit) throw new RuntimeException("Cyclic dependency");
                String requiredKey = dependenciesQueue.poll();
                dependenciesQueue.addAll(keyDependencies.get(requiredKey));
                dependencies.addAll(keyDependencies.get(requiredKey));
            }
            keyDependencies.put(keyName, dependencies);
        });
    }

    private void updateKeyDependencies(String keyName, List<Path> paths) {
        Set<String> keyDependency = new HashSet<>();
        keyDependencies.put(keyName, keyDependency);

        for (Path path : paths) {
            List<String> keysOrGates = path.keysAndGatesOrdered;
            if (!keysOrGates.contains(keyName)) {
                continue;
            }

            for(int i = 0; i< keysOrGates.size(); i++) {
                if (keysOrGates.get(i).equals(keyName)) {
                    for (int j = 0; j < i; j++) {
                        if (keysOrGates.get(j).matches("[A-Z]")) {
                            keyDependency.add(keysOrGates.get(j).toLowerCase()); // get the key name required to open the blocking gate
                        }
                    }
                }
            }
        }
    }

    private List<String[]> generatePossibleKeyPermutations() {
        List<String> keyNames = keysAndGatesCoordinates
                .keySet()
                .stream().filter(key -> key.matches("[a-z]"))
                .collect(Collectors.toCollection(ArrayList::new));

        List<String[]> permutations = new ArrayList<>();

        for (String keyName : keyNames) {
            if (isPossibleAndBetterThanCurrentBest(keyName, Collections.singletonList("@"))) {
                List<String> newSelectedKeys = new ArrayList<>();
                newSelectedKeys.add(keyName);
                generatePossibleKeyPermutations(permutations, newSelectedKeys, keyNames);
            }
        }

        return permutations;
    }

    private void generatePossibleKeyPermutations(List<String[]> permutations, List<String> selectedKeys, List<String> allKeys) {
        if (selectedKeys.size() > 1 && shortestPath <= computeCost(selectedKeys, keyOrGateToPaths, selectedKeys.size())) {
            return;
        }

        if (selectedKeys.size() == allKeys.size()) {
            String[] keys = new String[allKeys.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = selectedKeys.get(i);
            }
            shortestPath = computeCost(keys, keyOrGateToPaths);
            keyOrderForShortestPath = selectedKeys;
            System.out.printf("New best: %d, %s%n", shortestPath, keyOrderForShortestPath);
            permutations.add(keys);
            return;
        }

        List<String> unusedKeys = allKeys.stream().filter(key -> !selectedKeys.contains(key)).collect(Collectors.toCollection(ArrayList::new));
        for (String unusedKey : unusedKeys) {
            if (isPossibleAndBetterThanCurrentBest(unusedKey, selectedKeys)) {
                List<String> newSelectedKeys = new ArrayList<>(selectedKeys);
                newSelectedKeys.add(unusedKey);
                generatePossibleKeyPermutations(permutations, newSelectedKeys, allKeys);
            }
        }
    }

    /**
     * Checks if selected keys allow to get the unused key
     *
     * @param unusedKey    unused key
     * @param selectedKeys already 'found' keys
     * @return true if it is possible, otherwise false
     */
    private boolean isPossibleAndBetterThanCurrentBest(String unusedKey, List<String> selectedKeys) {
        Set<String> requiredKeys = keyDependencies.get(unusedKey);
        if (!selectedKeys.containsAll(requiredKeys)) {
            return false;
        }
        List<Path> paths = keyOrGateToPaths.get(selectedKeys.get(selectedKeys.size() - 1));
        Path toKey = paths.stream().filter(path -> path.to.equals(unusedKey)).findFirst().orElse(null);
        assert toKey != null;

        HashSet<String> collectedKeys = new HashSet<>(selectedKeys);
        for (int i = 0; i < toKey.keysAndGatesOrdered.size(); i++) {
            String nextKeyOrGate = toKey.keysAndGatesOrdered.get(i);
            if (nextKeyOrGate.matches("[a-z]")) {
                collectedKeys.add(nextKeyOrGate);
            } else if (nextKeyOrGate.matches("[A-Z]") && !collectedKeys.contains(nextKeyOrGate.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    private int computeCost(String[] keyOrder, Map<String, List<Path>> keyOrGateToPath) {
        return computeCost(Arrays.stream(keyOrder).collect(Collectors.toList()), keyOrGateToPath, keyOrder.length);
    }

    private int computeCost(List<String> keyOrder, Map<String, List<Path>> keyOrGateToPath, int length) {
        int cost = 0;
        for (int i = 0; i < length; i++) {
            String from = i == 0 ? "@" : keyOrder.get(i-1);
            String to = keyOrder.get(i);
            cost += Objects.requireNonNull(keyOrGateToPath.get(from)
                    .stream()
                    .filter(path -> path.to.equals(to))
                    .findFirst().orElse(null)).path.size();
        }
        return cost;
    }

    @Override
    public void solvePartTwo(List<String> lines) {

    }

    private void loadMaze(List<String> lines) {
        int height = lines.size();
        int width = lines.get(0).length();
        maze = new String[height][width];
        vertices = new HashSet<>();
        keysAndGatesCoordinates = new HashMap<>();
        coordinatesToKeysAndGates = new HashMap<>();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                maze[i][j] = lines.get(i).substring(j, j + 1);
                if (!maze[i][j].equals("#")) {
                    vertices.add(new Pair<>(j, i));
                }
                if (maze[i][j].matches("[A-Z]|[a-z]|@")) {
                    keysAndGatesCoordinates.put(maze[i][j], new Pair<>(j, i));
                    coordinatesToKeysAndGates.put(new Pair<>(j, i), maze[i][j]);
                }
            }
        }
    }

    /**
     * Finds the shortest paths (Dijkstra) from the root to all other nodes in the grid based labyrinth.
     * Collects the data about found keys
     *
     * @param root root of the paths
     * @return paths
     */
    private List<Path> findShortestPaths(String root) {
        HashMap<Pair<Integer>, Integer> unvisitedCost = new HashMap<>();
        vertices.forEach(vertex -> unvisitedCost.put(vertex, Integer.MAX_VALUE)); // all with max cost

        HashMap<Pair<Integer>, Pair<Integer>> childToParent = new HashMap<>();

        int pathCost = 0;
        Pair<Integer> currentPosition = keysAndGatesCoordinates.get(root);
        unvisitedCost.put(currentPosition, pathCost);

        while (!unvisitedCost.isEmpty() && currentPosition != null) {
            pathCost = unvisitedCost.remove(currentPosition);
            pathCost++;

            for (Pair<Integer> move : MOVES) {
                Pair<Integer> nextPosition = new Pair<>(currentPosition.x + move.x, currentPosition.y + move.y);
                Integer cost = unvisitedCost.get(nextPosition);
                if (cost != null && cost > pathCost) {
                    childToParent.put(nextPosition, currentPosition);
                    unvisitedCost.put(nextPosition, pathCost);
                }
            }

            currentPosition = getUnvisitedNodeWithLowestCost(unvisitedCost);
        }

        return mapToPaths(root, childToParent);
    }

    private Pair<Integer> getUnvisitedNodeWithLowestCost(HashMap<Pair<Integer>, Integer> unvisitedCost) {
        Integer lowestCost = unvisitedCost.values().stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
        if (lowestCost == Integer.MAX_VALUE) {
            return null;
        }
        return unvisitedCost
                .keySet()
                .stream()
                .filter(key -> unvisitedCost.get(key).equals(lowestCost))
                .findFirst().orElse(null);
    }

    /**
     * Each path starts with the root and ends with one of the coordinates in {@link Day18V2#keysAndGatesCoordinates}
     *
     * @param root          name of the root node
     * @param childToParent shortest paths from the root to all other available non-wall nodes
     * @return list of paths that end with key or gate (or @)
     */
    private List<Path> mapToPaths(String root, HashMap<Pair<Integer>, Pair<Integer>> childToParent) {
        return keysAndGatesCoordinates
                .keySet()
                .stream()
                .filter(keyOrGate -> !keyOrGate.equals(root))
                .map(keyOrGate -> {
                    Path path = new Path(root, keyOrGate);
                    Pair<Integer> rootCoordinates = keysAndGatesCoordinates.get(root);
                    Pair<Integer> currentCoordinates = keysAndGatesCoordinates.get(keyOrGate);
                    while (!currentCoordinates.equals(rootCoordinates)) {
                        path.path.add(currentCoordinates);
                        if (coordinatesToKeysAndGates.containsKey(currentCoordinates)) {
                            path.keysAndGatesOrdered.add(coordinatesToKeysAndGates.get(currentCoordinates));
                        }
                        currentCoordinates = childToParent.get(currentCoordinates);
                    }
                    Collections.reverse(path.path);
                    Collections.reverse(path.keysAndGatesOrdered);
                    return path;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void printKeysOrGatesToPaths() {
        if (!ENABLE_DEBUG_PRINT) {
            return;
        }

        keyOrGateToPaths.forEach((key, value) -> {
            System.out.printf("%n---Paths from: %s---%n", key);
            value.forEach(System.out::println);
        });
    }

    static class Path {

        String from;
        String to;
        List<Pair<Integer>> path;
        List<String> keysAndGatesOrdered;

        public Path(String from, String to) {
            this.from = from;
            this.to = to;
            path = new ArrayList<>();
            keysAndGatesOrdered = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "Path{" +
                    "from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    ", path=" + path +
                    ", keysAndGatesOrdered=" + keysAndGatesOrdered +
                    '}';
        }
    }
}
