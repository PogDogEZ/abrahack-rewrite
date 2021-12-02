package tech.futureclient.message.impl;

import tech.futureclient.message.MessageType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FromMinecraft
{
    private final String unformatted;

    private static final Pattern usernamePattern = Pattern.compile("<[A-Za-z0-9_]+>");
    private static final Pattern toPattern = Pattern.compile("To ([A-Za-z0-9_]+)");
    private static final Pattern whispersPattern = Pattern.compile("[A-Za-z0-9_]+ whispers");
    private static final Pattern joinedPattern = Pattern.compile("([A-Za-z0-9_]+ joined the game)");
    private static final Pattern leftPattern = Pattern.compile("([A-Za-z0-9_]+ left the game)");

    String[] sections;

    public FromMinecraft(String unformatted)
    {
        this.unformatted = unformatted;

        sections = unformatted.split(" ");
    }

    public static String removeLastChar(String s)
    {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }

    public String getSenderName()
    {
        if(isWhisperMessage())
        {
            return sections[0];
        }
        if(isToMessage())
        {
            return removeLastChar(sections[1]);
        }
        if(isNormalMessage())
        {
            String s = sections[0].substring(1, sections[0].length() - 1);
            return s;
        }
        return null;
    }

    public boolean isNormalMessage()
    {
        String[] arrowSeparatorNormal = unformatted.split(">", 2);
        return arrowSeparatorNormal.length >= 2;
    }

    public boolean isWhisperMessage()
    {
        String[] s = unformatted.split(":");
        Matcher matcher = whispersPattern.matcher(s[0]);
        return matcher.matches();
    }

    public boolean isToMessage()
    {
        String[] s = unformatted.split(":");
        Matcher matcher = toPattern.matcher(s[0]);
        return matcher.matches();
    }

    public boolean isGreenTextMessage()
    {
        if(isNormalMessage()) {
            String[] ch = unformatted.split("> ", 2);
            if(ch.length >= 2)
            {
                if(ch[1].startsWith(">")) {
                    return true;
                }
            }

        }
        return false;
    }

    public boolean isServerMessage()
    {
        if(!isNormalMessage() && (unformatted.contains("Server") || unformatted.contains("server")))
        {
            return true;
        }
        return false;
    }

    public boolean isLogMessage()
    {
        Matcher matcherJoin = joinedPattern.matcher(unformatted);
        Matcher matcherLeave = leftPattern.matcher(unformatted);
        return matcherJoin.matches() || matcherLeave.matches();
    }

    public String getMessage()
    {
        String[] arrowSeparatorGreen = unformatted.split(">");
        String[] arrowSeparatorNormal = unformatted.split("> ", 2);
        String[] whisperSeparator = unformatted.split(": ");

        if(isLogMessage()) return unformatted;

        if(isToMessage()) return "-> " + whisperSeparator[1];

        if(isGreenTextMessage()) return arrowSeparatorGreen[2];

        if(isWhisperMessage()) return whisperSeparator[1];

        if(isNormalMessage()) return arrowSeparatorNormal[1];

        return unformatted;
    }

    public MessageType getType()
    {
        if (isLogMessage()) return MessageType.LOG;
        if (isWhisperMessage()) return MessageType.WHISPER;
        if (isToMessage()) return MessageType.WHISPER;
        if (isGreenTextMessage()) return MessageType.GREEN_TEXT;
        if (isServerMessage()) return MessageType.SERVER;
        if (isNormalMessage()) return MessageType.NORMAL;

        return MessageType.DEATH;
    }
}
