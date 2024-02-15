package org.qdrin.qfsm.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.model.entity.StateMachineEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachine;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.apache.commons.lang3.NotImplementedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QStateMachineRepository implements JpaStateMachineRepository {
  @Autowired
  StateMachineCrudRepository repo;

  public QStateMachineRepository() {
    log.info("QStateMachineRepository constructor");
  }
  
  public <S extends JpaRepositoryStateMachine> S save(S entity) {
    log.info("JQStateMachineRepository.save");
    // repo.save(entity);
    return entity;
  }

  @Override
  public <S extends JpaRepositoryStateMachine> Iterable<S> saveAll(Iterable<S> entities) {
      log.info("QStateMachineRepository.saveAll");

      for (S item : entities) {
          log.info("QStateMachineRepository.saveAll -> saving entity {}", item);
          // repo.save(JRSMtoSMRuntimeMapper(item));
      }

      return entities;
  }

    @Override
    public Optional<JpaRepositoryStateMachine> findById(String s) {
        log.info("QStateMachineRepository.findById");
        Optional<StateMachineEntity> v = repo.findById(s);
        return v.map((e) -> entityMapper(e));
    }

    @Override
    public boolean existsById(String s) {
        log.info("QStateMachineRepository.existsById");
        return repo.existsById(s);
    }

    @Override
    public Iterable<JpaRepositoryStateMachine> findAll() {
        log.info("QStateMachineRepository.findAll");
        Iterable<StateMachineEntity> smrResult = repo.findAll();
        List<JpaRepositoryStateMachine> result = new ArrayList<>();
        for (StateMachineEntity item : smrResult) {
            result.add(entityMapper(item));
        }
        return  result;
    }

    @Override
    public Iterable<JpaRepositoryStateMachine> findAllById(Iterable<String> strings) {
        log.info("QStateMachineRepository.findAllById");
        throw new NotImplementedException();
    }

    @Override
    public long count() {
        log.info("QStateMachineRepository.count");
        return repo.count();
    }

    @Override
    public void deleteById(String s) {
        log.info("QStateMachineRepository.deleteById");
        repo.deleteById(s);
    }

    @Override
    public void delete(JpaRepositoryStateMachine entity) {
        log.info("QStateMachineRepository.delete");
        repo.delete(entityMapper(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {
        log.info("QStateMachineRepository.deleteAllById");
        throw new NotImplementedException();

    }

    @Override
    public void deleteAll(Iterable<? extends JpaRepositoryStateMachine> entities) {
        log.info("QStateMachineRepository.deleteAll");
        throw new NotImplementedException();

    }

    @Override
    public void deleteAll() {
        log.info("QStateMachineRepository.deleteAll");
        throw new NotImplementedException();
    }

    public static JpaRepositoryStateMachine entityMapper(StateMachineEntity entity){
      JpaRepositoryStateMachine jsrm = new JpaRepositoryStateMachine();
      jsrm.setMachineId(entity.getMachineId());
      jsrm.setState(entity.getState());
      jsrm.setStateMachineContext(entity.getStateMachineContext());
      return  jsrm;
    }

    public static StateMachineEntity entityMapper(JpaRepositoryStateMachine entity){
      StateMachineEntity smr = new StateMachineEntity();
      smr.setMachineId(entity.getMachineId());
      smr.setState(entity.getState());
      smr.setStateMachineContext(entity.getStateMachineContext());
      return  smr;
  }
}
