function _is_undefined(value) {
  return typeof(value) === "undefined";
}

function _get_query(q) {
  var analyzer = 
    new Packages.org.apache.lucene.analysis.standard.StandardAnalyzer();
//  var parser = new Packages.org.apache.lucene.queryParser.QueryParser("f", analyzer);
//  return parser.parse(q);
}

function count() {
  print("count:" + ir.numDocs());
}

function count(q) {
  var searcher = new Packages.org.apache.lucene.search.IndexSearcher(ir);
  var query = _get_query(q);
//  var hits = searcher.search(query, 100).scoreDocs;
//  print("count(" + q + "):" + hits.length);
}

function search(q) {
  var searcher = new Packages.org.apache.lucene.search.IndexSearcher(ir);
  var query = _get_query(q);
//  var hits = searcher.search(query, 100).scoreDocs;
//  for (var i = 0; i < hits.length; i++) {
//    get(hits[i].doc, hits[i].score);
//  }
//  searcher.close();
}

function find(key, val) {
  var numDocs = ir.numDocs();
  for (var i = 0; i < numDocs; i++) {
    var doc = ir.document(i);
    var docval = String(doc.get(key));
    if (docval == null) {
      continue;
    }
    if (val == docval) {
      get(i);
    }
  }
}

function get(docId, score) {
  if (_is_undefined(score)) {
    print("-- docId: " + docId + " --");
  } else {
    print("-- docId:" + docId + " (score:" + score + ") --");
  }
  var doc = ir.document(docId);
  var fields = doc.getFields();
  for (var i = 0; i < fields.size(); i++) {
    var field = fields.get(i);
    var fieldname = field.name();
    print(fieldname + ":" + doc.get(fieldname));
  }
}

function terms(fieldname) {
  var te = ir.terms();
  var termDict = {};
  while (te.next()) {
    var fldname = te.term().field();
    if (_is_undefined(termDict[fldname])) {
      termDict[fldname] = 1;
    } else {
      termDict[fldname] = termDict[fldname] + 1;
    }
  }
  if (fieldname == "") {
    var sortable = [];
    for (var key in termDict) {
      sortable.push([key, termDict[key]]);
    }
    var sortedTermDict = sortable.sort(function(a,b) { return b[1] - a[1]; });
    for (var i = 0; i < sortedTermDict.length; i++) {
      print(sortedTermDict[i][0] + ":" + sortedTermDict[i][1]);
    }
  } else {
    if (_is_undefined(termDict[fieldname])) {
      print("Field not found:" + fieldname);
    } else {
      print(fieldname + ":" + termDict[fieldname]);
    }
  }
}

// unit tests
print("#-docs in index");
count();
//print("#-docs for title:bone");
//count("title:bone");

//print("Search for title:bone");
//search("title:bone");

//print("get doc 0");
//get(0);

//print("Find record with title: Broken bone");
//find("title", "Broken bone");

print("printing all term counts");
idxInfo = new Packages.org.getopt.luke.IndexInfo(ir,"C:\var\opengrok\data\index\repositories");
idxInfo.getNumTerms();
var fields=idxInfo.getFieldNames();
var termCounts=idxInfo.getFieldTermCounts();


for (var i = 0; i < fields.size(); i++) {
    var s = fields.get(i);
    var sc=termCounts.get(s).termCount;
    print(s+" "+sc);
  }



//print(ir.terms());
//terms("");
//print("printing term counts for idx");
//terms("idx");
//print("printing term counts for non-existent field foo");
//terms("foo");
