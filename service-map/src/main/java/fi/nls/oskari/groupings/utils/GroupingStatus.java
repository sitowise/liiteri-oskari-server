package fi.nls.oskari.groupings.utils;

import java.util.HashMap;

public enum GroupingStatus {
 PRE(0,"alustava"), INPROGRESS(1,"kesken"), TESTING(2,"testauksessa"),PUBLISHED(3,"julkaistu");
	  private int codeValue;
 private String name;
 
 private GroupingStatus(int codeValue, String name) {
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
 
 private static HashMap<Integer, GroupingStatus> codeValueMap = new HashMap<Integer, GroupingStatus>(4);
 private static HashMap<String, GroupingStatus> nameValueMap = new HashMap<String, GroupingStatus>(4);
 static
 {
     for (GroupingStatus  type : GroupingStatus.values())
     {
         codeValueMap.put(type.codeValue, type);
     }
     
     for (GroupingStatus  type : GroupingStatus.values())
     {
         nameValueMap.put(type.name, type);
     }
 }

 //constructor and getCodeValue left out      

 public static GroupingStatus getInstanceFromCodeValue(int codeValue)
 {
     return codeValueMap.get(codeValue);
 }
 
 public static GroupingStatus getInstanceFromName(String name)
 {
     return nameValueMap.get(name);
 }
	  
}
