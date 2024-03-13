package org.qdrin.qfsm.persist;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.kryo.StateMachineContextSerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QStateMachineContextConverter {
  private final int maxBufSize = 1024*2048;
  
  StateMachineContext<String, String> toContext(byte[] buffer) {
    Kryo kryo = new Kryo();
    StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
    kryo.addDefaultSerializer(StateMachineContext.class, serializer);
    Input input = new Input(buffer);
    StateMachineContext<String, String> context = (StateMachineContext<String, String>) kryo.readClassAndObject(input);
    input.close();
    return context;
  }
  
  byte[] toBytes(StateMachineContext<String, String> context) {
    Kryo kryo = new Kryo();
    StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
    kryo.addDefaultSerializer(StateMachineContext.class, serializer);
    Output output = new Output(maxBufSize);
    kryo.writeClassAndObject(output, context);
    log.debug("output: {}", output);
    return output.toBytes();
  }
}
