package com.hezaerd.utils;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModLib {
    public static final String MOD_ID = "skipping_stones";
    public static final String MOD_NAME = "Skipping Stones";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static Identifier of(String path) {
        return Identifier.of(MOD_ID, path);
    }
}