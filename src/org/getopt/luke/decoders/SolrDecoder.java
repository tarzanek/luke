package org.getopt.luke.decoders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import org.apache.lucene.document.Fieldable;
import org.apache.solr.schema.FieldType;
import org.getopt.luke.ClassFinder;

public class SolrDecoder implements Decoder {
  private static final String solr_prefix = "org.apache.solr.schema.";

  private static final TreeMap<String, FieldType> types = new TreeMap<String, FieldType>();
  private static String[] typeNames = new String[0];
  
  private FieldType fieldType;
  private String name;
  
  static {
    // initialize the types map
    try {
      Class[] classes = ClassFinder.getInstantiableSubclasses(FieldType.class);
      if (classes == null || classes.length == 0) {
        throw new ClassNotFoundException("Missing Solr types???");
      }
      for (Class cls : classes) {
        FieldType ft = (FieldType)cls.newInstance();
        String name = cls.getName();
        types.put(name, ft);
      }
      ArrayList<String> names = new ArrayList<String>();
      for (String n : types.keySet()) {
        if (n.startsWith(solr_prefix)) {
          names.add("solr." + n.substring(solr_prefix.length()));
        }
      }
      Collections.sort(names);
      typeNames = (String[])names.toArray(new String[names.size()]);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public static final String[] getTypes() {
    return typeNames;
  }

  public SolrDecoder(String type) throws Exception {
    fieldType = types.get(type);
    if (fieldType == null && type.startsWith("solr.")) {
      String name = solr_prefix + type.substring(5);
      fieldType = types.get(name);
    }
    if (fieldType == null) {
      throw new Exception("Unknown Solr FieldType: " + type);
    }
    name = type;
  }
  
  public String decodeTerm(String fieldName, Object value) throws Exception {
    return fieldType.indexedToReadable(value.toString());
  }
  
  public String decodeStored(String fieldName, Fieldable value) throws Exception {
    return fieldType.storedToReadable(value);
  }
  
  public String toString() {
    return name;
  }

}
