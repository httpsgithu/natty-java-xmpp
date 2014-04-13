package org.lantern.natty; 

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;


public class NattyMessage {
  private String remote;
  private String local;
  private String candidate;
  private String sdpMLineIndex;
  private String sdpMid;
  private String type;

  public NattyMessage() {
    this.type = "";
  }

  public boolean isOffer() {
    return type.equals("offer");
  }

  public boolean isAnswer() {
    return type.equals("answer");
  }

  public boolean isCandidate() {
    return candidate != null;
  }

  public boolean is5Tuple() {
    return type.equals("5-tuple");
  }

  public String getCandidate() {
    return candidate;
  }

  public String getLocal() {
    return local;
  }

  public String getRemote() {
    return remote;
  }

  public String getType() {
    return type;
  }

  @Override
    public String toString() {
      return "Message [candidate=" + candidate + ", " + "remote=" + 
        remote + ", local=" + local + ", type=" + type + "]";
    }

   public static NattyMessage fromJson(String json) throws JsonMappingException, IOException {
      if (json.contains("WARNING")) {
        return null;
      }
      ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
      mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return mapper.readValue(json, NattyMessage.class);
    }
}
