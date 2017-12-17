package com.example.kotlin.elevator

import akka.actor.ActorRef
import akka.actor.AbstractActor

import java.util.HashSet
import java.util.LinkedList
import java.util.PriorityQueue
import java.util.Collections
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import org.slf4j.LoggerFactory


class Elevator(delay: Long): AbstractActor() {

	data class Call(val floor: Int)
	data class Goto(val floor: Int)
	data class Goin(val user: String, val weight: Float)
	data class Gout(val user: String)
	data class Stop(val b: Int)

	data class PassingAt(val floor: Int)
	data class DoorOpeningAt(val floor: Int)
	data class DoorClosingAt(val floor: Int)

	data class Subscribe(val ref: ActorRef)
	data class Unsubscribe(val ref: ActorRef)

	class WhereAreYou


	companion object {
		private val L = LoggerFactory.getLogger(Elevator::class.java)
	}

	private val MOVE_DELAY			= Duration.create(delay, TimeUnit.MILLISECONDS)
	private val DOOR_OPEN_DELAY		= Duration.create(delay, TimeUnit.MILLISECONDS)
	private val DOOR_CLOSE_DELAY	= Duration.create(delay, TimeUnit.MILLISECONDS)

	private var floor = 0
	private var weight = 0.0f
	private var inbatch = false

	private val subs	= HashSet<ActorRef>()
	private val calls	= LinkedList<Int>()
	private val targets	= PriorityQueue<Int>(Collections.reverseOrder())

	private data class Move(val target: Int)
	private data class DoorOpen(val target: Int)
	private data class DoorClose(val target: Int)

	override fun createReceive(): Receive {
		return receiveBuilder()
			.match(Move::class.java, { (target: Int) ->
				L.debug("Move received: current floor {} target {}", floor, target)
				onNextFloor(target)
				if (target != floor) {
					for (ref in subs) {
						ref.tell(PassingAt(floor), ActorRef.noSender())
					}
				}
			})
			.match(Goto::class.java, { (target: Int) ->
				L.debug("Goto received: current floor {} target {}", floor, target)
				onGoto(target)
			})
			.match(DoorOpen::class.java, { (target: Int) ->
				L.debug("DoorOpen received: current floor {} target {}", floor, target)
				onDoorOpen(target)
				if (target == floor) {
					for (ref in subs) {
						ref.tell(PassingAt(floor), ActorRef.noSender())
					}
				}
				for (ref in subs) {
					ref.tell(DoorOpeningAt(floor), ActorRef.noSender())
				}
			})
			.match(DoorClose::class.java, { (target: Int) ->
				L.debug("DoorClose received: current floor {} target {}", floor, target)
				onDoorClose(target)
				for (ref in subs) {
					ref.tell(DoorClosingAt(floor), ActorRef.noSender())
				}
			})
			.match(Call::class.java, { (target: Int) ->
				L.debug("Call received: current floor {} target {}", floor, target)
				onCall(target)
			})
			.match(Subscribe::class.java, { (ref: ActorRef) ->
				L.debug("Sub received: {}", ref)
				subs.add(ref)
			})
			.match(Unsubscribe::class.java, { (ref: ActorRef) ->
				L.debug("Unsub received: {}", ref)
				subs.remove(ref)
			})
			.match(WhereAreYou::class.java, {
				sender().tell(floor, ActorRef.noSender())
			})
			.build()
	}

	private fun onCall(target: Int): Unit {
		if (inbatch) {
			calls.add(target)
		} else {
			inbatch = true
			L.debug("Batch started")
			next(target)
		}
	}

	private fun onGoto(target: Int): Unit {
		if (inbatch) {
			targets.add(target)
		} else {
			inbatch = true
			next(target)
		}
	}

	private fun onNextFloor(target: Int): Unit {
		if (floor == target) {
			send(DoorOpen(floor), DOOR_OPEN_DELAY)
		} else {
			if (floor < target) {
				floor += 1
			} else {
				floor -= 1
			}
			if (targets.contains(floor)) {
				send(DoorOpen(target), DOOR_OPEN_DELAY)
			} else {
				next(target)
			}
		}
	}
	
	private fun onDoorOpen(target: Int): Unit {
		if (targets.contains(floor)) {
			targets.remove(floor)
		}
		send(DoorClose(target), DOOR_CLOSE_DELAY)
	}

	private fun onDoorClose(target: Int): Unit {
		if (target != floor) {
			next(target)
		} else {
			val t = targets.poll()
			if (t != null) {
				next(t)
			} else {
				val c = calls.poll()
				if (c != null) {
					next(c)
				} else {
					inbatch = false
					L.debug("Batch done")
				}
			}
		}
	}

	private fun next(target: Int): Unit {
		send(Move(target), MOVE_DELAY)
	}

	private fun <T> send(msg: T, delay: FiniteDuration): Unit {
		context.system.scheduler().scheduleOnce(
			delay, self, msg, context.dispatcher(), null
		)
	}
}

