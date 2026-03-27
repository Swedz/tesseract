package net.swedz.tesseract.api.function;

public interface TriFunction<A, B, C, R>
{
	R apply(A a, B b, C c);
}
