package org.qdrin.qfsm.persist;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.kryo.StateMachineContextSerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;

public class QStateMachineContextConverter {
  private final int maxBufSize = 1024*2048;
  
  StateMachineContext<String, String> toContext(byte[] buffer) {
    Kryo kryo = new Kryo();
    StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
    kryo.addDefaultSerializer(StateMachineContext.class, serializer);
    Input input = new Input(buffer);
    return (StateMachineContext<String, String>) kryo.readClassAndObject(input);
  }
  
  byte[] toBytes(StateMachineContext<String, String> context) {
    Kryo kryo = new Kryo();
    StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
    kryo.addDefaultSerializer(StateMachineContext.class, serializer);
    byte[] buffer = new byte[maxBufSize];
    Output output = new Output(maxBufSize);
    kryo.writeClassAndObject(output, context);
    output.close();
    return buffer;
  }
}
