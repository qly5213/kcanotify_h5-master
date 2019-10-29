package com.antest1.kcanotify.h5;

import java.util.List;
import java.util.Map;

public interface GameConnection {

    Map<String, List<String>> getCookies();

    String getStartUrl();
}
