package com.xinran.testdinamicclassloader.example;


import com.xinran.testdinamicclassloader.classloader.ICalculator;

public class CalculatorAdvanced implements ICalculator {

	public String calculate(String expression) {
		return "Result is " + expression;
	}

	public String getVersion() {
		return "2.0";
	}

}
