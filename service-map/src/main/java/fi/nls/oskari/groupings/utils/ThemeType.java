package fi.nls.oskari.groupings.utils;

import java.util.HashMap;

public enum ThemeType {
 MAP(0,"map_layers"), STAT(1,"statistics");
	  private int codeValue;
 private String name;
 
 private ThemeType(int codeValue, String name) {
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
 
 private static HashMap<Integer, ThemeType> codeValueMap = new HashMap<Integer, ThemeType>(2);
 private static HashMap<String, ThemeType> nameValueMap = new HashMap<String, ThemeType>(2);
 static
 {
     for (ThemeType  type : ThemeType.values())
     {
         codeValueMap.put(type.codeValue, type);
     }
     
     for (ThemeType  type : ThemeType.values())
     {
         nameValueMap.put(type.name, type);
     }
 }

 //constructor and getCodeValue left out      

 public static ThemeType getInstanceFromCodeValue(int codeValue)
 {
     return codeValueMap.get(codeValue);
 }
 
 public static ThemeType getInstanceFromName(String name)
 {
     return nameValueMap.get(name);
 }
	  
}
