package net.swedz.tesseract.config;

import net.swedz.tesseract.config.annotation.ConfigKey;
import net.swedz.tesseract.config.annotation.SubSection;
import net.swedz.tesseract.config.exception.IllegalConfigMethodException;
import net.swedz.tesseract.helper.NamingConventionHelper;
import net.swedz.tesseract.interfaceproxy.InterfaceProxyHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;
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
		this.resetCache();
	}
	
	private Supplier<Object> loadValue(Object proxy, Method method, Class<?> type, String path)
	{
		return () ->
		{
			var fileValue = manager.file().get(type, path);
			if(fileValue != null)
			{
				return fileValue;
			}
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
	
	private Consumer<Object> editValue(Class<?> type, String path)
	{
		return (value) ->
		{
			manager.file().set(type, path, value);
			this.resetCache(path);
		};
	}
	
	void resetCache(String path)
	{
		for(var entry : this.entries())
		{
			if(entry.path().equals(path))
			{
				entry.resetCache();
			}
		}
	}
	
	void resetCache()
	{
		for(var entry : this.entries())
		{
			entry.resetCache();
		}
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
			var returnType = method.getReturnType();
			
			Object value;
			if(returnType == void.class)
			{
				if(method.getParameterCount() != 1)
				{
					throw new IllegalConfigMethodException("Cannot have void config setter method without exactly one parameter");
				}
				var parameterType = method.getParameterTypes()[0];
				value = this.editValue(parameterType, path);
			}
			else if(method.isAnnotationPresent(SubSection.class))
			{
				value = manager.build(returnType, new ConfigManagerArg(path)).load(false);
			}
			else
			{
				value = this.loadValue(proxy, method, returnType, path);
			}
			return Optional.of(new ConfigEntry(path, value, manager.cachesValues()));
		}
		return Optional.empty();
	}
}
