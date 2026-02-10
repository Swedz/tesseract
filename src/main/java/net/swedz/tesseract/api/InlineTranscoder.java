package net.swedz.tesseract.api;

import java.util.function.Function;

public final class InlineTranscoder<D, E> implements Transcoder<D, E>
{
	private final Function<D, E> encoder;
	private final Function<E, D> decoder;
	
	public InlineTranscoder(Function<D, E> encoder, Function<E, D> decoder)
	{
		this.encoder = encoder;
		this.decoder = decoder;
	}
	
	@Override
	public D decode(E encoded)
	{
		return decoder.apply(encoded);
	}
	
	@Override
	public E encode(D value)
	{
		return encoder.apply(value);
	}
}
