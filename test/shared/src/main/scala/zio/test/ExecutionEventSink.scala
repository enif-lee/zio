package zio.test

import zio.{Ref, UIO, ULayer, ZIO, ZLayer}

trait ExecutionEventSink {
  def getSummary: UIO[Summary]

  def process(
    event: ExecutionEvent
  ): ZIO[Any, Nothing, Unit]
}

object ExecutionEventSink {
  def getSummary: ZIO[ExecutionEventSink, Nothing, Summary] =
    ZIO.serviceWithZIO[ExecutionEventSink](_.getSummary)

  def process(
    event: ExecutionEvent
  ): ZIO[ExecutionEventSink, Nothing, Unit] =
    ZIO.serviceWithZIO[ExecutionEventSink](_.process(event))

  def ExecutionEventSinkLive(testOutput: TestOutput): ZIO[Any, Nothing, ExecutionEventSink] =
    for {
      summary <- Ref.make[Summary](Summary.empty)
    } yield new ExecutionEventSink {

      override def process(
        event: ExecutionEvent
      ): ZIO[Any, Nothing, Unit] =
        summary.update(
          _.add(event)
        ) *>
          testOutput.print(
            event
          )

      override def getSummary: UIO[Summary] = summary.get

    }

  val live: ZLayer[TestOutput, Nothing, ExecutionEventSink] =
    ZLayer.fromZIO(
      for {
        testOutput <- ZIO.service[TestOutput]
        sink       <- ExecutionEventSinkLive(testOutput)
      } yield sink
    )

  val silent: ULayer[ExecutionEventSink] =
    ZLayer.succeed(
      new ExecutionEventSink {
        override def getSummary: UIO[Summary] = ZIO.succeed(Summary.empty)

        override def process(event: ExecutionEvent): ZIO[Any, Nothing, Unit] = ZIO.unit
      }
    )
}
