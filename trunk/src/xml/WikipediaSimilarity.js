// This is an implementation of WikipediaSimilarity
// in JavaScript.
// (See http://issues.apache.org/bugzilla/show_bug.cgi?id=32674)
//

//--- GLOBAL VARIABLES ---

// lengthNorm uses logs to the base 10
var LOG10 = Math.log(10.0);
// Base of logarithm used to flatten tf's
var tfLogBase = Math.log(10.0);
// Base of logarithm used to flatten idf's
var idfLogBase = Math.log(10.0);

//--- ABSTRACT METHODS ---
// You HAVE TO implement these

function coord(overlap, maxOverlap) {
  return overlap / (1.0 * maxOverlap);
}

function idf(docFreq, numDocs) {
  return Math.sqrt(1.0 + Math.log(numDocs / (docFreq + 1.0)) / idfLogBase);
}

function lengthNorm(fieldName, numTerms) {
  if (fieldName.equals("body"))
      return 3.0 / (Math.log(1000 + numTerms) / LOG10);
  return 1.0;
}

function queryNorm(sumOfSquaredWeights) {
  return (1.0 / Math.sqrt(sumOfSquaredWeights));
}

function sloppyFreq(distance) {
  return 1.0 / (distance + 1);
}

function tf(freq) {
  return 1.0 + Math.log(freq) / tfLogBase;
}
