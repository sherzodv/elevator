package com.example.kotlin.elevator


import akka.testkit.javadsl.TestKit

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.assertEquals

import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.ActorSystem
import akka.actor.AbstractActor
import scala.concurrent.duration.Duration


class ElevatorTest {

	companion object {

		var sys: ActorSystem? = null

		@BeforeAll
		@JvmStatic
		fun start(): Unit {
			sys = ActorSystem.create()
		}

		@AfterAll
		@JvmStatic
		fun finish(): Unit {
			TestKit.shutdownActorSystem(sys)
			sys = null
		}
	
	}

	private var elv: ActorRef? = null

	@BeforeEach
	fun beforeTest(): Unit {
		val props = Props.create(Elevator::class.java, 10L)
		elv = sys?.actorOf(props)
	}

	@AfterEach
	fun afterTest(): Unit {
		elv?.tell(PoisonPill.getInstance(), ActorRef.noSender())
		elv = null
	}
	
	private fun <T> req(msg: T): Unit {
		elv?.tell(msg, ActorRef.noSender())
	}

	@Test
	fun `arrives, opens doors, closes doors`(): Unit { object: TestKit(sys) { init {
		req(Elevator.Subscribe(getRef()))

		req(Elevator.Call(0))

		within(duration("1 second"), {
			expectMsgEquals(Elevator.PassingAt(0))
			expectMsgEquals(Elevator.DoorOpeningAt(0))
			expectMsgEquals(Elevator.DoorClosingAt(0))
		})
	} } }

	@Test
	fun `arrives, takes passengers, delivers, delivers again`(): Unit { object: TestKit(sys) { init {
		req(Elevator.Subscribe(getRef()))

		req(Elevator.Call(0))

		expectMsgEquals(Elevator.PassingAt(0))
		expectMsgEquals(Elevator.DoorOpeningAt(0))

		req(Elevator.Goin("Ivan", 80f))

		req(Elevator.Goto(2))

		expectMsgEquals(Elevator.DoorClosingAt(0))
		expectMsgEquals(Elevator.PassingAt(1))
		expectMsgEquals(Elevator.PassingAt(2))

		expectMsgEquals(Elevator.DoorOpeningAt(2))
		expectMsgEquals(Elevator.DoorClosingAt(2))

		req(Elevator.Goto(10))
		receiveN(8)

		expectMsgEquals(Elevator.DoorOpeningAt(10))
		expectMsgEquals(Elevator.DoorClosingAt(10))
	} } }

	@Test
	fun `arrives, takes passengers, delivers`(): Unit { object: TestKit(sys) { init {
		req(Elevator.Subscribe(getRef()))

		req(Elevator.Call(0))

		expectMsgEquals(Elevator.PassingAt(0))
		expectMsgEquals(Elevator.DoorOpeningAt(0))

		req(Elevator.Goin("Ivan", 80f))
		req(Elevator.Goin("Firuza", 60f))
		req(Elevator.Goin("Tetka kotoraya vsyo isportiala", 120f))
		req(Elevator.Goto(23))
		req(Elevator.Goto(3))

		expectMsgEquals(Elevator.DoorClosingAt(0))
		expectMsgEquals(Elevator.PassingAt(1))
		expectMsgEquals(Elevator.PassingAt(2))
		expectMsgEquals(Elevator.PassingAt(3))
		expectMsgEquals(Elevator.DoorOpeningAt(3))
		expectMsgEquals(Elevator.DoorClosingAt(3))
		receiveN(19)
		expectMsgEquals(Elevator.PassingAt(23))
		expectMsgEquals(Elevator.DoorOpeningAt(23))
		expectMsgEquals(Elevator.DoorClosingAt(23))
	} } }

	@Test
	fun `arrives, takes passengers, delivers, goes back`(): Unit { object: TestKit(sys) { init {
		req(Elevator.Subscribe(getRef()))

		req(Elevator.Call(0))

		expectMsgEquals(Elevator.PassingAt(0))
		expectMsgEquals(Elevator.DoorOpeningAt(0))

		req(Elevator.Goin("Ivan", 80f))
		req(Elevator.Goin("Firuza", 60f))

		req(Elevator.Goto(2))
		req(Elevator.Goto(5))

		expectMsgEquals(Elevator.DoorClosingAt(0))
		expectMsgEquals(Elevator.PassingAt(1))
		expectMsgEquals(Elevator.PassingAt(2))

		req(Elevator.Goto(1))

		expectMsgEquals(Elevator.DoorOpeningAt(2))
		expectMsgEquals(Elevator.DoorClosingAt(2))
		expectMsgEquals(Elevator.PassingAt(3))
		expectMsgEquals(Elevator.PassingAt(4))
		expectMsgEquals(Elevator.PassingAt(5))
		expectMsgEquals(Elevator.DoorOpeningAt(5))
		expectMsgEquals(Elevator.DoorClosingAt(5))
		expectMsgEquals(Elevator.PassingAt(4))
		expectMsgEquals(Elevator.PassingAt(3))
		expectMsgEquals(Elevator.PassingAt(2))
		expectMsgEquals(Elevator.PassingAt(1))
		expectMsgEquals(Elevator.DoorOpeningAt(1))
		expectMsgEquals(Elevator.DoorClosingAt(1))

	} } }
}
