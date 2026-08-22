package net.swedz.tesseract.config;

import net.swedz.tesseract.interfaceproxy.InterfaceProxyEntry;
import net.swedz.tesseract.interfaceproxy.InterfaceProxyInstance;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigEntry implements InterfaceProxyEntry<Object>
{
	private final String  path;
	private final Object  value;
	private final boolean useCache;
	
	private Object cached;
	
	public ConfigEntry(String path, Object value, boolean useCache)
	{
		this.path = path;
		this.value = value;
		this.useCache = useCache;
	}
	
	public String path()
	{
		return path;
	}
	
	private Object resolveDirect(Object[] args)
	{
		if(value instanceof InterfaceProxyInstance<?, ?> config)
		{
			return config.proxy();
		}
		else if(value instanceof Supplier<?> supplier)
		{
			return supplier.get();
		}
		else if(value instanceof Consumer consumer)
		{
			consumer.accept(args[0]);
			return null;
		}
		return value;
	}
	
	@Override
	public Object resolve(Object[] args)
	{
		if(!useCache)
		{
			return this.resolveDirect(args);
		}
		if(cached == null)
		{
			cached = this.resolveDirect(args);
		}
		return cached;
	}
	
	public void resetCache()
	{
		cached = null;
	}
}
