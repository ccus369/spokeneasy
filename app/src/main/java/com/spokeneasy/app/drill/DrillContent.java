package com.spokeneasy.app.drill;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DrillContent {

    public enum DrillType {
        SUBSTITUTION("substitution", "替换练习"),
        TRANSFORMATION("transformation", "转换练习"),
        EXPANSION("expansion", "扩展练习"),
        RESPONSE("response", "问答练习");

        public final String key;
        public final String labelCn;

        DrillType(String key, String labelCn) {
            this.key = key;
            this.labelCn = labelCn;
        }

        public static DrillType fromKey(String key) {
            for (DrillType t : values()) {
                if (t.key.equals(key)) return t;
            }
            return SUBSTITUTION;
        }
    }

    public static class DrillStep {
        public final String id;
        public final String base;
        public final String cue;
        public final String expected;
        public final String hintCn;

        DrillStep(JSONObject obj) throws JSONException {
            id = obj.getString("id");
            base = obj.optString("base", "");
            cue = obj.optString("cue", "");
            expected = obj.optString("expected", "");
            hintCn = obj.optString("hint_cn", "");
        }
    }

    public static class DrillSet {
        public final String id;
        public final String grammarPoint;
        public final String grammarPointEn;
        public final DrillType drillType;
        public final int difficulty;
        public final String instructionsCn;
        public final String progressLabel;
        public final List<DrillStep> steps;

        DrillSet(JSONObject obj) throws JSONException {
            id = obj.getString("id");
            grammarPoint = obj.getString("grammar_point");
            grammarPointEn = obj.optString("grammar_point_en", "");
            drillType = DrillType.fromKey(obj.optString("drill_type", "substitution"));
            difficulty = obj.optInt("difficulty", 1);
            instructionsCn = obj.optString("instructions_cn", "");
            progressLabel = obj.optString("progress_label", "");

            JSONArray stepArr = obj.getJSONArray("steps");
            steps = new ArrayList<>(stepArr.length());
            for (int i = 0; i < stepArr.length(); i++) {
                steps.add(new DrillStep(stepArr.getJSONObject(i)));
            }
        }
    }

    /**
     * A grammar point containing one or more DrillSets (different drill types).
     */
    public static class DrillCollection {
        public final String grammarPoint;
        public final String grammarPointEn;
        public final List<DrillSet> sets;

        DrillCollection(JSONObject obj) throws JSONException {
            grammarPoint = obj.getString("grammar_point");
            grammarPointEn = obj.optString("grammar_point_en", "");
            JSONArray setsArr = obj.getJSONArray("sets");
            sets = new ArrayList<>(setsArr.length());
            for (int i = 0; i < setsArr.length(); i++) {
                sets.add(new DrillSet(setsArr.getJSONObject(i)));
            }
        }

        /** Total steps across all sets. */
        public int totalSteps() {
            int count = 0;
            for (DrillSet s : sets) count += s.steps.size();
            return count;
        }
    }

    /** Flatten all steps from a DrillCollection into one list (preserving set order). */
    public static List<DrillStep> flattenSteps(DrillCollection collection) {
        List<DrillStep> all = new ArrayList<>();
        for (DrillSet set : collection.sets) {
            all.addAll(set.steps);
        }
        return all;
    }

    public static DrillCollection parseCollection(String jsonString) throws JSONException {
        return new DrillCollection(new JSONObject(jsonString));
    }
}
