package net.swedz.tesseract.interfaceproxy;

public interface InterfaceProxyEntry<R>
{
	R resolve(Object[] args);
}
