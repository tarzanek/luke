// This is an implementation of DefaultSimilarity
// in JavaScript.
//
// For demonstration purposes each of the abstract methods implemented
// here prints out its result using JavaScript print() function. The
// actual output of print() depends on the compilation setings.

//--- ABSTRACT METHODS ---
// You HAVE TO implement these

function coord(overlap, maxOverlap) {
  var res = overlap / (1.0 * maxOverlap);
  print("coord", res);
  return res;
}

function idf(docFreq, numDocs) {
  var res = (Math.log(numDocs/(docFreq+1)) + 1.0);
  print("idf", res);
  return res;
}

function lengthNorm(fieldName, numTerms) {
  var res = (1.0 / Math.sqrt(numTerms));
  print("lengthNorm", res);
  return res;
}

function queryNorm(sumOfSquaredWeights) {
  var res = (1.0 / Math.sqrt(sumOfSquaredWeights));
  print("queryNorm", res);
  return res;
}

function sloppyFreq(distance) {
  var res = 1.0 / (distance + 1);
  print("sloppyFreq", res);
  return res;
}

function tf(freq) {
  var res = Math.sqrt(freq);
  print("tf", res);
  return res;
}