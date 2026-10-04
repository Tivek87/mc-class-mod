package nl.tivek.multiversepowers.update.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.WipTag;

// A release's notes laid out to read calmly: each version in bold with the day it came out, its headings (Added, Fixed)
// in their colour and its lines with a quiet dot.
final class ChangelogLayout {
    static final int BODY = 0xD4D8DE;
    static final int MUTED = 0x8C95A3;
    private static final int CODE = 0x9AD7FF;
    private static final int DOT = 0x6E7682;
    private static final int LINK = 0x7CC4FF;
    private static final int LINE = 10;
    private static final int BULLET_INDENT = 11;
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)]\\([^)]*\\)");

    interface Block {
        int height();

        void draw(GuiGraphics graphics, int x, int y);
    }

    private ChangelogLayout() {
    }

    // `banner`: the unfinished-mod banner on top.
    static List<Block> build(Font font, List<Release> releases, String installed, @Nullable Component note, int width,
            boolean banner) {
        List<Block> blocks = new ArrayList<>();
        if (banner) {
            blocks.add(wip(font, width));
        }
        for (int i = 0; i < releases.size(); i++) {
            if (i > 0) {
                blocks.add(divider(width));
            }
            Release release = releases.get(i);
            blocks.add(header(font, release, Release.compare(release.version(), installed) == 0, width));
            notes(font, release.notes(), width, blocks);
        }
        if (note != null) {
            if (!blocks.isEmpty()) {
                blocks.add(divider(width));
            }
            blocks.add(text(font, note.copy().withColor(MUTED), 0, -1, width));
        }
        return blocks;
    }

    private static void notes(Font font, String notes, int width, List<Block> blocks) {
        StringBuilder bullet = null;
        StringBuilder paragraph = null;
        boolean any = false;
        for (String raw : notes.replace("\r", "").split("\n")) {
            String line = raw.strip();
            boolean startsBlock = line.isEmpty() || line.startsWith("#") || line.startsWith("- ")
                    || line.startsWith("* ") || line.equals("---") || line.equals("***");
            if (startsBlock) {
                any |= flush(font, bullet, paragraph, width, blocks);
                bullet = null;
                paragraph = null;
            }
            if (line.equals("---") || line.equals("***")) {
                break;
            } else if (line.startsWith("#")) {
                String heading = line.replaceFirst("^#+\\s*", "");
                blocks.add(label(font, heading, colorOf(heading)));
                any = true;
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                bullet = new StringBuilder(line.substring(2).strip());
            } else if (!line.isEmpty()) {
                if (bullet != null) {
                    bullet.append(' ').append(line);
                } else if (paragraph != null) {
                    paragraph.append(' ').append(line);
                } else {
                    paragraph = new StringBuilder(line);
                }
            }
        }
        any |= flush(font, bullet, paragraph, width, blocks);
        if (!any) {
            blocks.add(text(font, UpdateManagerScreen.text("changelog.empty").copy().withColor(MUTED), 0, -1, width));
        }
    }

    private static boolean flush(Font font, StringBuilder bullet, StringBuilder paragraph, int width,
            List<Block> blocks) {
        if (bullet != null) {
            blocks.add(text(font, inline(bullet.toString()), BULLET_INDENT, DOT, width));
            return true;
        }
        if (paragraph != null) {
            blocks.add(text(font, inline(paragraph.toString()), 0, -1, width));
            return true;
        }
        return false;
    }

    private static int colorOf(String heading) {
        String name = heading.toLowerCase(Locale.ROOT);
        if (name.startsWith("add") || name.startsWith("new")) {
            return 0x6EE7A0;
        } else if (name.startsWith("fix")) {
            return 0xFFB35C;
        } else if (name.startsWith("remov")) {
            return 0xFF7B7B;
        } else if (name.startsWith("chang") || name.startsWith("improv")) {
            return 0x7CC4FF;
        } else if (name.startsWith("unfinished") || name.startsWith("wip")) {
            return WipTag.AMBER;
        }
        return 0xF2C84B;
    }

    static Component inline(String text) {
        MutableComponent out = Component.empty();
        Matcher match = INLINE.matcher(text);
        int last = 0;
        while (match.find()) {
            if (match.start() > last) {
                out.append(Component.literal(text.substring(last, match.start())).withColor(BODY));
            }
            if (match.group(1) != null) {
                out.append(Component.literal(match.group(1)).withStyle(ChatFormatting.BOLD).withColor(0xFFFFFF));
            } else if (match.group(2) != null) {
                out.append(Component.literal(match.group(2)).withColor(CODE));
            } else {
                out.append(Component.literal(match.group(3)).withStyle(ChatFormatting.UNDERLINE).withColor(LINK));
            }
            last = match.end();
        }
        if (last < text.length()) {
            out.append(Component.literal(text.substring(last)).withColor(BODY));
        }
        return out;
    }

    // The version in bold, "yours" after your own, and the day it came out at the right (under it where it is narrow).
    private static Block header(Font font, Release release, boolean yours, int width) {
        Component version = Component.literal(UpdateManagerScreen.name(release.version()))
                .withStyle(ChatFormatting.BOLD);
        Component mark = yours ? UpdateManagerScreen.text("versions.yours") : null;
        int taken = font.width(version) + (mark == null ? 0 : 5 + font.width(mark));
        Component full = Component.literal(UpdateManagerScreen.DATE_TIME.format(release.published()));
        Component date = taken + 8 + font.width(full) <= width ? full
                : Component.literal(UpdateManagerScreen.DATE.format(release.published()));
        boolean below = taken + 8 + font.width(date) > width;
        return new Block() {
            @Override
            public int height() {
                return below ? 25 : 15;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                graphics.drawString(font, version, x, y + 1, 0xFFFFFFFF, false);
                if (mark != null) {
                    graphics.drawString(font, mark, x + font.width(version) + 5, y + 1,
                            0xFF000000 | UpdatePopup.ACCENT, false);
                }
                if (below) {
                    graphics.drawString(font, date, x, y + 12, 0xFF000000 | MUTED, false);
                } else {
                    graphics.drawString(font, date, x + width - font.width(date), y + 1, 0xFF000000 | MUTED, false);
                }
            }
        };
    }

    private static Block label(Font font, String heading, int color) {
        Component text = Component.literal(heading).withStyle(ChatFormatting.BOLD);
        return new Block() {
            @Override
            public int height() {
                return 15;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                graphics.drawString(font, text, x, y + 3, 0xFF000000 | color, false);
            }
        };
    }

    private static Block text(Font font, Component text, int indent, int bullet, int width) {
        List<FormattedCharSequence> lines = font.split(text, width - indent);
        return new Block() {
            @Override
            public int height() {
                return lines.size() * LINE + 5;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                if (bullet >= 0) {
                    GuiShapes.disc(graphics, x + 4.0F, y + 3.5F, 2.0F, 0xFF000000 | bullet);
                    GuiShapes.flush(graphics);
                }
                for (int i = 0; i < lines.size(); i++) {
                    graphics.drawString(font, lines.get(i), x + indent, y + i * LINE, 0xFF000000 | BODY, false);
                }
            }
        };
    }

    private static Block wip(Font font, int width) {
        int height = WipTag.bannerHeight(font, width);
        return new Block() {
            @Override
            public int height() {
                return height + 8;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                WipTag.banner(graphics, font, x, y, width);
            }
        };
    }

    private static Block divider(int width) {
        return new Block() {
            @Override
            public int height() {
                return 16;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                graphics.fill(x, y + 7, x + width, y + 8, 0x30FFFFFF);
            }
        };
    }
}
