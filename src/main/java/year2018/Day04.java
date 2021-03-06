package year2018;

import aoc.IAocTask;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Day04 implements IAocTask {

    private TreeMap<Date, GuardEvent> guardLog;

    private int laziestGuardId = -1;
    private int mostTimesAsleepMinute = -1;

    @Override
    public String getFileName() {
        return "aoc2018/input_04.txt";
    }

    @Override
    public void solvePartOne(List<String> lines) {
        initializeGuardLog(lines);

        System.out.printf("log size (before filling up): %d\n", guardLog.values().size());

        fillGuardIdsAndSetSleepingStatus();

        printGuardLogs();

        findTheLaziestGuard();
    }

    @Override
    public void solvePartTwo(List<String> lines) {
        initializeGuardLog(lines);

        fillGuardIdsAndSetSleepingStatus();

        findGuardWithHighestSleepPerMinuteRatio();
    }

    private void initializeGuardLog(List<String> lines) {
        guardLog = new TreeMap<>();

        String EVENT_REGEX = "\\[([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2})] ([\\p{Print}\\s]+)";
        Pattern pattern = Pattern.compile(EVENT_REGEX);

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
//                String year = matcher.group(1);
                String month = matcher.group(2);
                String day = matcher.group(3);
                String hour = matcher.group(4);
                String minute = matcher.group(5);

                String eventDetails = matcher.group(6);

                try {
                    addLog(month, day, hour, minute, eventDetails, line);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.printf("Could not find log in line: %s\n", line);
            }
        }
    }

    private void printGuardLogs() {
        for (GuardEvent guardEvent : guardLog.values()) {
            if (!guardEvent.isGenerated) {
                System.out.println(guardEvent.print());
            }
        }
    }

    private void findTheLaziestGuard() {
        Map<Integer, Integer> guardSleepTime = new HashMap<>();
        guardLog.values()
                .forEach(guardEvent -> {
                    if (!guardSleepTime.containsKey(guardEvent.guardId)) {
                        guardSleepTime.put(guardEvent.guardId, 0);    // BEGINS
                    } else if (guardEvent.isSleeping) {
                        guardSleepTime.put(guardEvent.guardId, guardSleepTime.get(guardEvent.guardId) + 1);
                    }
                });

        int maxAsleepTime;
        maxAsleepTime = guardSleepTime.values().stream().max(Integer::compareTo).orElse(-1);

        for (Integer key : guardSleepTime.keySet()) {
            if (guardSleepTime.get(key) == maxAsleepTime) {
                laziestGuardId = key;
            }
        }

        Map<Integer, Integer> sleepMinutes = new HashMap<>();

        List<GuardEvent> laziestGuardAllEvents = guardLog
                .values()
                .stream()
                .filter(guardEvent -> guardEvent.guardId == laziestGuardId)
                .collect(Collectors.toList());

        List<GuardEvent> laziestGuardSleepingEvents = laziestGuardAllEvents
                .stream()
                .filter(guardEvent -> guardEvent.isSleeping)
                .collect(Collectors.toList());

        laziestGuardSleepingEvents
                .forEach(guardEvent -> updateSleepMinutes(sleepMinutes, guardEvent));

        int mostMinuteRepeats = sleepMinutes.values().stream().max(Integer::compareTo).orElse(-1);

        for (Integer minute : sleepMinutes.keySet()) {
            if (sleepMinutes.get(minute) == mostMinuteRepeats) {
                mostTimesAsleepMinute = minute;
                break;
            }
        }

        System.out.println(String.format("laziest guard #%d was sleeping most often during the %d minute (%d)",
                laziestGuardId, mostTimesAsleepMinute, laziestGuardId * mostTimesAsleepMinute));
    }

    private void updateSleepMinutes(Map<Integer, Integer> sleepMinutes, GuardEvent guardEvent) {
        int minute = getMinutes(guardEvent.date);

        if (!sleepMinutes.containsKey(minute)) {
            sleepMinutes.put(minute, 1);
        } else {
            sleepMinutes.put(minute, sleepMinutes.get(minute) + 1);
        }
    }

    private void fillGuardIdsAndSetSleepingStatus() {
        TreeMap<Date, GuardEvent> guardLogNoGaps = new TreeMap<>();

        int currentGuardId = 0;
        for (GuardEvent event : guardLog.values()) {
            if (event.eventType == GuardEventType.BEGINS) {
                currentGuardId = event.guardId;
            } else {
                event.guardId = currentGuardId;
            }

            event = normalizeEvent(event);

            if (!guardLogNoGaps.isEmpty()) {
                Date previousDate = guardLogNoGaps.lastKey();
                GuardEvent previousEvent = guardLogNoGaps.get(previousDate);

                for (int i = 1; i < getNumberOfMinutesToFill(event, previousDate); i++) {
                    Date date = getTime(getMonth(previousDate), getCalendarDate(previousDate), getHours(previousDate), getMinutes(previousDate) + i);
                    GuardEvent toFill = new GuardEvent(date, previousEvent.guardId, previousEvent.eventType != GuardEventType.BEGINS ? previousEvent.eventType : GuardEventType.WAKES_UP);
                    toFill.isGenerated = true;
                    guardLogNoGaps.put(date, toFill);
                }

                guardLogNoGaps.put(event.date, event);
            } else {
                guardLogNoGaps.put(event.date, event);
            }
        }

        guardLog = guardLogNoGaps;
    }

    private GuardEvent normalizeEvent(GuardEvent event) {
        Date date = new Date(event.date.getTime());
        if (getHours(date) != 0) {
            date = setHoursAndMinutes(date);
            date = new Date(date.getTime() + 24 * 60 * 60 * 1000);
        }
        return new GuardEvent(date, event.guardId, event.eventType);
    }

    private Date setHoursAndMinutes(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar.getTime();
    }

    private long getNumberOfMinutesToFill(GuardEvent event, Date previousDate) {
        Date currentDate = event.date;

        if (getCalendarDate(currentDate) != getCalendarDate(previousDate)) {
            return 60 - getMinutes(previousDate);
        } else {
            return (currentDate.getTime() - previousDate.getTime()) / (1000 * 60);
        }
    }

    private void addLog(String month, String day, String hour, String minute, String eventDetails, String rawLine) {
        Date date = getTime(Integer.parseInt(month) - 1, Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(minute));

        int guardId = 0;
        GuardEventType guardEventType = null;
        if (eventDetails.contains("#")) {
            guardId = getGuardIdFromLog(eventDetails);
            guardEventType = GuardEventType.BEGINS;
        } else if (eventDetails.contains("falls asleep")) {
            guardEventType = GuardEventType.FALLS_ASLEEP;
        } else if (eventDetails.contains("wakes up")) {
            guardEventType = GuardEventType.WAKES_UP;
        }

        if (guardLog.containsKey(date)) {
            throw new RuntimeException(String.format("duplicate log - two guards at the same time! %s\n%s\n%s\n", date, rawLine, guardLog.get(date).rawLine));
        }

        guardLog.put(date, new GuardEvent(date, guardId, guardEventType, rawLine));
    }

    private Date getTime(int month, int day, int hour, int minute) {
        return new GregorianCalendar(2018, month, day, hour, minute).getTime();
    }

    private int getGuardIdFromLog(String eventDetails) {
        String numberSubstring = eventDetails.substring(eventDetails.indexOf('#'));
        numberSubstring = numberSubstring.substring(1, numberSubstring.indexOf(' '));

        return Integer.parseInt(numberSubstring);
    }

    private int getMinutes(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }

    class GuardEvent {

        private final String rawLine;

        boolean isGenerated;
        boolean isSleeping;

        GuardEvent(Date date, int guardId, GuardEventType eventType) {
            this(date, guardId, eventType, null);
        }

        GuardEvent(Date date, int guardId, GuardEventType eventType, String rawLine) {
            this.date = date;
            this.guardId = guardId;
            this.eventType = eventType;
            this.isSleeping = eventType == GuardEventType.FALLS_ASLEEP;
            this.isGenerated = false;
            this.rawLine = rawLine;
        }

        Date date;

        int guardId;
        GuardEventType eventType;

        String print() {
            String dateLog = String.format("[%d-%02d-%02d %02d:%02d] ", 1518, getMonth(date), getCalendarDate(date), getHours(date), getMinutes(date));
            String description = eventType == GuardEventType.FALLS_ASLEEP
                    ? "falls asleep" : eventType == GuardEventType.WAKES_UP
                    ? "wakes up" : String.format("Guard #%d begins shift", guardId);
            return isGenerated ?
                    "----> " + dateLog + description : dateLog + description;
        }

    }

    enum GuardEventType {
        BEGINS,
        WAKES_UP,
        FALLS_ASLEEP
    }

    private void findGuardWithHighestSleepPerMinuteRatio() {
        HashMap<Integer, HashMap<Integer, Integer>> guardSleepFrequencies = new HashMap<>();
        guardLog.values()
                .stream()
                .filter(guardEvent -> guardEvent.isSleeping)
                .forEach(guardEvent -> {
                    if (!guardSleepFrequencies.containsKey(guardEvent.guardId)) {
                        guardSleepFrequencies.put(guardEvent.guardId, new HashMap<>());
                        return;
                    }

                    HashMap<Integer, Integer> minutesFrequencies = guardSleepFrequencies.get(guardEvent.guardId);
                    updateSleepMinutes(minutesFrequencies, guardEvent);
                });
        HashMap<Integer, Integer> guardsFavouriteMinute = new HashMap<>();

        guardSleepFrequencies.keySet().forEach(guardId -> guardsFavouriteMinute
                .put(guardId, guardSleepFrequencies.get(guardId)
                        .values()
                        .stream()
                        .max(Integer::compareTo).orElse(-1)));

        int guardWithFavoriteMinute = -1;
        int currentBest = 0;

        for(Integer guardId : guardsFavouriteMinute.keySet()) {
            int guardBestSleepMinute = guardsFavouriteMinute.get(guardId);
            if (currentBest < guardsFavouriteMinute.get(guardId)) {
                currentBest = guardBestSleepMinute;
                guardWithFavoriteMinute = guardId;
            }
        }

        int favouriteMinute = -1;

        for(Integer minute : guardSleepFrequencies.get(guardWithFavoriteMinute).keySet()) {
            if (guardSleepFrequencies.get(guardWithFavoriteMinute).get(minute) == currentBest) {
                favouriteMinute = minute;
                break;
            }
        }



        System.out.printf("Guard most frequently asleep on the same minute (%d): #%d, %d",
                favouriteMinute,
                guardWithFavoriteMinute,
                favouriteMinute * guardWithFavoriteMinute);
    }

    private int getHours(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    private int getCalendarDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DATE);
    }

    private int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH);
    }
}
