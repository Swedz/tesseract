package net.swedz.tesseract.config;

import net.swedz.tesseract.config.annotation.ConfigKey;
import net.swedz.tesseract.config.annotation.SubSection;
import net.swedz.tesseract.helper.NamingConventionHelper;
import net.swedz.tesseract.interfaceproxy.InterfaceProxyHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Supplier;

public final class ConfigHandler extends InterfaceProxyHandler<ConfigEntry>
{
	private final ConfigManager manager;
	private final String        path;
	
	public ConfigHandler(ConfigManager manager, String path)
	{
		this.manager = manager;
		this.path = path;
	}
	
	private String path(String key)
	{
		return (path.isEmpty() ? "" : (path + ".")) + key;
	}
	
	void loadFile(Class<?> proxyClass)
	{
		manager.file().load(proxyClass);
	}
	
	private Object loadValue(Object proxy, Method method, Class<?> type, String path)
	{
		var fileValue = manager.file().get(type, path);
		if(fileValue != null)
		{
			return fileValue;
		}
		return (Supplier<Object>) () ->
		{
			try
			{
				return InvocationHandler.invokeDefault(proxy, method);
			}
			catch(Throwable ex)
			{
				throw new RuntimeException(ex);
			}
		};
	}
	
	@Override
	protected Optional<ConfigEntry> generate(Class<?> proxyClass, Object proxy, Method method)
	{
		if(method.isAnnotationPresent(ConfigKey.class))
		{
			var key = method.getAnnotation(ConfigKey.class).value();
			if(key.isEmpty())
			{
				key = NamingConventionHelper.fromCamelCaseToSnakeCase(method);
			}
			var path = this.path(key);
			var type = method.getReturnType();
			
			Object value;
			if(method.isAnnotationPresent(SubSection.class))
			{
				value = manager.build(type, new ConfigManagerArg(path)).load(false);
			}
			else
			{
				value = this.loadValue(proxy, method, type, path);
			}
			return Optional.of(new ConfigEntry(value));
		}
		return Optional.empty();
	}
}
