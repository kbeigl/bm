package bm.traccar.camel.api;

import bm.traccar.generated.model.dto.Device;

public interface Api {

  interface Devices {
    Device[] getById(int id);
  }

  //	interface Users { define Java method signatures }

  //	add APIs as needed

}
