package net.swedz.tesseract.config;

import net.swedz.tesseract.interfaceproxy.InterfaceProxyInstance;

public record ConfigInstance<C>(
		Class<C> proxyClass,
		C proxy,
		ConfigHandler handler
) implements InterfaceProxyInstance<C, ConfigHandler>
{
	@Override
	public ConfigInstance<C> load(boolean file)
	{
		if(file)
		{
			handler.loadFile(proxyClass);
		}
		InterfaceProxyInstance.super.load(file);
		return this;
	}
	
	@Override
	public ConfigInstance<C> load()
	{
		InterfaceProxyInstance.super.load();
		return this;
	}
}
