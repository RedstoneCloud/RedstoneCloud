package de.redstonecloud.cloud.logger;

import lombok.Getter;

@Getter
public enum Colors {
    WHITE("white", 'f', "30m"),
    RED("red", 'c', "31m"),
    GREEN("green", 'a', "32m"),
    YELLOW("yellow", 'e', "33m"),
    MAGENTA("magenta", 'd', "35m"),
    CYAN("cyan", 'b', "36m"),
    RESET("reset", 'r', "0m"),


    RESET1("reset1", '0', "0m"),
    BLUE1("blue1", '1', "34m"),
    GREEN1("green1", '2', "32m"),
    CYAN1("cyan1", '3', "36m"),
    RED1("red1", '4', "31m"),
    MAGENTA1("magenta", '5', "35m"),
    YELLOW1("yellow", '6', "33m"),
    GRAY1("gray1", '7', "37m"),
    GRAY2("gray2", '8', "37m"),
    BLUE2("blue2", '9', "34m"),

    BOLD("bold", 'l', "1m"),
    RESET_BOLD("reset_bold", 'r', "21m"),
    UNDERLINED("underlined", '_', "4m");


    private final String name;
    private final String javaCode;
    private final char index;

    Colors(String name, char index, String javaCode) {
        this.name = name;
        this.index = index;
        this.javaCode = (char) 27 + "[" + javaCode;
    }

    public static String toColor(String text) {
        if (text == null) {
            throw new NullPointerException();
        }

        for (Colors consoleColour : values()) {
            text = text.replace("§" + consoleColour.index, consoleColour.javaCode);
        }

        return text;
    }

    @Override
    public String toString() {
        return "Color{" +
                "name='" + name + '\'' +
                ", javaCode='" + javaCode + '\'' +
                ", index=" + index +
                '}';
    }

}