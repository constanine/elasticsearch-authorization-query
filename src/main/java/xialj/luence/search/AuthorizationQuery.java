package xialj.luence.search;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class AuthorizationQuery extends Query {
	private final Term term;
	private final TermContext perReaderTermState;

	final class AccessControlQueryWeight extends Weight {
		private final TermContext termContext;
		private final boolean needsScores;

		public AccessControlQueryWeight(IndexSearcher searcher, boolean needsScores, float boost,
				TermContext termContext) throws IOException {
			super(AuthorizationQuery.this);
			if (needsScores && termContext == null) {
				throw new IllegalStateException("termStates are required when scores are needed");
			}
			this.needsScores = needsScores;
			this.termContext = termContext;
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
			AuthorizationScorer scorer = (AuthorizationScorer) scorer(context);
			if (scorer != null) {
				int newDoc = scorer.iterator().advance(doc);
				if (newDoc == doc) {
					float freq = scorer.freq();
					Explanation freqExplanation = Explanation.match(freq, "AuthorizationFreq=" + freq);
					return freqExplanation;
				}
			}
			return Explanation.noMatch("no matching term");
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			final TermsEnum termsEnum = getTermsEnum(context);
			if (termsEnum == null) {
				return null;
			}
			PostingsEnum docs = termsEnum.postings(null, needsScores ? PostingsEnum.FREQS : PostingsEnum.NONE);
			assert docs != null;
			return new AuthorizationScorer(this, docs, term);
		}

		private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
			if (termContext != null) {
				// TermQuery either used as a Query or the term states have been
				// provided at construction time
				assert termContext.wasBuiltFor(ReaderUtil.getTopLevelContext(
						context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader ("
								+ ReaderUtil.getTopLevelContext(context);
				final TermState state = termContext.get(context.ord);
				if (state == null) { // term is not present in that reader
					assert termNotInReader(context.reader(),
							term) : "no termstate found but term exists in reader term=" + term;
					return null;
				}
				final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
				termsEnum.seekExact(term.bytes(), state);
				return termsEnum;
			} else {
				Terms terms = context.reader().terms(term.field());
				if (terms == null) {
					return null;
				}
				final TermsEnum termsEnum = terms.iterator();
				if (termsEnum.seekExact(term.bytes())) {
					return termsEnum;
				} else {
					return null;
				}
			}
		}

		@Override
		public String toString() {
			return "weight(" + AuthorizationQuery.this + ")";
		}
	}

	public AuthorizationQuery(Term term) {
		this.term = Objects.requireNonNull(term);
		this.perReaderTermState = null;
	}

	public AuthorizationQuery(Term term, TermContext states) {
		assert states != null;
		this.term = Objects.requireNonNull(term);
		this.perReaderTermState = Objects.requireNonNull(states);
	}

	public Term getTerm() {
		return term;
	}

	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		final IndexReaderContext context = searcher.getTopReaderContext();
		final TermContext termState;
		if (perReaderTermState == null || perReaderTermState.wasBuiltFor(context) == false) {
			if (needsScores) {
				// make TermQuery single-pass if we don't have a PRTS or if the
				// context
				// differs!
				termState = TermContext.build(context, term);
			} else {
				// do not compute the term state, this will help save seeks in
				// the terms
				// dict on segments that have a cache entry for this query
				termState = null;
			}
		} else {
			// PRTS was pre-build for this IS
			termState = this.perReaderTermState;
		}
		return new AccessControlQueryWeight(searcher, needsScores, boost, termState);
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

	private boolean termNotInReader(LeafReader reader, Term term) throws IOException {
		return reader.docFreq(term) == 0;
	}
}
