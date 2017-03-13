package giter8

import giter8.PropertyResolver._
import org.scalatest.{FlatSpec, Matchers, TryValues}

class PropertyResolversTest extends FlatSpec with Matchers with TestFileHelpers with TryValues {
  import FileDsl._

  "FilePropertiesResolver" should "parse property file" in tempDirectory { temp =>
    "foo = bar" >> (temp / "test.properties")

    FilePropertyResolver(temp / "test.properties").resolve(Map.empty).success.value should
      contain theSameElementsAs Map("foo" -> "bar")
  }

  it should "parse multiple files" in tempDirectory { temp =>
    "foo = bar" >> (temp / "test1.properties")
    "baz = quux" >> (temp / "test2.properties")

    FilePropertyResolver(temp / "test1.properties", temp / "test2.properties").resolve(Map.empty).success.value should
      contain theSameElementsAs Map("foo" -> "bar", "baz" -> "quux")
  }

  it should "return DuplicatePropertiesError if duplicate properties found" in tempDirectory { temp =>
    "foo = bar" >> (temp / "test1.properties")
    "foo = baz" >> (temp / "test2.properties")

    val resolved = FilePropertyResolver(temp / "test1.properties", temp / "test2.properties").resolve(Map.empty)
    resolved.failure.exception shouldBe a[DuplicatePropertyError]
  }

  it should "resolve inner properties" in tempDirectory { temp =>
    "foo = a $anotherFoo$" >> (temp / "test1.properties")
    "anotherFoo = bar" >> (temp / "test2.properties")

    FilePropertyResolver(temp / "test1.properties", temp / "test2.properties")
      .resolve(Map.empty)
      .success
      .value should
      contain theSameElementsAs Map("anotherFoo" -> "bar", "foo" -> "a bar")
  }

  "StaticPropertyResolver" should "add additional properties to existing" in {
    StaticPropertyResolver(Map("baz" -> "quux")).resolve(Map("foo" -> "bar")).success.value should
      contain theSameElementsAs Map("foo" -> "bar", "baz" -> "quux")
  }

  it should "override existing properties" in {
    StaticPropertyResolver(Map("foo" -> "quux")).resolve(Map("foo" -> "bar")).success.value should
      contain theSameElementsAs Map("foo" -> "quux")
  }
}
