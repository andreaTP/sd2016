final protected def setActorFields(actorInstance: Actor, context: ActorContext, self: ActorRef): Unit =
  if (actorInstance ne null) {
    if (!Reflect.lookupAndSetField(actorInstance.getClass, actorInstance, "context", context)
      || !Reflect.lookupAndSetField(actorInstance.getClass, actorInstance, "self", self))
      throw new IllegalActorStateException(actorInstance.getClass + " is not an Actor since it have not mixed in the 'Actor' trait")
  }
