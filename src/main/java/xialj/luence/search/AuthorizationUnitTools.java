package xialj.luence.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import xialj.luence.search.bean.AuthorizationUnit;

public class AuthorizationUnitTools {

	/*
	 * Term => "authorization":
	 * "CURRENCY=33504;DEPARTMENT=33107;EXCHANGERATE=-1;MATERIAL=32311,32904;PARTNER=-1;UNIT=-1,32309"
	 */

	public static Set<AuthorizationUnit> parse(String str) {
		Set<AuthorizationUnit> result = new HashSet<AuthorizationUnit>();
		Map<String,AuthorizationUnit> refMap = new HashMap<String,AuthorizationUnit>();
		String[] authUnitStrs = str.split(";");
		for (String authUnitStr : authUnitStrs) {
			String[] unitDataStr = authUnitStr.split("=");
			Set<String> unitValues = new HashSet<String>(Arrays.asList(unitDataStr[1].split(",")));
			AuthorizationUnit authUnit =refMap.get(unitDataStr[0]);
			if(null == authUnit){
				authUnit = new AuthorizationUnit(unitDataStr[0], unitValues);
			}else{
				authUnit.appengValues(unitValues);
			}
			refMap.put(unitDataStr[0], authUnit);
		}
		for(Entry<String, AuthorizationUnit> r:refMap.entrySet()){
			result.add(r.getValue());
		}
		return result;
	}
}
