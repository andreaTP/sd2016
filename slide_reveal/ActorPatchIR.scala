akkaActorJSIrPatches/scalajsp akka/actor/Actor.sjsir

class Lakka_actor_Actor extends O {
  var context$1: Lakka_actor_ActorContext
  var self$1: Lakka_actor_ActorRef
  def context__Lakka_actor_ActorContext(): Lakka_actor_ActorContext = {
    this.context$1
  }
  def context$und$eq__Lakka_actor_ActorContext__V(x$1: Lakka_actor_ActorContext) {
    this.context$1 = x$1
  }
  def self__Lakka_actor_ActorRef(): Lakka_actor_ActorRef = {
    this.self$1
  }
  def self$und$eq__Lakka_actor_ActorRef__V(x$1: Lakka_actor_ActorRef) {
    this.self$1 = x$1
  }
  def init___() {
    this.O::init___();
    this.context$1 = null;
    this.self$1 = null
  }
}
