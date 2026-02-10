package net.swedz.tesseract.config;

import net.swedz.tesseract.interfaceproxy.InterfaceProxyEntry;
import net.swedz.tesseract.interfaceproxy.InterfaceProxyInstance;

import java.util.function.Supplier;

public record ConfigEntry(
		Object value
) implements InterfaceProxyEntry<Object>
{
	@Override
	public Object resolve(Object[] args)
	{
		if(value instanceof InterfaceProxyInstance<?, ?> config)
		{
			return config.proxy();
		}
		else if(value instanceof Supplier<?> supplier)
		{
			return supplier.get();
		}
		return value;
	}
}
