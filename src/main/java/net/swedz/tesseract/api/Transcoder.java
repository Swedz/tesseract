package net.swedz.tesseract.api;

public interface Transcoder<D, E>
{
	D decode(E encoded);
	
	E encode(D value);
}
