package net.swedz.tesseract.config;

public interface ConfigFileAccess<D>
{
	ConfigCodecMap<D> codecs();
	
	void load(Class<?> proxyClass);
	
	Object get(Class<?> type, String path);
	
	void set(Class<?> type, String path, Object value);
}
