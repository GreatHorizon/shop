package com.example.main.model;

public enum CartActionModel {
    PLUS,
    MINUS,
    DELETE;

    public boolean isPlus() {
        return this == CartActionModel.PLUS;
    }

    public boolean isMinus() {
        return this == CartActionModel.MINUS;
    }

    public boolean isDelete() {
        return this == CartActionModel.DELETE;
    }
}
