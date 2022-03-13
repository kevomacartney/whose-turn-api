import ItemRepositorySpec.withRepository
import cats.effect.unsafe.IORuntime
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.repositories.ItemRepository
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ItemRepositorySpec extends AnyWordSpec with Matchers with ScalaFutures {
  import ItemRepositorySpec._
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  "ItemRepository" should {
    "return a repositoryItem for valid id" in {
      withRepository { repository =>
        val item = repository.get("valid-id").unsafeRunSync()
        item must be('defined)
      }
    }

    "return a nothing for invalid id" in {
      withRepository { repository =>
        val item = repository.get("failure").unsafeRunSync()
        item mustBe None
      }
    }

    "metrics are recorded" in {
      implicit val metricRegistry: MetricRegistry = new MetricRegistry()

      withRepository { repository =>
        repository.get("failure").unsafeRunSync()
        repository.get("success").unsafeRunSync()

        metricRegistry.meter("item-repository.failure").getCount mustBe 1
        metricRegistry.meter("item-repository.success").getCount mustBe 1
      }(metricRegistry)
    }
  }
}

object ItemRepositorySpec {
  implicit val metricsRegistry: MetricRegistry = new MetricRegistry()

  def withRepository[T](f: ItemRepository => T)(implicit mr: MetricRegistry): T = {
    val repo = new ItemRepository(mr)
    f(repo)
  }
}
