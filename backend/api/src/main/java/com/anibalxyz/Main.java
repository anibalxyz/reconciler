package com.anibalxyz;

import com.anibalxyz.server.Config;
import com.anibalxyz.server.Router;

public class Main {
    public static void main(String[] args) throws Exception {
        PersistenceManager.init();

        Router.init(Config.getServer(), 4000);

        PersistenceManager.shutdown();
    }
}
