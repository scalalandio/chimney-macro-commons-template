package example

import cats.syntax.either._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.circe.Decoder.Result
import magnolia1._

// based on https://github.com/vpavkin/circe-magnolia/

private[example] trait MagnoliaDecoder extends Derivation[Decoder] {

  type TypeClass[A] = Decoder[A]

  def join[T](caseClass: CaseClass[Decoder, T]): Decoder[T] =
    (c: HCursor) =>
      caseClass
        .constructEither { p =>
          p.typeclass.tryDecode(c.downField(p.label))
        }
        .leftMap(_.head)

  def split[T](sealedTrait: SealedTrait[Decoder, T]): Decoder[T] =
    (c: HCursor) => {
      val constructorLookup = sealedTrait.subtypes.map { s =>
        s.typeInfo.short -> s
      }.toMap
      c.keys match {
        case Some(keys) if keys.size == 1 =>
          val key = keys.head
          for {
            theSubtype <- Either.fromOption(
              constructorLookup.get(key),
              DecodingFailure(
                s"""Can't decode coproduct type: couldn't find matching subtype.
                   |JSON: ${c.value},
                   |Key: $key
                   |Known subtypes: ${constructorLookup.keys.toSeq.sorted.mkString(",")}\n""".stripMargin,
                c.history
              )
            )
            result <- c.get(key)(theSubtype.typeclass)
          } yield result
        case _ =>
          Left(
            DecodingFailure(
              s"""Can't decode coproduct type: zero or several keys were found, while coproduct type requires exactly one.
                 |JSON: ${c.value},
                 |Keys: ${c.keys.map(_.mkString(","))}
                 |Known subtypes: ${constructorLookup.keys.toSeq.sorted.mkString(",")}\n""".stripMargin,
              c.history
            )
          )
      }
    }
}

object DecoderSemi extends MagnoliaDecoder

object DecoderAuto extends MagnoliaDecoder with AutoDerivation[Decoder]

private[example] trait MagnoliaEncoder extends Derivation[Encoder] {

  def join[T](caseClass: CaseClass[Encoder, T]): Encoder[T] =
    (a: T) =>
      Json.obj(caseClass.params.map { p =>
        p.label -> p.typeclass(p.deref(a))
      }: _*)

  def split[T](sealedTrait: SealedTrait[Encoder, T]): Encoder[T] = (a: T) =>
    sealedTrait.choose(a) { subtype =>
      Json.obj(subtype.typeInfo.short -> subtype.typeclass(subtype.cast(a)))
    }
}

object EncoderSemi extends MagnoliaEncoder

object EncoderAuto extends MagnoliaEncoder with AutoDerivation[Encoder]
