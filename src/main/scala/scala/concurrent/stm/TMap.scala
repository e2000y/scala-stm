/* scala-stm - (c) 2010, LAMP/EPFL */

package scala.concurrent.stm

import scala.collection.{immutable, mutable}


object TMap {

  /** A `Map` that provides atomic execution of all of its methods. */
  trait View[A, B] extends mutable.Map[A, B] with mutable.MapLike[A, B, View[A, B]] {

    /** Returns the `TMap` perspective on this transactional map, which
     *  provides map functionality only inside atomic blocks.
     */
    def tmap: TMap[A, B]

    /** Takes an atomic snapshot of this transactional map. */
    def snapshot: immutable.Map[A, B]

    override def empty: View[A, B] = throw new AbstractMethodError
  }


  /** Constructs and returns a new empty `TMap`. */
  def empty[A, B]: TMap[A, B] = impl.STMImpl.instance.newTMap[A, B]()

  /** Constructs and returns a new `TMap` that will contain the key/value pairs
   *  from `data`.
   */
  def apply[A, B](data: TraversableOnce[(A, B)]): TMap[A, B] = impl.STMImpl.instance.newTMap[A, B](data)


  /** Allows a `TMap` in a transactional context to be used as a `Map`. */
  implicit def asMap[A, B](m: TMap[A, B])(implicit txn: InTxn): View[A, B] = m.single
}


/** A transactional map implementation that requires that all of its map-like
 *  operations be called from inside an atomic block.  Rather than extending
 *  `Map`, an implicit conversion is provided from `TMap` to `Map` if the
 *  current scope is part of an atomic block (see `TMap.asMap`).
 *
 *  The keys (with type `A`) must be immutable, or at least not modified while
 *  they are in the map.  The `TMap` implementation assumes that it can safely
 *  perform key equality and hash checks outside a transaction without
 *  affecting atomicity. 
 */
trait TMap[A, B] {

  /** Returns an instance that provides transactional map functionality without
   *  requiring that operations be performed inside the static scope of an
   *  atomic block.
   */
  def single: TMap.View[A, B]

  // The following methods return the wrong receiver when invoked via the asMap
  // conversion.  They are exactly the methods of mutable.Map whose return type
  // is this.type.  Note that there are other methods of mutable.Map that we
  // allow to use the implicit mechanism, such as put(k).
  
  def += (kv: (A, B))(implicit txn: InTxn): this.type = { single += kv ; this }
  def += (kv1: (A, B), kv2: (A, B), kvs: (A, B)*)(implicit txn: InTxn): this.type = { single.+= (kv1, kv2, kvs: _*) ; this }
  def ++= (kvs: TraversableOnce[(A, B)])(implicit txn: InTxn): this.type = { single ++= kvs ; this }
  def -= (k: A)(implicit txn: InTxn): this.type = { single -= k ; this }
  def -= (k1: A, k2: A, ks: A*)(implicit txn: InTxn): this.type = { single.-= (k1, k2, ks: _*) ; this }
  def --= (ks: TraversableOnce[A])(implicit txn: InTxn): this.type = { single --= ks ; this }
  def transform(f: (A, B) => B)(implicit txn: InTxn): this.type = { single transform f ; this }
  def retain(p: (A, B) => Boolean)(implicit txn: InTxn): this.type = { single retain p ; this }
}