package com.medinote.medinotebackend.dataagent.dto;

public class ColumnMetaDto {

    private final String name;
    private final String type;
    private final boolean nullable;
    private final String key;

    public ColumnMetaDto(String name, String type, boolean nullable, String key) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.key = key;
    }

    public String getName()      { return name; }
    public String getType()      { return type; }
    public boolean isNullable()  { return nullable; }
    public String getKey()       { return key; }
}
