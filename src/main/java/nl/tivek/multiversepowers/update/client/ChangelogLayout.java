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

final class ChangelogLayout {
    static final int BODY = 0xD4D8DE;
    static final int MUTED = 0x8C95A3;
    private static final int CODE = 0x9AD7FF;
    private static final int INSTALLED_CHIP = 0x9AA6B5;
    private static final int LINK = 0x7CC4FF;
    private static final int LINE = 10;
    private static final int BULLET_INDENT = 11;
    private static final float VERSION_SCALE = 1.4F;
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)]\\([^)]*\\)");

    interface Block {
        int height();

        void draw(GuiGraphics graphics, int x, int y);
    }

    private ChangelogLayout() {
    }

    static List<Block> build(Font font, List<Release> releases, String installed, @Nullable Component note, int width) {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < releases.size(); i++) {
            if (i > 0) {
                blocks.add(divider(width));
            }
            Release release = releases.get(i);
            boolean isInstalled = Release.compare(release.version(), installed) == 0;
            Component chip = isInstalled ? UpdateManagerScreen.text("changelog.installed")
                    : i == 0 ? UpdateManagerScreen.text("changelog.newest") : null;
            blocks.add(header(font, release, chip, isInstalled ? INSTALLED_CHIP : UpdatePopup.ACCENT, width));
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
        int accent = colorOf("");
        StringBuilder bullet = null;
        StringBuilder paragraph = null;
        boolean any = false;
        for (String raw : notes.replace("\r", "").split("\n")) {
            String line = raw.strip();
            boolean startsBlock = line.isEmpty() || line.startsWith("#") || line.startsWith("- ")
                    || line.startsWith("* ") || line.equals("---") || line.equals("***");
            if (startsBlock) {
                any |= flush(font, bullet, paragraph, accent, width, blocks);
                bullet = null;
                paragraph = null;
            }
            if (line.equals("---") || line.equals("***")) {
                break;
            } else if (line.startsWith("#")) {
                String heading = line.replaceFirst("^#+\\s*", "");
                accent = colorOf(heading);
                blocks.add(label(font, heading, accent));
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
        any |= flush(font, bullet, paragraph, accent, width, blocks);
        if (!any) {
            blocks.add(text(font, UpdateManagerScreen.text("changelog.empty").copy().withColor(MUTED), 0, -1, width));
        }
    }

    private static boolean flush(Font font, StringBuilder bullet, StringBuilder paragraph, int accent, int width,
            List<Block> blocks) {
        if (bullet != null) {
            blocks.add(text(font, inline(bullet.toString()), BULLET_INDENT, accent, width));
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

    private static Block header(Font font, Release release, @Nullable Component label, int chipColor, int width) {
        Component version = Component.literal("v" + release.version()).withStyle(ChatFormatting.BOLD);
        Component date = Component.literal(UpdateManagerScreen.DATE.format(release.published()));
        Component chip = label == null ? null : label.copy().withStyle(ChatFormatting.BOLD);
        return new Block() {
            @Override
            public int height() {
                return 24;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                int versionWidth = (int) Math.ceil(font.width(version) * VERSION_SCALE);
                if (chip != null) {
                    int chipX = x + versionWidth + 7;
                    GuiShapes.roundRect(graphics, chipX, y + 5, font.width(chip) + 8, 11, 3.0F, 0xFF000000 | chipColor);
                    GuiShapes.flush(graphics);
                    graphics.drawString(font, chip, chipX + 4, y + 7, 0xFF0E2A1C, false);
                }
                graphics.pose().pushPose();
                graphics.pose().translate(x, y + 4, 0.0F);
                graphics.pose().scale(VERSION_SCALE, VERSION_SCALE, 1.0F);
                graphics.drawString(font, version, 0, 0, 0xFFFFFFFF, true);
                graphics.pose().popPose();
                graphics.drawString(font, date, x + width - font.width(date), y + 7, 0xFF000000 | MUTED, false);
            }
        };
    }

    private static Block label(Font font, String heading, int color) {
        Component text = Component.literal(heading.toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD);
        return new Block() {
            @Override
            public int height() {
                return 18;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                GuiShapes.roundRect(graphics, x, y + 3, font.width(text) + 10, 12, 3.0F, GuiShapes.fade(color, 0.22F));
                GuiShapes.roundRect(graphics, x, y + 3, 2.0F, 12, 1.0F, 0xFF000000 | color);
                GuiShapes.flush(graphics);
                graphics.drawString(font, text, x + 6, y + 5, 0xFF000000 | color, false);
            }
        };
    }

    private static Block text(Font font, Component text, int indent, int bullet, int width) {
        List<FormattedCharSequence> lines = font.split(text, width - indent);
        return new Block() {
            @Override
            public int height() {
                return lines.size() * LINE + 4;
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
