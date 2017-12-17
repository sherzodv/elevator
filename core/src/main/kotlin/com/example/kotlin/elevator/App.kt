package com.example.kotlin.elevator

import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import akka.NotUsed
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.server.PathMatchers.*
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Flow
import akka.pattern.PatternsCS.ask

import java.util.concurrent.CompletionStage


class App(val sys: ActorSystem, val mat: ActorMaterializer): AllDirectives() {

	private fun createRoute(): Route {
		return route(
			path("stop", {
				get({
					sys.terminate()
					complete("ok\n")
				})
			})
			, path(segment("call").slash(integerSegment()), { target ->
				val msg = Elevator.Call(target)
				sys.actorSelection("/user/elevator").tell(msg, ActorRef.noSender())
				complete("ok\n")
			})
			, path(segment("goto").slash(integerSegment()), { target ->
				if (target < 0) {
					complete("Wrong floor\n")
				} else {
					val msg = Elevator.Goto(target)
					sys.actorSelection("/user/elevator").tell(msg, ActorRef.noSender())
					complete("ok\n")
				}
			})
			, path("where", {
				val elv = sys.actorSelection("/user/elevator")
				onSuccess({
					ask(elv, Elevator.WhereAreYou(), 1000)
				}, { floor: Any ->
					complete("At: " + floor.toString() + "\n")
				})
			})
		)
	}

	private fun run(): Unit {
		val http = Http.get(sys)
		val route = createRoute()
		val handler = route.flow(sys, mat)
		val binding = http.bindAndHandle(
			handler, ConnectHttp.toHost("localhost", 8080), mat
		)
		binding.exceptionally({ e ->
			L.error("Exception while handling request:", e)
			sys.terminate()
			null
		})
	}

	companion object {

		private val L = LoggerFactory.getLogger(Elevator::class.java)

		@JvmStatic public fun main(args: Array<String>) {
			val sys = ActorSystem.create("building")
			try {
				val delay = 1000L
				val mat = ActorMaterializer.create(sys)
				sys.actorOf(Props.create(Elevator::class.java, delay), "elevator")
				val app = App(sys, mat)
				app.run()
			} catch (e: Exception) {
				L.error("Exception while initializing application:", e)
				sys.terminate()
			}
		}
	}

}
