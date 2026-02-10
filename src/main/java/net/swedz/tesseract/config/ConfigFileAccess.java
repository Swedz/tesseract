package net.swedz.tesseract.config;

public interface ConfigFileAccess<D>
{
	ConfigTranscoderMap<D> codecs();
	
	void load(Class<?> proxyClass);
	
	Object get(Class<?> type, String path);
}
