package net.swedz.tesseract.interfaceproxy;

public interface InterfaceProxyInstance<P, H extends InterfaceProxyHandler>
{
	Class<P> proxyClass();
	
	P proxy();
	
	default P config()
	{
		return this.proxy();
	}
	
	H handler();
	
	default InterfaceProxyInstance<P, H> load(boolean file)
	{
		this.handler().loadValues(this.proxyClass(), this.proxy());
		return this;
	}
	
	default InterfaceProxyInstance<P, H> load()
	{
		return this.load(true);
	}
}
