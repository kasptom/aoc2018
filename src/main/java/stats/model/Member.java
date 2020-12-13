package stats.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "id")
public class Member {
    static final String MEMBER_ROW_FORMAT = "%-25s %12d %12d";

    @JsonProperty
    long id;

    @JsonProperty
    int stars;

    @JsonProperty
    String name;

    @JsonProperty("local_score")
    int localScore;

    List<Integer> scoreHistory = new ArrayList<>();

    @JsonProperty("global_score")
    long globalScore;

    @JsonProperty("last_star_ts")
    long lastStartTimestamp;

    @JsonProperty("completion_day_level")
    HashMap<Integer, Day> completionDayLevel;

    List<List<Integer>> daysRanks;
    List<List<Integer>> dayPoints;

    public int getStarsForDay(int dayIdx) {
        if (!completionDayLevel.containsKey(dayIdx + 1)) {
            return 0;
        }
        Day day = completionDayLevel.get(dayIdx + 1);
        return day.getStarsCount();
    }

    public String getTime(int dayIdx, int part) {
        var day = completionDayLevel.get(dayIdx + 1);
        return day == null ? "N/A" : day.getTimestampStr(part);
    }

    public String getAnonymous() {
        return "(anonymous user #" + id + ")";
    }

    public int getDayRank(int dayIdx, int partIdx) {
        return daysRanks.get(dayIdx).get(partIdx);
    }

    public int getDayPoints(int dayIdx, int partIdx) {
        return dayPoints.get(dayIdx).get(partIdx);
    }

    public int getPointsAtDay(int dayIdx) {
        return dayPoints.subList(0, dayIdx + 1)
                .stream()
                .flatMap(List::stream)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getRankAtDay(int dayIdx) {
        return 123;
    }
}
