package org.getopt.luke.xmlQuery;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.xmlparser.CoreParser;
import org.apache.lucene.xmlparser.CorePlusExtensionsParser;

public class CorePlusExtensionsParserFactory implements XmlQueryParserFactory{

	@Override
	public CoreParser createParser(String defaultField, Analyzer analyzer){
		return new CorePlusExtensionsParser(defaultField,analyzer);
	}
}
