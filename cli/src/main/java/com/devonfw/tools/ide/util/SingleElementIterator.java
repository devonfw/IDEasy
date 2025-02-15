package com.devonfw.tools.ide.util;

import java.util.Iterator;

/**
 * Implementation of {@link Iterator} for a single element.
 *
 * @param <E> type of the element to iterate.
 */
public final class SingleElementIterator<E> implements Iterator<E> {

  private E element;

  /**
   * @param element the single element to iterate. If {@code null} this iterator will be empty.
   */
  public SingleElementIterator(E element) {

    this.element = element;
  }

  @Override
  public boolean hasNext() {

    return (this.element != null);
  }

  @Override
  public E next() {

    E result = this.element;
    this.element = null;
    return result;
  }

  @Override
  public String toString() {

    return "SingleElementIterator:" + this.element;
  }
}
