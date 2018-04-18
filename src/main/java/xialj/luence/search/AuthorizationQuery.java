package xialj.luence.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DocIdSetBuilder;

import xialj.luence.search.bean.AuthorizationUnit;

public class AuthorizationQuery extends Query {
	private final Term term;

	final class AccessControlQueryWeight extends Weight {
		private final float score;
		private final Term condTerm;

		public AccessControlQueryWeight(IndexSearcher searcher, boolean needsScores, float boost, Term term)
				throws IOException {
			super(AuthorizationQuery.this);
			if (needsScores && term == null) {
				throw new IllegalStateException("termStates are required when scores are needed");
			}
			this.score = boost;
			this.condTerm = term;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return true;
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			terms.add(getTerm());
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			final Scorer s = scorer(context);
			final boolean exists;
			if (s == null) {
				exists = false;
			} else {
				final TwoPhaseIterator twoPhase = s.twoPhaseIterator();
				if (twoPhase == null) {
					exists = s.iterator().advance(doc) == doc;
				} else {
					exists = twoPhase.approximation().advance(doc) == doc && twoPhase.matches();
				}
			}
			if (exists) {
				return Explanation.match(score, getQuery().toString() + (score == 1f ? "" : "^" + score));
			} else {
				return Explanation.noMatch(getQuery().toString() + " doesn't match id " + doc);
			}
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			final LeafReader reader = context.reader();
			final Terms terms = reader.terms(term.field());
			List<TermAndState> collectedTerms = _collectEffectTerms(context, terms, this.condTerm);
			return _scorer(_buildDocIdSet(reader, terms, collectedTerms));
		}

		private boolean _checkPermission(BytesRef termVal, String condText) {

			String authStr = Term.toString(termVal);

			Set<AuthorizationUnit> authUnits = AuthorizationUnitTools.parse(authStr);
			Set<AuthorizationUnit> condUnits = AuthorizationUnitTools.parse(condText);

			for (AuthorizationUnit authUnit : authUnits) {
				String curauthName = authUnit.getName();
				for (AuthorizationUnit condUnit : condUnits) {
					if (curauthName.equals(condUnit.getName())) {
						if (condUnit.getValues().containsAll(authUnit.getValues())) {
							break;
						} else {
							return false;
						}
					}
				}
			}
			return true;
		}

		private List<TermAndState> _collectEffectTerms(LeafReaderContext context, Terms terms, Term condTerm)
				throws IOException {
			List<TermAndState> result = new ArrayList<TermAndState>();
			TermsEnum termsEnum = terms.iterator();
			BytesRef curTermVal = termsEnum.next();
			while (null != curTermVal) {
				String condStr = condTerm.text();
				if ("-1".equals(condStr) || "all".equals(condStr) || _checkPermission(curTermVal, condStr)) {
					TermState state = termsEnum.termState();
					TermAndState t = new TermAndState(curTermVal, state);
					result.add(t);
				}
				curTermVal = termsEnum.next();
			}
			return result;
		}

		private DocIdSet _buildDocIdSet(LeafReader reader, Terms terms, List<TermAndState> collectedTerms)
				throws IOException {
			TermsEnum termsEnum2 = terms.iterator();
			PostingsEnum docs = null;
			DocIdSetBuilder builder = new DocIdSetBuilder(reader.maxDoc(), terms);
			if (collectedTerms.isEmpty() == false) {
				for (TermAndState t : collectedTerms) {
					termsEnum2.seekExact(t.term, t.state);
					docs = termsEnum2.postings(docs, PostingsEnum.NONE);
					builder.add(docs);
				}
			}
			return builder.build();
		}

		private Scorer _scorer(DocIdSet set) throws IOException {
			if (set == null) {
				return null;
			}
			final DocIdSetIterator disi = set.iterator();
			if (disi == null) {
				return null;
			}
			return new ConstantScoreScorer(this, this.score, disi);
		}

		@Override
		public String toString() {
			return "weight(" + AuthorizationQuery.this + ")";
		}
	}

	public AuthorizationQuery(Term term) {
		this.term = Objects.requireNonNull(term);
	}

	public AuthorizationQuery(Term term, TermContext states) {
		assert states != null;
		this.term = Objects.requireNonNull(term);
	}

	public Term getTerm() {
		return term;
	}

	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return new AccessControlQueryWeight(searcher, needsScores, boost, this.term);
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!term.field().equals(field)) {
			buffer.append(term.field());
			buffer.append(":");
		}
		buffer.append(term.text());
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AuthorizationQuery)) {
			return false;
		}

		AuthorizationQuery that = (AuthorizationQuery) o;
		if (this.term.equals(that.term)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return classHash() ^ term.hashCode();
	}

	private static class TermAndState {
		final BytesRef term;
		final TermState state;

		TermAndState(BytesRef term, TermState state) {
			this.term = term;
			this.state = state;
		}
	}
}
