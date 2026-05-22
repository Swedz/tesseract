package net.swedz.tesseract.config.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.swedz.tesseract.config.ConfigFileAccess;
import net.swedz.tesseract.config.ConfigTranscoderMap;
import net.swedz.tesseract.config.DefaultValueConfigHandler;
import net.swedz.tesseract.config.annotation.ConfigKey;
import net.swedz.tesseract.config.annotation.SubSection;
import net.swedz.tesseract.api.Assert;
import net.swedz.tesseract.api.InlineTranscoder;
import net.swedz.tesseract.api.Transcoder;
import net.swedz.tesseract.helper.NamingConventionHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public final class JsonConfigFileAccess implements ConfigFileAccess<JsonElement>
{
	private final ConfigTranscoderMap<JsonElement> codecs = new ConfigTranscoderMap<>();
	
	private final File file;
	
	private JsonObject json;
	
	public JsonConfigFileAccess(File file)
	{
		Assert.notNull(file);
		
		this.file = file;
		
		codecs.register(String.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsString));
		codecs.register(boolean.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsBoolean));
		codecs.register(Boolean.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsBoolean));
		codecs.register(int.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsInt));
		codecs.register(Integer.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsInt));
		codecs.register(long.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsLong));
		codecs.register(Long.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsLong));
		codecs.register(double.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsDouble));
		codecs.register(Double.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsDouble));
		codecs.register(float.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsFloat));
		codecs.register(Float.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsFloat));
		codecs.register(short.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsShort));
		codecs.register(Short.class, new InlineTranscoder<>(JsonPrimitive::new, JsonElement::getAsShort));
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
	public ConfigTranscoderMap<JsonElement> codecs()
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
					Transcoder codec = codecs.get(returnType);
					Object defaultValue;
					try
					{
						defaultValue = InvocationHandler.invokeDefault(proxyDefault, method);
					}
					catch(Throwable ex)
					{
						throw new RuntimeException(ex);
					}
					value = (JsonElement) codec.encode(defaultValue);
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
			var codec = codecs.get(type);
			return codec.decode(element);
		}
		
		return null;
	}
	
	@Override
	public void set(Class<?> type, String path, Object value)
	{
		Assert.notNull(json, "Config file has not yet been loaded", IllegalStateException::new);
		
		var codec = (Transcoder<Object, JsonElement>) codecs.get(type);
		setByPath(json, path, codec.encode(value));
		
		writeJson(file, json);
	}
}
