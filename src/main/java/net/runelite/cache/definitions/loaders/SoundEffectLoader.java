package net.runelite.cache.definitions.loaders;

import rs.sound.*;
import rs.util.CacheBuffer;

public class SoundEffectLoader {
    public static SoundEffectDefinition readSoundEffect(byte[] data) {
        SoundEffectDefinition effect = new SoundEffectDefinition();
        CacheBuffer in = new CacheBuffer(data);

        for (int i = 0; i < 10; ++i) {
            if ((in.get() & 0xFF) != 0) {
                in.position(in.position() - 1);
                effect.instruments.add(readInstrument(in));
            }
        }

        effect.start = in.getShort() & 0xFFFF;
        effect.end = in.getShort() & 0xFFFF;

        return effect;
    }

    public static AudioFilter readFilter(CacheBuffer in, SoundEnvelope envelope) {
        AudioFilter se = new AudioFilter();

        int var3 = in.get() & 0xFF;
        se.unknown0[0] = var3 >> 4;
        se.unknown0[1] = var3 & 15;
        if (var3 != 0) {
            se.unknown1[0] = in.getShort() & 0xFFFF;
            se.unknown1[1] = in.getShort() & 0xFFFF;
            int var4 = in.get() & 0xFF;

            int var5;
            int var6;
            for (var5 = 0; var5 < 2; ++var5) {
                for (var6 = 0; var6 < se.unknown0[var5]; ++var6) {
                    se.unknown2[var5][0][var6] = in.getShort() & 0xFFFF;
                    se.unknown3[var5][0][var6] = in.getShort() & 0xFFFF;
                }
            }

            for (var5 = 0; var5 < 2; ++var5) {
                for (var6 = 0; var6 < se.unknown0[var5]; ++var6) {
                    if ((var4 & 1 << var5 * 4 << var6) != 0) {
                        se.unknown2[var5][1][var6] = in.getShort() & 0xFFFF;
                        se.unknown3[var5][1][var6] = in.getShort() & 0xFFFF;
                    } else {
                        se.unknown2[var5][1][var6] = se.unknown2[var5][0][var6];
                        se.unknown3[var5][1][var6] = se.unknown3[var5][0][var6];
                    }
                }
            }

            if (var4 != 0 || se.unknown1[1] != se.unknown1[0]) {
                envelope.segments.clear();
                int segments = in.get() & 0xFF;
                for (int var21 = 0; var21 < segments; ++var21) {
                    envelope.segments.add(new SoundEnvelope.Segment(in.getShort() & 0xFFFF, in.getShort() & 0xFFFF));
                }

            }
        } else {
            int[] var7 = se.unknown1;
            se.unknown1[1] = 0;
            var7[0] = 0;
        }

        return se;
    }

    private static Instrument readInstrument(CacheBuffer in) {
        Instrument instrument = new Instrument();

        instrument.pitch = readSoundEnvelope(in);
        instrument.volume = readSoundEnvelope(in);

        if ((in.get() & 0xFF) != 0) {
            in.position(in.position() - 1);
            instrument.pitchModifier = readSoundEnvelope(in);
            instrument.pitchModifierAmplitude = readSoundEnvelope(in);
        }

        if ((in.get() & 0xFF) != 0) {
            in.position(in.position() - 1);
            instrument.volumeMultiplier = readSoundEnvelope(in);
            instrument.volumeMultiplierAmplitude = readSoundEnvelope(in);
        }

        if ((in.get() & 0xFF) != 0) {
            in.position(in.position() - 1);
            instrument.release = readSoundEnvelope(in);
            instrument.attack = readSoundEnvelope(in);
        }

        for (int i = 0; i < 10; ++i) {
            int volume = in.getSpecial2();

            if (volume == 0) {
                break;
            }

            instrument.oscillators.add(new Oscillator(
                    volume,
                    in.getSpecial1(),
                    in.getSpecial2()
            ));
        }

        instrument.delayTime = in.getSpecial2();
        instrument.delayDecay = in.getSpecial2();
        instrument.duration = in.getShort() & 0xFFFF;
        instrument.offset = in.getShort() & 0xFFFF;
        instrument.filterEnvelope = new SoundEnvelope();
        instrument.filter = readFilter(in, instrument.filterEnvelope);

        return instrument;
    }

    private static SoundEnvelope readSoundEnvelope(CacheBuffer in) {
        SoundEnvelope envelope = new SoundEnvelope();
        envelope.form = in.get() & 0xFF;
        envelope.start = in.getInt();
        envelope.end = in.getInt();

        envelope.segments.clear();
        int segments = in.get() & 0xFF;

        for (int i = 0; i < segments; ++i) {
            envelope.segments.add(new SoundEnvelope.Segment(in.getShort() & 0xFFFF, in.getShort() & 0xFFFF));
        }

        return envelope;
    }
}
