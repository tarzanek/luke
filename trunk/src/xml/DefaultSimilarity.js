// This is an implementation of DefaultSimilarity
// in JavaScript.
//
// NOTE: Since JavaScript is a weakly-typed language, some
// overloaded methods have been renamed to avoid ambiguity.
// You need to keep these changed names as they are, because
// the plugin depends on them. Other than that you are free
// to change anything else.

//--- ABSTRACT METHODS ---
// You HAVE TO implement these

function coord(overlap, maxOverlap) {
  return overlap / (1.0 * maxOverlap);
}

function idf(docFreq, numDocs) {
  return (Math.log(numDocs/(docFreq+1)) + 1.0);
}

function lengthNorm(fieldName, numTerms) {
  return (1.0 / Math.sqrt(numTerms));
}

function queryNorm(sumOfSquaredWeights) {
  return (1.0 / Math.sqrt(sumOfSquaredWeights));
}

function sloppyFreq(distance) {
  return 1.0 / (distance + 1);
}

function tf(freq) {
  return Math.sqrt(freq);
}

//--- PUBLIC METHODS ---
// You may choose to override these. If they are not overridden, the
// plugin will use DefaultSimilarity implementation, which is equivalent
// to the code reproduced below.

// RENAMED: float idf(Collection terms, Searcher searcher)
function idf_cs(terms, searcher) {
  var idf = 0.0;
  var i = terms.iterator();
  while (i.hasNext()) {
    // NOTE: we use a renamed method, due to ambiguity in overloading
    idf += idf_ts(i.next(), searcher);
  }
  return idf;
}

// RENAMED: float idf(Term term, Searcher searcher)
function idf_ts(term, searcher) {
  return idf(searcher.docFreq(term), searcher.maxDoc());
}

// RENAMED: float tf(int freq)
function tf_i(freq) {
  return tf(freq);
}