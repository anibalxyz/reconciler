package com.anibalxyz;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.Router;

public class Main {
  public static void main(String[] args) throws Exception {
    Router.init(4000);
  }
}
