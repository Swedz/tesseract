package net.swedz.tesseract.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.swedz.tesseract.api.Assert;

import java.util.HashMap;
import java.util.Map;

public class ConfigCodecMap<D>
{
	private final DynamicOps<D> ops;
	
	private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
	
	public ConfigCodecMap(DynamicOps<D> ops)
	{
		this.ops = ops;
	}
	
	public ConfigCodecMap<D> builtinCodecs()
	{
		this.register(String.class, Codec.STRING);
		this.register(boolean.class, Codec.BOOL);
		this.register(Boolean.class, Codec.BOOL);
		this.register(int.class, Codec.INT);
		this.register(Integer.class, Codec.INT);
		this.register(long.class, Codec.LONG);
		this.register(Long.class, Codec.LONG);
		this.register(double.class, Codec.DOUBLE);
		this.register(Double.class, Codec.DOUBLE);
		this.register(float.class, Codec.FLOAT);
		this.register(Float.class, Codec.FLOAT);
		this.register(short.class, Codec.SHORT);
		this.register(Short.class, Codec.SHORT);
		return this;
	}
	
	public <T> ConfigCodecMap<D> register(Class<T> type, Codec<T> codec)
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
	
	public <T> Codec<T> get(Class<T> type)
	{
		Assert.notNull(type);
		Assert.that(this.has(type), "No codec registered for type %s".formatted(type.getName()));
		
		return (Codec<T>) codecs.get(type);
	}
	
	public <T> T decode(Class<T> type, D value)
	{
		var codec = codecs.get(type);
		return (T) codec.decode(ops, value).getOrThrow().getFirst();
	}
	
	public D encode(Class<?> type, Object value)
	{
		var codec = (Codec) this.get(type);
		return (D) codec.encodeStart(ops, value).getOrThrow();
	}
}
