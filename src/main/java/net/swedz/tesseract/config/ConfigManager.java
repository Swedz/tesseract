package net.swedz.tesseract.config;

import net.swedz.tesseract.interfaceproxy.InterfaceProxyManager;

public final class ConfigManager extends InterfaceProxyManager.WithArgument<ConfigHandler, ConfigManagerArg>
{
	private final ConfigFileAccess<?> file;
	
	public ConfigManager(ConfigFileAccess<?> file)
	{
		this.file = file;
	}
	
	ConfigFileAccess<?> file()
	{
		return file;
	}
	
	@Override
	protected ConfigManagerArg defaultManagerArgument()
	{
		return new ConfigManagerArg("");
	}
	
	@Override
	protected <P> ConfigHandler createHandler(Class<P> proxyClass, ConfigManagerArg arg)
	{
		return new ConfigHandler(this, arg.path());
	}
	
	@Override
	protected <P> ConfigInstance<P> createInstance(Class<P> proxyClass, P proxy, ConfigHandler handler)
	{
		return new ConfigInstance<>(proxyClass, proxy, handler);
	}
	
	@Override
	public <P> ConfigInstance<P> build(Class<P> proxyClass)
	{
		return (ConfigInstance<P>) super.build(proxyClass);
	}
	
	@Override
	public <P> ConfigInstance<P> build(Class<P> proxyClass, ConfigManagerArg arg)
	{
		return (ConfigInstance<P>) super.build(proxyClass, arg);
	}
}
