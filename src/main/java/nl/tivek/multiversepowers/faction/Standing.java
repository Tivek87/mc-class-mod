package nl.tivek.multiversepowers.faction;

public enum Standing {
    HOSTILE(0xFF3A30),
    NEUTRAL(0xFFD23A),
    FRIENDLY(0x4CFF6E);

    private final int rgb;

    Standing(int rgb) {
        this.rgb = rgb;
    }

    public int rgb() {
        return this.rgb;
    }
}
