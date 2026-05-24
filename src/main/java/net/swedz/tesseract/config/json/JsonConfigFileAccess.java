package net.swedz.tesseract.config.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.swedz.tesseract.config.ConfigFileAccess;
import net.swedz.tesseract.config.ConfigCodecMap;
import net.swedz.tesseract.config.DefaultValueConfigHandler;
import net.swedz.tesseract.config.annotation.ConfigKey;
import net.swedz.tesseract.config.annotation.SubSection;
import net.swedz.tesseract.api.Assert;
import net.swedz.tesseract.helper.NamingConventionHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public final class JsonConfigFileAccess implements ConfigFileAccess<JsonElement>
{
	private final ConfigCodecMap<JsonElement> codecs = new ConfigCodecMap(JsonOps.INSTANCE);
	
	private final File file;
	
	private JsonObject json;
	
	public JsonConfigFileAccess(File file)
	{
		Assert.notNull(file);
		
		this.file = file;
		
		codecs.builtinCodecs();
	}
	
	private static JsonObject readJson(File file)
	{
		var gson = new Gson();
		try(var reader = new FileReader(file))
		{
			return gson.fromJson(reader, JsonObject.class);
		}
		catch(IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	private static void writeJson(File file, JsonObject json)
	{
		try(var writer = new FileWriter(file))
		{
			var gson = new GsonBuilder()
					.setPrettyPrinting()
					.create();
			gson.toJson(json, writer);
		}
		catch(IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	private static JsonObject getObjectContainingValueAtPath(JsonObject json, String path)
	{
		var keys = path.split("\\.");
		
		JsonElement current = json;
		JsonObject last = null;
		for(var key : keys)
		{
			if(current != null && current.isJsonObject())
			{
				last = current.getAsJsonObject();
				current = last.get(key);
			}
			else
			{
				return null;
			}
		}
		return last;
	}
	
	private static JsonElement getByPath(JsonObject json, String path)
	{
		var keys = path.split("\\.");
		var object = getObjectContainingValueAtPath(json, path);
		return object != null ? object.get(keys[keys.length - 1]) : null;
	}
	
	private static void setByPath(JsonObject json, String path, JsonElement value)
	{
		var keys = path.split("\\.");
		var object = getObjectContainingValueAtPath(json, path);
		if(object != null)
		{
			object.add(keys[keys.length - 1], value);
		}
	}
	
	@Override
	public ConfigCodecMap<JsonElement> codecs()
	{
		return codecs;
	}
	
	private JsonObject buildDefaults(Class<?> proxyClass, String parentPath)
	{
		var json = new JsonObject();
		
		var proxyDefault = Proxy.newProxyInstance(proxyClass.getClassLoader(), new Class[]{proxyClass}, new DefaultValueConfigHandler());
		
		for(var method : proxyClass.getMethods())
		{
			if(method.isAnnotationPresent(ConfigKey.class))
			{
				var key = method.getAnnotation(ConfigKey.class).value();
				if(key.isEmpty())
				{
					key = NamingConventionHelper.fromCamelCaseToSnakeCase(method);
				}
				var path = (parentPath.isEmpty() ? "" : (parentPath + ".")) + key;
				var returnType = method.getReturnType();
				
				if(returnType == void.class)
				{
					continue;
				}
				
				JsonElement value;
				if(method.isAnnotationPresent(SubSection.class))
				{
					value = this.buildDefaults(returnType, path);
				}
				else
				{
					Object defaultValue;
					try
					{
						defaultValue = InvocationHandler.invokeDefault(proxyDefault, method);
					}
					catch(Throwable ex)
					{
						throw new RuntimeException(ex);
					}
					value = codecs.encode(returnType, defaultValue);
				}
				
				json.add(key, value);
			}
		}
		
		return json;
	}
	
	private static void softMerge(JsonObject destination, JsonObject source)
	{
		for(var entry : source.entrySet())
		{
			var key = entry.getKey();
			var value = entry.getValue();
			
			if(destination.has(key) &&
			   destination.get(key).isJsonObject() &&
			   value.isJsonObject())
			{
				softMerge(destination.getAsJsonObject(key), value.getAsJsonObject());
			}
			else if(!destination.has(key))
			{
				destination.add(key, value);
			}
		}
	}
	
	@Override
	public void load(Class<?> proxyClass)
	{
		var defaultJson = this.buildDefaults(proxyClass, "");
		
		if(file.exists())
		{
			var existingJson = readJson(file);
			
			var json = new JsonObject();
			softMerge(json, existingJson);
			softMerge(json, defaultJson);
			this.json = json;
		}
		else
		{
			json = defaultJson;
		}
		
		writeJson(file, json);
	}
	
	@Override
	public Object get(Class<?> type, String path)
	{
		Assert.notNull(json, "Config file has not yet been loaded", IllegalStateException::new);
		
		var element = getByPath(json, path);
		if(element != null)
		{
			return codecs.encode(type, element);
		}
		
		return null;
	}
	
	@Override
	public void set(Class<?> type, String path, Object value)
	{
		Assert.notNull(json, "Config file has not yet been loaded", IllegalStateException::new);
		
		setByPath(json, path, codecs.encode(type, value));
		
		writeJson(file, json);
	}
}
