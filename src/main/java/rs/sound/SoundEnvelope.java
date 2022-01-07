package rs.sound;

import java.util.ArrayList;
import java.util.List;

public class SoundEnvelope {
    public int form;
    public int start;
    public int end;
    public List<Segment> segments = new ArrayList<>();

    public SoundEnvelope() {
        segments.add(new Segment(0, 0xffff));
        segments.add(new Segment(0, 0xffff));
    }

    public static class Segment {
        public final int duration;
        public final int phase;

        public Segment(int duration, int phase) {
            this.duration = duration;
            this.phase = phase;
        }
    }
}
