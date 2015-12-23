package fi.nls.oskari.groupings.utils;

import java.util.HashMap;

public enum DataType {
 MAP(0,"map_layer"), STAT(1,"statistic");
	  private int codeValue;
 private String name;
 
 private DataType(int codeValue, String name) {
	   this.codeValue = codeValue;
	   this.name = name;
	 }
 
 public int getCode()
{
	return codeValue;
}
 public String getName()
{
	return name;
}
 
 private static HashMap<Integer, DataType> codeValueMap = new HashMap<Integer, DataType>(2);
 private static HashMap<String, DataType> nameValueMap = new HashMap<String, DataType>(2);

 static
 {
     for (DataType  type : DataType.values())
     {
         codeValueMap.put(type.codeValue, type);
     }
     
     for (DataType  type : DataType.values())
     {
         nameValueMap.put(type.name, type);
     }
 }

 //constructor and getCodeValue left out      

 public static DataType getInstanceFromCodeValue(int codeValue)
 {
     return codeValueMap.get(codeValue);
 }
 
 public static DataType getInstanceFromName(String name)
 {
     return nameValueMap.get(name);
 }
	  
}
