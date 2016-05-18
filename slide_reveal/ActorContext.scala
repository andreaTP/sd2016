trait Actor {
  /**
   * Stores the context for this actor, including self, and sender.
   * It is implicit to support operations such as `forward`.
   *
   * WARNING: Only valid within the Actor itself, so do not close over it and
   * publish it to other threads!
   *
   */
  implicit val context: ActorContext = {
    val contextStack = ActorCell.contextStack.get
    if ((contextStack.isEmpty) || (contextStack.head eq null))
      throw ActorInitializationException(
        s"You cannot create an instance of [${getClass.getName}] explicitly using the constructor (new). " +
          "You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.")
    val c = contextStack.head
    ActorCell.contextStack.set(null :: contextStack)
    c
  }
