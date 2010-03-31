package org.getopt.luke.xmlQuery;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.xmlparser.CoreParser;

public class CoreParserFactory implements XmlQueryParserFactory{
	@Override
	public CoreParser createParser(String defaultField, Analyzer analyzer){
		return new CoreParser(defaultField,analyzer);
	}
}
