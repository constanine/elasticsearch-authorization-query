package xialj.luence.search.bean;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSON;

public class AuthorizationUnit {
	String name;
	Set<String> values;
	public String getName() {
		return name;
	}
	public Set<String> getValues() {
		return values;
	}		
	public AuthorizationUnit(String name,Set<String> values){
		this.name = name;
		this.values = values;
	}
	
	public void appengValues(Set<String> unitValues) {
		if(null == this.values){
			this.values = new HashSet<String>();
		}
		this.values.addAll(unitValues);
	}
	
	public String toString(){
		return JSON.toJSONString(this);
	}
}
