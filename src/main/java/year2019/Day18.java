package year2019;

import aoc.IAocTask;
import year2019.utils.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Day18 implements IAocTask {
    private String[][] maze;
    private Set<String> allKeys;
    private static final List<Pair<Integer>> MOVES = createMoves();
    int MAX_STEPS = 900;
    private boolean printEnabled = false;
    private int currentBestPath = Integer.MAX_VALUE;

    @Override
    public String getFileName() {
//        return "aoc2019/input_18.txt"; // TODO detect cycles
        return "aoc2019/input_18_small_132.txt";
    }

    @Override
    public void solvePartOne(List<String> lines) {
        loadMaze(lines);
        int fewestSteps = collectAllKeys();
        System.out.printf("fewest steps to collect all keys: %d%n", fewestSteps);
    }

    private int collectAllKeys() {
        HashSet<String> foundKeys = new HashSet<>();

        Pair<Integer> startPosition = findMazeStartPosition();
        int shortestPath = Integer.MAX_VALUE;
        List<Pair<Integer>> possibleNextPositions = getPossibleNextPositions(null, startPosition, foundKeys);
        int stepsCount = 0;

        printBoardWithCurrentPositionIfEnabled(startPosition, foundKeys, new HashSet<>());

        for (Pair<Integer> position: possibleNextPositions) {
            HashSet<String> openedGatesCopy = new HashSet<>();
            HashSet<String> foundKeysCopy = new HashSet<>(foundKeys);

            Pair<Integer> prevPosition = new Pair<>(startPosition);
            int stepsToOpenAllGates = getStepsToOpenAllGates(prevPosition, position, foundKeysCopy, openedGatesCopy, stepsCount);

            if (stepsToOpenAllGates < shortestPath) {
                shortestPath = stepsToOpenAllGates;
            }
        }
        return shortestPath;
    }

    private void printBoardWithCurrentPositionIfEnabled(Pair<Integer> currentPosition, HashSet<String> foundKeys, HashSet<String> openedGates) {
        if (!printEnabled) {
            return;
        }
        System.out.println();
        String footer = "@(%2d, %2d)=%s\nkeys: %s,\nopened gates: %s";
        String tileValue = "X";
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[0].length; j++) {
                String mazeCell = maze[i][j];

                if (i == currentPosition.y && j == currentPosition.x) {
                    System.out.print("@");
                    tileValue = mazeCell.equals("@") || foundKeys.contains(mazeCell) || openedGates.contains(mazeCell) ? "." : mazeCell;
                } else if (mazeCell.equals("@")) {
                    System.out.print(".");
                } else {
                    System.out.print((foundKeys.contains(mazeCell) || openedGates.contains(mazeCell) ? "*" : mazeCell));
                }
            }
            System.out.println();
        }
        System.out.println(String.format(footer, currentPosition.x, currentPosition.y, tileValue, foundKeys, openedGates));
    }

    private int getStepsToOpenAllGates(Pair<Integer> prevPosition, Pair<Integer> position, HashSet<String> foundKeys, HashSet<String> openedGates, int stepsToCurrentPosition) {
        printBoardWithCurrentPositionIfEnabled(position, foundKeys, openedGates);
        if (stepsToCurrentPosition > MAX_STEPS || currentBestPath < stepsToCurrentPosition) {
            return Integer.MAX_VALUE;
        }
        if (foundKeys.size() == allKeys.size()) {
            if (printEnabled) {
                System.out.printf("steps to current position: %d\n", stepsToCurrentPosition);
            }
            return stepsToCurrentPosition;
        }
        List<Pair<Integer>> possibleNextPositions = getPossibleNextPositions(prevPosition, position, foundKeys);

        HashSet<String> openedGatesCopy = new HashSet<>(openedGates);
        HashSet<String> foundKeysCopy = new HashSet<>(foundKeys);
        String mazePlace = maze[position.y][position.x];
        if (isKeyLocation(mazePlace)) {
            foundKeysCopy.add(mazePlace);
        } else if (isGateLocation(mazePlace)) {
            assert foundKeysCopy.contains(mazePlace.toLowerCase());
            openedGatesCopy.add(mazePlace);
        }

        for (Pair<Integer> nextPos: possibleNextPositions) {
            Pair<Integer> prevPos = new Pair<>(position);
            int stepsToOpenAllGates = getStepsToOpenAllGates(prevPos, nextPos, foundKeysCopy, openedGatesCopy, stepsToCurrentPosition + 1);
            if (stepsToOpenAllGates < currentBestPath) {
                currentBestPath = stepsToOpenAllGates;
            }
        }
        return currentBestPath;
    }

    private List<Pair<Integer>> getPossibleNextPositions(Pair<Integer> prevPosition, Pair<Integer> currentPosition, HashSet<String> foundKeys) {
        List<Pair<Integer>> nextPositions = new ArrayList<>();
        for (Pair<Integer> move : MOVES) {
            if (isMovePossible(prevPosition, currentPosition, move, foundKeys)) {
                nextPositions.add(new Pair<>(currentPosition.x + move.x, currentPosition.y + move.y));
            }
        }
        return nextPositions;
    }

    private boolean isMovePossible(Pair<Integer> prevPosition, Pair<Integer> currentPosition, Pair<Integer> move, HashSet<String> foundKeys) {
        Pair<Integer> nextPosition = new Pair<>(currentPosition.x + move.x, currentPosition.y + move.y);
        String currentMazeCell = maze[currentPosition.y][currentPosition.x];
        String mazeCell = maze[nextPosition.y][nextPosition.x];

        if (nextPosition.equals(prevPosition) && !isNewKeyPosition(foundKeys, currentMazeCell)) {
            return false;
        }

        if (isGateLocation(mazeCell)) {
            return foundKeys.contains(mazeCell.toLowerCase());
        }

        return isFree(mazeCell);
    }

    private boolean isNewKeyPosition(HashSet<String> foundKeys, String mazeCell) {
        return isKeyLocation(mazeCell) && !foundKeys.contains(mazeCell);
    }

    private Pair<Integer> findMazeStartPosition() {
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[0].length; j++) {
                if (maze[i][j].equals("@")) {
                    return new Pair<>(j, i);
                }
            }
        }
        throw new RuntimeException("Could not find the starting position");
    }

    @Override
    public void solvePartTwo(List<String> lines) {

    }

    private void loadMaze(List<String> lines) {
        maze = new String[lines.size()][lines.get(0).length()];
        allKeys = new HashSet<>();

        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[0].length; j++) {
                maze[i][j] = lines.get(i).substring(j, j + 1);
                if (maze[i][j].matches("[a-z]")) {
                    allKeys.add(maze[i][j]);
                }
            }
        }
    }

    private static List<Pair<Integer>> createMoves() {
        List<Pair<Integer>> moves = new ArrayList<>();
        moves.add(new Pair<>(0, -1));
        moves.add(new Pair<>(1, 0));
        moves.add(new Pair<>(0, 1));
        moves.add(new Pair<>(-1, 0));
        return moves;
    }

    private boolean isGateLocation(String mazePlace) {
        return mazePlace.matches("[A-Z]");
    }

    private boolean isKeyLocation(String mazePlace) {
        return mazePlace.matches("[a-z]");
    }

    private boolean isFree(String mazeCell) {
        return mazeCell.matches("[a-z]|\\.|@");
    }
}