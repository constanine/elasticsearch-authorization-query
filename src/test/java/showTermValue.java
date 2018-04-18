import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

public class showTermValue {
	public static void main (String[] args){
		byte[] b = new byte[]{78, 101, 119, 89, 111, 114, 107, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		BytesRef b1 = new BytesRef("6f 72 6b");
		System.out.println(Term.toString(new BytesRef(b)));
		System.out.println(Term.toString(b1));
	}
}
