package rs.sound;

import java.util.ArrayList;
import java.util.List;

public class Instrument {
    public SoundEnvelope pitch;
    public SoundEnvelope pitchModifier;
    public SoundEnvelope pitchModifierAmplitude;

    public SoundEnvelope volume;
    public SoundEnvelope volumeMultiplier;
    public SoundEnvelope volumeMultiplierAmplitude;

    public SoundEnvelope attack;
    public SoundEnvelope release;

    public int delayTime = 0;
    public int delayDecay = 100;
    public int duration = 500;
    public int offset = 0;

    public AudioFilter filter;
    public SoundEnvelope filterEnvelope;

    public List<Oscillator> oscillators = new ArrayList<>();
}
