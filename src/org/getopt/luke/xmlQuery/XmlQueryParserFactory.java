package org.getopt.luke.xmlQuery;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.xmlparser.CoreParser;

public interface XmlQueryParserFactory
{

	CoreParser createParser(String defaultField, Analyzer analyzer);

}
