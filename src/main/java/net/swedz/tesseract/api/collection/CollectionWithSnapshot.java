package net.swedz.tesseract.api.collection;

import net.swedz.tesseract.api.Assert;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>A collection that wraps another collection and provides a snapshot of the collection at the time it is accessed.
 * This is primarily for collections that need to be read with their contents at a given point in time, not reflecting
 * changes after it has returned some results.</p>
 *
 * <p>The source collection must not be accessed directly when the returned value may change to reflect the source
 * collection. For example, iterating over the collection must iterate over the snapshot of the collection. On the
 * other hand, checking if the collection is empty or fetching the size of the collection may return directly from the
 * source since those values (boolean and int) are immutable.</p>
 *
 * @param <E> the element type
 */
public final class CollectionWithSnapshot<E> implements Collection<E>
{
	private final Collection<E> source;
	
	private final Function<Collection<E>, Collection<E>> snapshotter;
	
	private Collection<E> snapshot;
	
	public CollectionWithSnapshot(
			Supplier<Collection<E>> sourceSupplier,
			Function<Collection<E>, Collection<E>> snapshotter
	)
	{
		Assert.noneNull(sourceSupplier, snapshotter);
		this.source = sourceSupplier.get();
		this.snapshotter = snapshotter;
		Assert.notNull(this.source);
	}
	
	@Override
	public boolean add(E element)
	{
		var result = source.add(element);
		if(result)
		{
			snapshot = null;
		}
		return result;
	}
	
	@Override
	public boolean addAll(Collection<? extends E> other)
	{
		var result = source.addAll(other);
		if(result)
		{
			snapshot = null;
		}
		return result;
	}
	
	@Override
	public boolean remove(Object element)
	{
		var result = source.remove(element);
		if(result)
		{
			snapshot = null;
		}
		return result;
	}
	
	@Override
	public boolean removeIf(Predicate<? super E> filter)
	{
		var result = source.removeIf(filter);
		if(result)
		{
			snapshot = null;
		}
		return result;
	}
	
	@Override
	public boolean removeAll(Collection<?> other)
	{
		var result = source.removeAll(other);
		if(result)
		{
			snapshot = null;
		}
		return result;
	}
	
	@Override
	public boolean retainAll(Collection<?> other)
	{
		var result = source.retainAll(other);
		if(result)
		{
			snapshot = null;
		}
		return result;
	}
	
	@Override
	public void clear()
	{
		source.clear();
		snapshot = null;
	}
	
	@Override
	public boolean contains(Object element)
	{
		return source.contains(element);
	}
	
	@Override
	public boolean containsAll(Collection<?> other)
	{
		return source.containsAll(other);
	}
	
	@Override
	public int size()
	{
		return source.size();
	}
	
	@Override
	public boolean isEmpty()
	{
		return source.isEmpty();
	}
	
	@Override
	public Iterator<E> iterator()
	{
		return this.snapshot().iterator();
	}
	
	@Override
	public Object[] toArray()
	{
		return this.snapshot().toArray();
	}
	
	@Override
	public <T1> T1[] toArray(T1[] a)
	{
		return this.snapshot().toArray(a);
	}
	
	public Collection<E> snapshot()
	{
		if(snapshot == null)
		{
			snapshot = snapshotter.apply(source);
			Assert.notNull(snapshot);
		}
		return snapshot;
	}
}
