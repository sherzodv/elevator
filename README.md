
### Simple Elevator simulator

Goals:

	Start to learn Kotlin
	Try some Akka, Java, Kotlin "K"ooperation.
	Elevators' logic is interesting.

### Structure

	Elevator is an actor. There is a thin and simple HTTP layer.

### Commands

	curl localhost:8080/call/0

		Call elevator to floor #0

	curl localhost:8080/goto/5

		Ask elevator to go to floor #5. At reaching the target floor
		it will open the doors and close them after.

	curl localhost:8080/stop

		Gracefully stop the service and start using stairs.

### More

	Take a look at tests and logs. The most interesting dynamic and async part
	of action happens there.
