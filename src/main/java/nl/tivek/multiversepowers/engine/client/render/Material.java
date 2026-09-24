package nl.tivek.multiversepowers.engine.client.render;

/**
 * The colours one kind of energy is drawn in by the {@link ConstructPainter}: every power brings its own, and the
 * painter draws every shape and every light in it. Green Lantern's hard light is green; another power can be red,
 * gold, blue or anything else with the same shapes and the same light.
 *
 * @param mass the solid body of a construct, at full light: the painter shades it darker on the sides that face away
 * @param edge the bright lines along its edges, and the rays of a flare
 * @param glow the soft glow around it, added on top of what is behind it like light
 * @param hot  the white-hot heart of fresh light: the middle of a flare or a beam, and a construct flaring up as it
 *             strikes
 */
public record Material(int mass, int edge, int glow, int hot) {
}
