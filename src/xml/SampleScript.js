// This is a sample script to demonstrate scripting of Luke

print("Available plugins:");
plugins = app.plugins;
for (var i = 0; i < plugins.size(); i++) {
	print("  - " + plugins.get(i).pluginName);
}
print("Available analyzers:");
for (var i = 0; i < app.analyzers.length; i++) {
	print("  - " + app.analyzers[i]);
}
if (ir != null) {
 print("Number of documents: " + ir.numDocs());
 print("Field names: " + ir.fieldNames);
}
app.actionAbout();
