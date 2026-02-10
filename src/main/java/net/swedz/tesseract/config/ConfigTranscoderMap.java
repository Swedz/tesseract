package net.swedz.tesseract.config;

import net.swedz.tesseract.api.Assert;
import net.swedz.tesseract.api.Transcoder;

import java.util.HashMap;
import java.util.Map;

public class ConfigTranscoderMap<D>
{
	private final Map<Class<?>, Transcoder<?, D>> codecs = new HashMap<>();
	
	public <T> ConfigTranscoderMap<D> register(Class<T> type, Transcoder<T, D> codec)
	{
		Assert.noneNull(type, codec);
		
		codecs.put(type, codec);
		
		return this;
	}
	
	public <T> boolean has(Class<T> type)
	{
		Assert.notNull(type);
		
		return codecs.containsKey(type);
	}
	
	public <T> Transcoder<T, D> get(Class<T> type)
	{
		Assert.notNull(type);
		Assert.that(this.has(type), "No codec registered for type %s".formatted(type.getName()));
		
		return (Transcoder<T, D>) codecs.get(type);
	}
}
