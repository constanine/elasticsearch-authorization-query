package xialj.luence.search;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import xialj.luence.search.bean.AuthorizationUnit;

public class AuthorizationScorer extends Scorer {
	private final PostingsEnum postingsEnum;
	private final Term condTerm;

	protected AuthorizationScorer(Weight weight, PostingsEnum pe,Term condTerm) {
		super(weight);
		this.postingsEnum = pe;
		this.condTerm = condTerm;
	}

	@Override
	public int docID() {
		return postingsEnum.docID();
	}

	final int freq() throws IOException {
		return postingsEnum.freq();
	}

	@Override
	public float score() throws IOException {		
		if (condTerm == null || condTerm.text()== "-1"){
			return 1;
		}
		
		String authStr = Term.toString(postingsEnum.getPayload());
		if (authStr == null || authStr.length() == 0) {
			return 1;
		}

		Set<AuthorizationUnit> authUnits = AuthorizationUnitTools.parse(authStr);
		return checkPermission(authUnits);
	}

	@Override
	public DocIdSetIterator iterator() {
		return postingsEnum;
	}

	@Override
	public String toString() {
		return "scorer(" + weight + ")[" + super.toString() + "]";
	}
	
	protected float checkPermission(Set<AuthorizationUnit> authUnits) {
		String grantsStr = condTerm.text();
		Set<AuthorizationUnit> condUnits = AuthorizationUnitTools.parse(grantsStr);
		for (AuthorizationUnit authUnit : authUnits) {
			String curauthName = authUnit.getName();
			for (AuthorizationUnit condUnit : condUnits) {
				if(curauthName.equals(condUnit.getName())){
					if(authUnit.getValues().containsAll(condUnit.getValues())){
						break;
					}else{
						return 0;
					}
				}
			}
		}
		return 1;
	}

}
