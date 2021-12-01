package me.iska.jserver.util;

/**
 * ANSI escape codes for console colours.
 *
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
public class Colour {

    /*
    "\u001b[s"             // save cursor position
    "\u001b[5000;5000H"    // move to col 5000 row 5000
    "\u001b[6n"            // request cursor position
    "\u001b[u"             // restore cursor position

     */

    public static final String RESET = "\u001b[0m";

    public static class Foreground {
        public static final String BLACK = "\u001b[30m";
        public static final String RED = "\u001b[31m";
        public static final String GREEN = "\u001b[32m";
        public static final String YELLOW = "\u001b[33m";
        public static final String BLUE = "\u001b[34m";
        public static final String MAGENTA = "\u001b[35m";
        public static final String CYAN = "\u001b[36m";
        public static final String WHITE = "\u001b[37m";

        public static final String BRIGHT_BLACK = "\u001b[30;1m";
        public static final String BRIGHT_RED = "\u001b[31;1m";
        public static final String BRIGHT_GREEN = "\u001b[32;1m";
        public static final String BRIGHT_YELLOW = "\u001b[33;1m";
        public static final String BRIGHT_BLUE = "\u001b[34;1m";
        public static final String BRIGHT_MAGENTA = "\u001b[35;1m";
        public static final String BRIGHT_CYAN = "\u001b[36;1m";
        public static final String BRIGHT_WHITE = "\u001b[37;1m";
    }

    public static class Background {
        public static final String BLACK = "\u001b[40m";
        public static final String RED = "\u001b[41m";
        public static final String GREEN = "\u001b[42m";
        public static final String YELLOW = "\u001b[43m";
        public static final String BLUE = "\u001b[44m";
        public static final String MAGENTA = "\u001b[45m";
        public static final String CYAN = "\u001b[46m";
        public static final String WHITE = "\u001b[47m";

        public static final String BRIGHT_BLACK = "\u001b[40;1m";
        public static final String BRIGHT_RED = "\u001b[41;1m";
        public static final String BRIGHT_GREEN = "\u001b[42;1m";
        public static final String BRIGHT_YELLOW = "\u001b[43;1m";
        public static final String BRIGHT_BLUE = "\u001b[44;1m";
        public static final String BRIGHT_MAGENTA = "\u001b[45;1m";
        public static final String BRIGHT_CYAN = "\u001b[46;1m";
        public static final String BRIGHT_WHITE = "\u001b[47;1m";
    }

    public static class Style {
        public static final String BOLD = "\u001b[1m";
        public static final String UNDERLINED = "\u001b[4m";
        public static final String INVERTED = "\u001b[7m";
    }

    public static class Cursor {
        public static final String HIDE = "\u001b[?25l";
        public static final String VISIBLE = "\u001b[?25h";

        public static String up(int n) {
            return String.format("\u001b[%dA", n);
        }

        public static String down(int n) {
            return String.format("\u001b[%dB", n);
        }

        public static String left(int n) {
            return String.format("\u001b[%dD", n);
        }

        public static String right(int n) {
            return String.format("\u001b[%dC", n);
        }
    }

    public static class Clear {
        private static final String SCREEN_BASE = "\u001b[%dJ";
        private static final String LINE_BASE = "\u001b[%dK";

        public static final String SCREEN_TO_CURSOR = String.format(SCREEN_BASE, 0);
        public static final String SCREEN_FROM_CURSOR = String.format(SCREEN_BASE, 1);
        public static final String SCREEN_FULL = String.format(SCREEN_BASE, 2);

        public static final String LINE_TO_CURSOR = String.format(LINE_BASE, 0);
        public static final String LINE_FROM_CURSOR = String.format(LINE_BASE, 1);
        public static final String LINE_FULL = String.format(LINE_BASE, 2);
    }

    public static class Position {
        public static final String SAVE_POSITION = "\u001b[s";
        public static final String RESTORE_POSITION = "\u001b[u";
        public static final String REQUEST_POSITION = "\u001b[6n";

        public static String setColumn(int col) {
            return String.format("\u001b[%dG", col);
        }

        public static String setPosition(int row, int col) {
            return String.format("\u001b[%d;%dH", row, col);
        }
    }
}