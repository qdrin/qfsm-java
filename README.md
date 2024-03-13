# qfsm-java
qfsm port based on spring StateMachine

StateMachineService doesn't work properly - it can't restore orthogonal subregions to saved states. Instead it restores them to initial subregions's states. Known issue: https://github.com/spring-projects/spring-statemachine/pull/998

